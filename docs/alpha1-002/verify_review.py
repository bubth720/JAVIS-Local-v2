#!/usr/bin/env python3
"""Read-only provenance/scope audit + isolated candidate reconstruction, never git apply.
The scratch directory contains review material only; no Android native execution.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
BASE = 'b2ec98238f70e89edc636c1c671f2ddf1b957d74'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(*args, cwd=None):
    return subprocess.check_output(list(map(str, args)), cwd=cwd, text=True)


def reconstruct(original, lines):
    old = original.splitlines(keepends=True)
    out = []
    position = 0
    i = 0
    while i < len(lines):
        match = re.match(r'@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@', lines[i])
        if not match:
            i += 1
            continue
        target = max(0, int(match[1]) - 1)
        assert target >= position
        out.extend(old[position:target])
        position = target
        removed = added = 0
        i += 1
        while i < len(lines) and not lines[i].startswith('@@ '):
            line = lines[i]
            assert line[:1] in {' ', '+', '-'}, 'Unsupported patch record'
            if line[0] in ' -':
                assert position < len(old) and old[position] == line[1:]
                position += 1
                removed += 1
            if line[0] in ' +':
                out.append(line[1:])
                added += 1
            i += 1
        assert removed == int(match[2] if match[2] is not None else 1)
        assert added == int(match[4] if match[4] is not None else 1)
    out.extend(old[position:])
    return ''.join(out)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, required=True)
    parser.add_argument('--aar', type=Path, required=True)
    parser.add_argument('--scratch', type=Path, required=True)
    args = parser.parse_args()
    lock = json.loads((HERE / 'SOURCE_LOCK.json').read_text())
    files = json.loads((HERE / 'PATCH_FILES.json').read_text())
    protected = run('git', 'ls-tree', '-r', '--name-only', BASE, cwd=ROOT).splitlines()
    allowed = {'AGENTS.md', 'docs/PROJECT_KNOWLEDGE.md'}
    for name in protected:
        if name not in allowed:
            assert (ROOT / name).read_bytes() == subprocess.check_output(
                ['git', 'show', BASE + ':' + name], cwd=ROOT), name
    current = set(run('git', 'ls-files', cwd=ROOT).splitlines()) | set(
        run('git', 'ls-files', '--others', '--exclude-standard', cwd=ROOT).splitlines())
    assert all(name.startswith('docs/alpha1-002/') for name in current - set(protected)), 'Unexpected addition'
    for name, expected in lock['source_files'].items():
        assert sha((args.upstream / name).read_bytes()) == expected, name
    assert sha(args.aar.read_bytes()) == lock['aar_sha256']
    with zipfile.ZipFile(args.aar) as archive:
        for name, expected in lock['aar_parts'].items():
            assert sha(archive.read(name)) == expected, name
    assert not args.scratch.resolve().is_relative_to(ROOT.resolve()), 'Scratch must be outside CORE'
    assert not args.scratch.resolve().is_relative_to(args.upstream.resolve()), 'Scratch must be outside source snapshot'
    args.scratch.mkdir(parents=True, exist_ok=True)
    scratch = Path(tempfile.mkdtemp(prefix='alpha1-002-', dir=args.scratch))
    assert not scratch.resolve().is_relative_to(ROOT.resolve())
    patch = (HERE / 'PATCH-A-VALIDER.patch').read_text()
    sections = re.split(r'^diff --git a/(\S+) b/\S+\n', patch, flags=re.M)
    observed = set()
    for name, body in zip(sections[1::2], sections[2::2]):
        assert name in files and '..' not in Path(name).parts
        observed.add(name)
        folder, relative = name.split('/', 1)
        if files[name]['base_sha256'] is None:
            original = ''
        elif folder == 'upstream':
            original = (args.upstream / relative).read_text()
        else:
            assert folder == 'core'
            original = subprocess.check_output(['git', 'show', BASE + ':' + relative], cwd=ROOT).decode()
        if files[name]['base_sha256'] is not None:
            assert sha(original.encode()) == files[name]['base_sha256']
            dest = scratch / 'baseline' / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(original)
        lines = body.splitlines(keepends=True)
        first = next(i for i, line in enumerate(lines) if line.startswith('@@ '))
        candidate = reconstruct(original, lines[first:])
        assert sha(candidate.encode()) == files[name]['proposed_sha256'], name
        dest = scratch / 'candidate' / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(candidate)
    assert observed == set(files)
    run('git', 'apply', '--check', HERE / 'PATCH-A-VALIDER.patch', cwd=scratch / 'baseline')
    measured = (scratch / 'candidate/upstream/library/src/main/jni/measured.inc').read_text()
    prep = measured.split('Java_dev_ffmpegkit_llama_LlamaJNI_nativePrepareMeasured', 1)[1].split(
        'Java_dev_ffmpegkit_llama_LlamaJNI_nativeConsumeMeasured', 1)[0]
    consume = measured.split('Java_dev_ffmpegkit_llama_LlamaJNI_nativeConsumeMeasured', 1)[1]
    for prohibited in ('llama_decode(', 'llama_memory_clear(', 'llama_init_from_model(', 'llama_model_load_from_file('):
        assert prohibited not in prep, prohibited
    assert 'tokenize_strict(' not in consume and 'render_strict(' not in consume
    report = {'base': BASE, 'baseline_files_preserved': len(protected) - len(allowed),
              'source_files_hash_verified': len(lock['source_files']),
              'aar_sha256': lock['aar_sha256'], 'aar_parts_verified': len(lock['aar_parts']),
              'patch_files_reconstructed': len(observed), 'patch_sha256': sha(patch.encode()), 'patch_check': 'PASS',
              'static_prepare_consume_separation': 'PASS', 'scratch': str(scratch),
              'runtime_modified': False, 'jni_executed': False, 'binary_reproducibility_proven': False}
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
