#!/usr/bin/env python3
"""Local, pinned build-time snapshots only. Never fetch, checkout, mutate repositories, or load models."""
import argparse, hashlib, io, json, pathlib, subprocess, tarfile
LOCK = pathlib.Path(__file__).resolve().parents[1] / 'docs/intensive-2026-10-22/CORE03-SOURCES.json'
def verify(root, expected):
    if (root / '.git').exists(): raise ValueError('Use an isolated snapshot, never a live repository')
    rows = []
    for p in root.rglob('*'):
        parts = p.relative_to(root).parts
        if parts[0] in {'.gradle', '.kotlin', 'build'} or len(parts) > 1 and parts[1] == 'build': continue
        if p.is_symlink(): raise ValueError('Source symlink refused')
        if p.is_file():
            if p.stat().st_size > 20 * 1024 * 1024: raise ValueError('Source size refused')
            rows.append((p.relative_to(root).as_posix(), hashlib.sha256(p.read_bytes()).hexdigest()))
    listing = ''.join(f'{digest}  {name}\n' for name, digest in sorted(rows))
    if len(rows) != expected['source_files'] or hashlib.sha256(listing.encode()).hexdigest() != expected['tree_sha256']:
        raise ValueError('Source snapshot differs from pinned Git commit')
    print('Source verified:', expected['repository'], expected['commit'])
def main():
    p = argparse.ArgumentParser(description=__doc__); p.add_argument('--verify', action='store_true')
    p.add_argument('name', choices=['nexus', 'services']); p.add_argument('directory', type=pathlib.Path)
    p.add_argument('--checkout', type=pathlib.Path); a = p.parse_args()
    expected = json.loads(LOCK.read_text())[a.name]
    if a.verify or a.directory.exists(): verify(a.directory, expected); return
    if a.checkout is None: p.error('An existing local checkout is required; no automatic download')
    git = lambda *args: subprocess.check_output(['git', '-C', str(a.checkout), *args])
    commit = expected['commit']
    if git('rev-parse', commit + '^{tree}').decode().strip() != expected['git_tree']: raise ValueError('Git tree mismatch')
    archive = git('archive', '--format=tar', commit)
    if len(archive) > 20 * 1024 * 1024: raise ValueError('Source archive too large')
    entries = []
    with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
        for member in tar:
            if member.isdir(): continue
            path = pathlib.PurePosixPath(member.name)
            if not member.isfile() or path.is_absolute() or '..' in path.parts: raise ValueError('Unsafe source entry')
            entries.append((path.parts, tar.extractfile(member).read()))
    if len(entries) != expected['source_files']: raise ValueError('Source count mismatch')
    a.directory.mkdir(parents=True, exist_ok=False)
    for parts, data in entries:
        target = a.directory.joinpath(*parts); target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(data)
    verify(a.directory, expected)
if __name__ == '__main__': main()
