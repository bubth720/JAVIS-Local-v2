#!/usr/bin/env python3
"""Kotlin-only static check in a separate review directory. No C++/JNI/AAR build."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import review


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, required=True)
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('--scratch', type=Path, required=True)
    args = parser.parse_args()
    root = args.scratch.resolve()
    assert not root.is_relative_to(review.ROOT.resolve())
    assert not root.is_relative_to(args.upstream.resolve())
    root.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='003a-kotlin-only-', dir=root))
    originals, _, revised = review.candidate(args.upstream)
    sources = {}
    for file in (args.upstream / review.FOLDER).glob('*.kt'):
        sources[file.name] = file.read_text()
    for name, value in revised.items():
        if name.endswith('.kt'):
            sources[Path(name).name] = value
    for name, value in sources.items():
        (work / name).write_text(value)
    assert not list(work.rglob('*.cpp')) and not list(work.rglob('*.inc'))
    cache = args.tools / 'gradle-proxy-home/caches/modules-2/files-2.1'
    def jar(group, artifact, version):
        return next((cache / group / artifact / version).rglob('*.jar'))
    compiler = [jar('org.jetbrains.kotlin', n, '2.0.21') for n in
                ['kotlin-compiler-embeddable', 'kotlin-stdlib', 'kotlin-script-runtime', 'kotlin-daemon-embeddable']]
    compiler += [jar('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10'),
                 jar('org.jetbrains', 'annotations', '13.0'),
                 jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.9.0')]
    compiler += list((cache / 'org.jetbrains.intellij.deps/trove4j').rglob('*.jar'))
    classpath = [jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21'),
                 jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.9.0'),
                 args.tools / 'android-sdk/platforms/android-35/android.jar']
    java = args.tools / 'jdk-17.0.16+8/bin/java'
    cmd = [str(java), '-cp', ':'.join(map(str, compiler)), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
           '-no-stdlib', '-no-reflect', '-jvm-target', '11', '-module-name', 'library_release',
           '-classpath', ':'.join(map(str, classpath)), '-d', str(work / 'review-classes')]
    subprocess.run(cmd + [str(work / name) for name in sorted(sources)], check=True)
    aar = next((cache / 'dev.ffmpegkit-maintained/llama-android/0.1.1').rglob('*.aar'))
    assert review.sha(aar.read_bytes()) == 'fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501'
    with zipfile.ZipFile(aar) as z:
        (work / 'old-classes.jar').write_bytes(z.read('classes.jar'))
    signatures = {}
    for name in ['Llama', 'LlamaJNI', 'LlamaModel']:
        def public_declarations(path):
            text = subprocess.check_output([str(args.tools / 'jdk-17.0.16+8/bin/javap'), '-p',
                '-classpath', str(path), 'dev.ffmpegkit.llama.' + name], text=True)
            return {line.strip() for line in text.splitlines() if line.strip().startswith('public ') and line.strip().endswith(';')}
        old = public_declarations(work / 'old-classes.jar')
        current = public_declarations(work / 'review-classes')
        assert old <= current, (name, old - current)
        signatures[name] = {'old_public_declarations': len(old), 'all_present': True}
    print(json.dumps({'kotlin_typecheck': 'PASS', 'jvm_signatures': signatures,
                      'patch_sha256': review.sha(review.make_diff(originals, revised).encode()),
                      'kotlin_source_files': len(sources), 'jni_loaded': False,
                      'native_compiled': False, 'aar_built': False, 'patch_applied': False}, indent=2))


if __name__ == '__main__':
    main()
