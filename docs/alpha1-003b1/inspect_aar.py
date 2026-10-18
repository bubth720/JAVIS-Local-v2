#!/usr/bin/env python3
"""Inspect candidate bytes/JVM declarations without loading any candidate native library."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess
import tempfile
import zipfile


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--aar', type=Path, required=True)
    p.add_argument('--historical', type=Path, required=True)
    p.add_argument('--tools', type=Path, required=True)
    p.add_argument('--scratch', type=Path, required=True)
    args = p.parse_args()
    args.scratch.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='inspect-', dir=args.scratch))
    readelf = args.tools / 'android-sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf'
    javap = args.tools / 'jdk-17.0.16+8/bin/javap'
    result = {'candidate_sha256': sha(args.aar.read_bytes()), 'historical_sha256': sha(args.historical.read_bytes()),
              'members': {}, 'native': {}, 'public_jvm_compatibility': {}, 'candidate_native_loaded': False}
    with zipfile.ZipFile(args.aar) as z, zipfile.ZipFile(args.historical) as old:
        for name in sorted(z.namelist()):
            if name.endswith('/'):
                continue
            b = z.read(name)
            previous = old.read(name) if name in old.namelist() else None
            result['members'][name] = {'sha256': sha(b), 'size': len(b),
                'historical_sha256': sha(previous) if previous is not None else None, 'identical_historical': b == previous}
            if not name.endswith('.so'):
                continue
            assert name.startswith('jni/arm64-v8a/')
            file = work / Path(name).name
            file.write_bytes(b)
            assert b[:6] == b'\x7fELF\x02\x01'
            machine = struct.unpack_from('<H', b, 18)[0]
            offset = struct.unpack_from('<Q', b, 32)[0]
            size, count = struct.unpack_from('<HH', b, 54)
            segments = [struct.unpack_from('<IIQQQQQQ', b, offset + i * size) for i in range(count)]
            loads = [h[7] for h in segments if h[0] == 1]
            section_offset = struct.unpack_from('<Q', b, 40)[0]
            section_size, section_count, name_index = struct.unpack_from('<HHH', b, 58)
            sections = [struct.unpack_from('<IIQQQQIIQQ', b, section_offset + i * section_size)
                        for i in range(section_count)]
            names_header = sections[name_index]
            names = b[names_header[4]:names_header[4] + names_header[5]]
            android_api = None
            for header in sections:
                section_name = names[header[0]:].split(b'\0')[0]
                if section_name == b'.note.android.ident':
                    note = b[header[4]:header[4] + header[5]]
                    name_length = struct.unpack_from('<I', note, 0)[0]
                    android_api = struct.unpack_from('<I', note, 12 + (name_length + 3) // 4 * 4)[0]
            dynamic = subprocess.check_output([str(readelf), '-d', '-n', '--dyn-syms', '--wide', str(file)], text=True)
            result['native'][name] = {'sha256': sha(b), 'machine': machine, 'aarch64': machine == 183,
                'load_alignments': loads, 'at_least_16k': all(n >= 16384 for n in loads),
                'android_api_note': android_api,
                'executable_stack': any(h[0] == 0x6474e551 and h[1] & 1 for h in segments),
                'gnu_relro': any(h[0] == 0x6474e552 for h in segments),
                'needed': re.findall(r'Shared library: \[([^\]]+)\]', dynamic),
                'soname': re.findall(r'Library soname: \[([^\]]+)\]', dynamic),
                'jni_exports': sorted(set(re.findall(r'Java_dev_ffmpegkit_llama_LlamaJNI_\w+', dynamic)))}
        manifest = z.read('AndroidManifest.xml').decode()
        result['min_sdk_24_manifest'] = 'android:minSdkVersion="24"' in manifest
        result['manifest_permissions'] = re.findall(r'<uses-permission[^>]+>', manifest)
        result['consumer_proguard_packaged'] = 'proguard.txt' in z.namelist()
        (work / 'candidate.jar').write_bytes(z.read('classes.jar'))
        (work / 'historical.jar').write_bytes(old.read('classes.jar'))
    for cls in ['Llama', 'LlamaModel', 'LlamaJNI']:
        declarations = {}
        for version in ['candidate', 'historical']:
            output = subprocess.check_output([str(javap), '-p', '-s', '-classpath', str(work / (version + '.jar')),
                                              'dev.ffmpegkit.llama.' + cls], text=True)
            declarations[version] = {line.strip() for line in output.splitlines()
                                    if line.strip().startswith('public ') and line.strip().endswith(';')}
        missing = declarations['historical'] - declarations['candidate']
        result['public_jvm_compatibility'][cls] = {'historical_count': len(declarations['historical']),
            'all_preserved': not missing, 'missing': sorted(missing),
            'added': sorted(declarations['candidate'] - declarations['historical'])}
        assert not missing, (cls, missing)
    assert result['min_sdk_24_manifest'] and not result['manifest_permissions']
    assert len(result['native']) == 6
    assert all(v['aarch64'] and v['at_least_16k'] and not v['executable_stack'] for v in result['native'].values())
    exports = result['native']['jni/arm64-v8a/libllama_jni.so']['jni_exports']
    expected = {'Java_dev_ffmpegkit_llama_LlamaJNI_' + n for n in ['nativeLoadModel', 'nativeComplete', 'nativeEmbed',
                'nativeReleaseModel', 'nativeGetSystemInfo', 'nativePrepareMeasured', 'nativeConsumeMeasured', 'nativeDiscardMeasured']}
    assert set(exports) == expected
    assert result['native']['jni/arm64-v8a/libllama_jni.so']['android_api_note'] == 24
    packaged = {Path(name).name for name in result['native']}
    system = {'libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libandroid.so'}
    assert all(set(item['needed']) <= packaged | system for item in result['native'].values())
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
