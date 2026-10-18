#!/usr/bin/env python3
"""Read-only audit of pinned inputs. Patch candidates exist in memory only; no JNI."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import struct
import sys
import zipfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
BASE = 'd2a677bb35e63ee94db9dfa4edcaa97036b45dce'
PATCH_SHA = '94f06d0179b95c332f5842b73b69d103e09ca319befc319a0aeca341735c6df7'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


def audit(args):
    review = ROOT / 'docs/alpha1-002'
    lock = json.loads((review / 'SOURCE_LOCK.json').read_text())
    manifest = json.loads((review / 'PATCH_FILES.json').read_text())
    patch = (review / 'PATCH-A-VALIDER.patch').read_bytes()
    assert sha(patch) == PATCH_SHA
    protected = git('ls-tree', '-r', '--name-only', BASE).decode().splitlines()
    for name in protected:
        assert (ROOT / name).read_bytes() == git('show', BASE + ':' + name), name
    current = set(git('ls-files').decode().splitlines()) | set(
        git('ls-files', '--others', '--exclude-standard').decode().splitlines())
    # Pre-existing ID-002N review documents are retained, never added to this delivery.
    for name in current - set(protected):
        assert name.startswith(('docs/alpha1-003/', 'tools/alpha1-003/', 'docs/identity-002n/')), name
    for name, digest in lock['source_files'].items():
        assert sha((args.upstream / name).read_bytes()) == digest, name
    for name, digest in lock['llama_cpp_headers'].items():
        assert sha((args.llama / name).read_bytes()) == digest, name
    assert sha(args.aar.read_bytes()) == lock['aar_sha256']
    with zipfile.ZipFile(args.aar) as archive:
        for name, digest in lock['aar_parts'].items():
            assert sha(archive.read(name)) == digest, name
    spec = importlib.util.spec_from_file_location('read_only_review', review / 'verify_review.py')
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    sections = re.split(r'^diff --git a/(\S+) b/\S+\n', patch.decode(), flags=re.M)
    candidate = {}
    for name, body in zip(sections[1::2], sections[2::2]):
        assert name in manifest and '..' not in Path(name).parts
        prefix, relative = name.split('/', 1)
        original = ''
        if manifest[name]['base_sha256'] is not None:
            original = ((args.upstream / relative).read_text() if prefix == 'upstream'
                        else git('show', BASE + ':' + relative).decode())
            assert sha(original.encode()) == manifest[name]['base_sha256'], name
        lines = body.splitlines(keepends=True)
        first = next(i for i, line in enumerate(lines) if line.startswith('@@ '))
        value = helper.reconstruct(original, lines[first:])
        assert sha(value.encode()) == manifest[name]['proposed_sha256'], name
        assert name not in candidate
        candidate[name] = value
    assert set(candidate) == set(manifest) and len(candidate) == 12
    native = candidate['upstream/library/src/main/jni/measured.inc']
    prepare = native.split('Java_dev_ffmpegkit_llama_LlamaJNI_nativePrepareMeasured', 1)[1].split(
        'Java_dev_ffmpegkit_llama_LlamaJNI_nativeConsumeMeasured', 1)[0]
    consume = native.split('Java_dev_ffmpegkit_llama_LlamaJNI_nativeConsumeMeasured', 1)[1].split(
        'Java_dev_ffmpegkit_llama_LlamaJNI_nativeDiscardMeasured', 1)[0]
    for forbidden in ('llama_decode(', 'llama_memory_clear(', 'llama_init_from_model(', 'llama_model_load_from_file('):
        assert forbidden not in prepare
    assert 'render_strict(' not in consume and 'tokenize_strict(' not in consume
    licences = {}
    for source, folder in [('llama-android', args.upstream), ('llama.cpp', args.llama)]:
        raw = (folder / 'LICENSE').read_bytes()
        assert b'MIT License' in raw
        licences[source] = {'sha256': sha(raw), 'license': 'MIT', 'notice_required': True}
    return {'base': BASE, 'protected_tracked_files': len(protected),
            'source_files_verified': len(lock['source_files']),
            'llama_headers_verified': len(lock['llama_cpp_headers']),
            'aar_sha256': lock['aar_sha256'], 'aar_components_verified': len(lock['aar_parts']),
            'patch_sha256': PATCH_SHA, 'sensitive_files': manifest,
            'licences': licences, 'candidate_materialized': False,
            'patch_applied': False, 'jni_executed': False,
            'static_separation_only': True}


def compare(paths, published):
    def inspect(path):
        with zipfile.ZipFile(path) as archive:
            return {'aar_sha256': sha(path.read_bytes()), 'members': {
                name: sha(archive.read(name)) for name in sorted(archive.namelist())
                if not name.endswith('/')}, 'zip_timestamps': {
                entry.filename: list(entry.date_time) for entry in archive.infolist()
                if not entry.is_dir()}}
    records = [inspect(path) for path in paths]
    old = inspect(published)
    names = sorted(set().union(*(r['members'] for r in records), old['members']))
    components = {name: {'build_1': records[0]['members'].get(name),
                         'build_2': records[1]['members'].get(name),
                         'published': old['members'].get(name)} for name in names}
    elf = {}
    def sections(raw):
        assert raw[:6] == b'\x7fELF\x02\x01', 'ELF64 little endian required'
        offset = struct.unpack_from('<Q', raw, 40)[0]
        size, count, names_index = struct.unpack_from('<HHH', raw, 58)
        headers = [struct.unpack_from('<IIQQQQIIQQ', raw, offset + i * size) for i in range(count)]
        name_header = headers[names_index]
        names = raw[name_header[4]:name_header[4] + name_header[5]]
        return {names[h[0]:].split(b'\0')[0].decode(): raw[h[4]:h[4] + h[5]]
                for h in headers if h[1] != 8}
    with zipfile.ZipFile(paths[0]) as first, zipfile.ZipFile(paths[1]) as second:
        for name in sorted(first.namelist()):
            if not name.endswith('.so') or name not in second.namelist():
                continue
            a, b = sections(first.read(name)), sections(second.read(name))
            elf[name] = {'different_sections': [n for n in a if a[n] != b.get(n)],
                         'text_identical': a.get('.text') == b.get('.text'),
                         'identical_except_build_id': all(a[n] == b.get(n) for n in a if n != '.note.gnu.build-id')}
    return {'builds': records, 'published_sha256': old['aar_sha256'], 'components': components,
            'elf_sections': elf,
            'whole_aar_identical': records[0]['aar_sha256'] == records[1]['aar_sha256'],
            'all_member_bytes_identical': records[0]['members'] == records[1]['members'],
            'published_aar_identical': all(r['aar_sha256'] == old['aar_sha256'] for r in records)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('audit')
    p.add_argument('--upstream', type=Path, required=True)
    p.add_argument('--llama', type=Path, required=True)
    p.add_argument('--aar', type=Path, required=True)
    p = sub.add_parser('compare')
    p.add_argument('builds', nargs=2, type=Path)
    p.add_argument('--published', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args) if args.command == 'audit' else compare(args.builds, args.published)
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
