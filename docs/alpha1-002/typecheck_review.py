#!/usr/bin/env python3
"""Type/syntax-check isolated review candidates; no AAR build, JNI load or model."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--review-root', type=Path, required=True, help='scratch emitted by verify_review.py')
    p.add_argument('--upstream', type=Path, required=True)
    p.add_argument('--llama', type=Path, required=True, help='pinned llama.cpp headers')
    p.add_argument('--tool-root', type=Path, required=True, help='existing cloud tools/cache')
    args = p.parse_args()
    work = args.review_root
    lock = json.loads((HERE / 'SOURCE_LOCK.json').read_text())
    for name, digest in lock['llama_cpp_headers'].items():
        assert hashlib.sha256((args.llama / name).read_bytes()).hexdigest() == digest, name
    files = json.loads((HERE / 'PATCH_FILES.json').read_text())
    for name, hashes in files.items():
        assert hashlib.sha256((work / 'candidate' / name).read_bytes()).hexdigest() == hashes['proposed_sha256']
    cache = args.tool_root / 'gradle-home/caches/modules-2/files-2.1'
    def jar(group, artifact, version):
        return next((cache / group / artifact / version).rglob('*.jar'))
    compiler = [jar('org.jetbrains.kotlin', n, '2.0.21') for n in
                ('kotlin-compiler-embeddable', 'kotlin-stdlib', 'kotlin-script-runtime', 'kotlin-daemon-embeddable')]
    compiler += list((cache / 'org.jetbrains.intellij.deps/trove4j').rglob('*.jar'))
    compiler += [jar('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10'), jar('org.jetbrains', 'annotations', '13.0'),
                 jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.9.0')]
    cp = [jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21'),
          jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.9.0'),
          args.tool_root / 'android-sdk/platforms/android-35/android.jar']
    source_dir = 'library/src/main/kotlin/dev/ffmpegkit/llama'
    sources = {f.name: f for f in (args.upstream / source_dir).glob('*.kt')}
    sources.update({f.name: f for f in (work / 'candidate/upstream' / source_dir).glob('*.kt')})
    cmd = [str(args.tool_root / 'jdk-17.0.16+8/bin/java'), '-cp', ':'.join(map(str, compiler)),
           'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-jvm-target', '11']
    def execute(command):
        subprocess.run(list(map(str, command)), check=True)
    execute(cmd + ['-module-name', 'library_release', '-classpath', ':'.join(map(str, cp)), '-d', work / 'kotlin-library'] + list(sources.values()))
    aar = next((cache / 'dev.ffmpegkit-maintained/llama-android/0.1.1').rglob('*.aar'))
    lock = json.loads((HERE / 'SOURCE_LOCK.json').read_text())
    assert hashlib.sha256(aar.read_bytes()).hexdigest() == lock['aar_sha256']
    old_jar = work / 'old-classes.jar'
    with zipfile.ZipFile(aar) as archive:
        old_jar.write_bytes(archive.read('classes.jar'))
    signatures = {}
    for cls in ('Llama', 'LlamaModel', 'LlamaJNI'):
        def declarations(classpath):
            text = subprocess.check_output([str(args.tool_root / 'jdk-17.0.16+8/bin/javap'), '-p',
                '-classpath', str(classpath), 'dev.ffmpegkit.llama.' + cls], text=True)
            return {line.strip() for line in text.splitlines()
                    if line.strip().startswith('public ') and line.strip().endswith(';')}
        previous = declarations(old_jar)
        proposed = declarations(work / 'kotlin-library')
        assert previous <= proposed, (cls, previous - proposed)
        signatures[cls] = {'previous_public_declarations': len(previous), 'all_preserved': True}
    core_classes = ROOT / 'app/build/tmp/kotlin-classes/debug'
    assert core_classes.is_dir(), 'Run unchanged CORE build first'
    cp += [work / 'kotlin-library', core_classes]
    sources = list((work / 'candidate/core/app/src/main/java/com/stark/jarvislocal').glob('*.kt'))
    execute(cmd + ['-Xfriend-paths=' + str(core_classes), '-classpath', ':'.join(map(str, cp)),
                   '-d', work / 'kotlin-core'] + sources)
    stubs = work / 'platform-stubs/android'
    stubs.mkdir(parents=True, exist_ok=True)
    (stubs / 'log.h').write_text('#pragma once\n#define ANDROID_LOG_ERROR 6\nextern "C" int __android_log_print(int,const char*,const char*,...);\n')
    native = work / 'candidate/upstream/library/src/main/jni'
    common = ['g++', '-std=c++17', '-Wall', '-Wextra', '-Werror=return-type']
    for include in (args.tool_root / 'jdk-17.0.16+8/include', args.tool_root / 'jdk-17.0.16+8/include/linux',
                    stubs.parent, args.llama / 'include', args.llama / 'ggml/include', native):
        common += ['-I', str(include)]
    execute(common + ['-fsyntax-only', native / 'llama_jni.cpp'])
    execute(common + ['-O1', '-ffunction-sections', '-fdata-sections', '-Wl,--gc-sections',
                      HERE / 'BUDGET_UTF8_REVIEW_TEST.cpp', '-o', work / 'budget-utf8-test'])
    output = subprocess.check_output([str(work / 'budget-utf8-test')], text=True)
    assert output.strip() == 'PASS 4214 pure helper checks; no JNI/model execution'
    result = {'patch_sha256': hashlib.sha256((HERE / 'PATCH-A-VALIDER.patch').read_bytes()).hexdigest(),
              'kotlin_library_typecheck': 'PASS', 'public_jvm_signatures_static_comparison': signatures, 'kotlin_core_typecheck': 'PASS', 'cpp_syntax': 'PASS',
              'pure_budget_utf8_checks_passed': 4214, 'jni_executed': False, 'experimental_aar_built': False}
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
