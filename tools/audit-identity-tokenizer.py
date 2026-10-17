#!/usr/bin/env python3
"""Read-only inspection of the resolved AAR. Does not load JNI or execute a model."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

EXPECTED = 'fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501'
BASE = '8adec3e0e8dfdce685a33ef796fc3150facc7f0a'
ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--aar', type=Path, required=True, help='Existing resolved llama-android 0.1.1 AAR; no download')
    args = parser.parse_args()
    data = args.aar.read_bytes()
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED:
        raise SystemExit('UNREVIEWED_ARTIFACT: re-audit required; no capability assumed')
    java_home = os.environ.get('JAVA_HOME')
    javap = str(Path(java_home) / 'bin/javap') if java_home else 'javap'
    with tempfile.TemporaryDirectory(prefix='identity-tokenizer-audit-') as tmp:
        with zipfile.ZipFile(args.aar) as archive:
            parts = {name: archive.read(name) for name in (
                'classes.jar', 'jni/arm64-v8a/libllama_jni.so', 'jni/arm64-v8a/libllama.so')}
        for name, payload in parts.items():
            (Path(tmp) / Path(name).name).write_bytes(payload)
        def inspect(*argv):
            return subprocess.check_output(argv, text=True)
        api = inspect(javap, '-p', '-classpath', str(Path(tmp) / 'classes.jar'),
                      'dev.ffmpegkit.llama.LlamaJNI', 'dev.ffmpegkit.llama.LlamaResult')
        jni_symbols = inspect('nm', '-D', '--defined-only', str(Path(tmp) / 'libllama_jni.so'))
        c_symbols = inspect('nm', '-D', '--defined-only', str(Path(tmp) / 'libllama.so'))
        native_methods = re.findall(r'public final native [^;]+;', api)
        exports = sorted(re.findall(r'\bJava_dev_ffmpegkit_llama_LlamaJNI_\w+', jni_symbols))
        expected_methods = {'nativeLoadModel', 'nativeComplete', 'nativeEmbed', 'nativeReleaseModel', 'nativeGetSystemInfo'}
        observed = set(re.findall(r'\b(native\w+)\(', '\n'.join(native_methods)))
        if observed != expected_methods or len(exports) != 5:
            raise SystemExit('UNREVIEWED_API: expected five JNI entry points; inspect the difference')
        available_c = [name for name in ('llama_tokenize', 'llama_chat_apply_template',
            'llama_model_chat_template', 'llama_model_get_vocab', 'llama_n_ctx')
            if re.search(r'\b' + name + r'$', c_symbols, re.M)]
        # Compare tracked baseline payloads, not git diff (which misses untracked additions).
        protected = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', BASE, 'app',
            'fixtures', 'settings.gradle.kts', 'build.gradle.kts', 'gradle.properties', '.github',
            'tools/check-004c.py', 'tools/check-identity-002.py'], cwd=ROOT, text=True).splitlines()
        for name in protected:
            original = subprocess.check_output(['git', 'show', BASE + ':' + name], cwd=ROOT)
            if (ROOT / name).read_bytes() != original:
                raise SystemExit('PRESERVATION_FAILURE: ' + name)
        print(json.dumps({'artifact': 'dev.ffmpegkit-maintained:llama-android:0.1.1', 'aar_sha256': digest,
            'parts_sha256': {k: hashlib.sha256(v).hexdigest() for k, v in parts.items()},
            'jni_methods': native_methods, 'jni_exports': exports, 'available_c_symbols': available_c,
            'prompt_tokens_exposed_in_kotlin_result': 'getPromptTokens(' in api,
            'preflight_measurement_jni_available': False, 'native_executed': False,
            'production_port': 'BLOCKED_REQUIRES_REVIEWED_JNI_EXTENSION',
            'baseline': BASE, 'protected_files_equal': len(protected)}, indent=2))


if __name__ == '__main__':
    main()
