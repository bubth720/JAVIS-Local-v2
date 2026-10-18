#!/usr/bin/env python3
"""Run actual candidate JVM guards with a Looper double; never add ARM64 SOs to loader paths."""
import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--aar', type=Path, required=True)
    p.add_argument('--tools', type=Path, required=True)
    p.add_argument('--scratch', type=Path, required=True)
    args = p.parse_args()
    here = Path(__file__).resolve().parent
    args.scratch.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='guard-jvm-', dir=args.scratch))
    with zipfile.ZipFile(args.aar) as z:
        (work / 'candidate.jar').write_bytes(z.read('classes.jar'))
    java = args.tools / 'jdk-17.0.16+8/bin/java'
    javac = args.tools / 'jdk-17.0.16+8/bin/javac'
    source = work / 'src/android/os/Looper.java'
    source.parent.mkdir(parents=True)
    shutil.copyfile(here / 'Looper.java.fixture', source)
    subprocess.run([str(javac), '--release', '11', '-d', str(work / 'android-double'), str(source)], check=True)
    shutil.copyfile(here / 'GuardChecks.kt.fixture', work / 'GuardChecks.kt')
    cache = args.tools / 'gradle-proxy-home/caches/modules-2/files-2.1'
    def jar(group, name, version):
        return next((cache / group / name / version).rglob('*.jar'))
    std = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21')
    co = jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.9.0')
    compiler = [jar('org.jetbrains.kotlin', n, '2.0.21') for n in
                ['kotlin-compiler-embeddable', 'kotlin-stdlib', 'kotlin-script-runtime', 'kotlin-daemon-embeddable']]
    compiler += [jar('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10'), jar('org.jetbrains', 'annotations', '13.0'), co]
    compiler += list((cache / 'org.jetbrains.intellij.deps/trove4j').rglob('*.jar'))
    classpath = [work / 'android-double', work / 'candidate.jar', std, co,
                 args.tools / 'android-sdk/platforms/android-35/android.jar']
    subprocess.run([str(java), '-cp', ':'.join(map(str, compiler)), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-jvm-target', '11', '-Xfriend-paths=' + str(work / 'candidate.jar'),
        '-classpath', ':'.join(map(str, classpath)), '-d', str(work / 'guards.jar'), str(work / 'GuardChecks.kt')], check=True)
    run = subprocess.run([str(java), '-Xlog:class+load=info:file=' + str(work / 'classloads.log'),
        '-cp', str(work / 'guards.jar') + ':' + ':'.join(map(str, classpath)),
        'dev.ffmpegkit.llama.GuardChecksKt'], check=False)
    loaded = (work / 'classloads.log').read_text()
    assert 'dev.ffmpegkit.llama.LlamaJNI ' not in loaded, 'JNI class unexpectedly reached'
    print('Candidate AAR SHA256:', hashlib.sha256(args.aar.read_bytes()).hexdigest())
    print('Runtime class-load trace: LlamaJNI absent; no SO extracted or loaded')
    if run.returncode:
        raise SystemExit(run.returncode)


if __name__ == '__main__':
    main()
