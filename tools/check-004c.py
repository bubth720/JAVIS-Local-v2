#!/usr/bin/env python3
"""Offline provenance/preservation check. No build, Git mutation or remote access."""
import hashlib
import json
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
fixture = root / 'fixtures/p2-client'
reference = json.loads((fixture / 'REFERENCE.json').read_text())
digest = hashlib.sha256()
for name, expected in sorted(reference['canonical_files'].items()):
    data = (fixture / name).read_bytes()
    assert hashlib.sha256(data).hexdigest() == expected, name
    digest.update(name.encode() + b'\0' + data + b'\0')
assert digest.hexdigest() == reference['contract_sha256'], 'canonical bundle changed'
assert reference['contract_sha256'] in (fixture / 'build.gradle.kts').read_text()
base = '29646448c80568d868a702192df761fc9d7a896d'
# Includes staged and unstaged changes against the pinned base; fixture never changes app.
assert not subprocess.check_output(['git', 'diff', base, '--', 'app', 'build.gradle.kts', '.github'], cwd=root), 'historical CORE changed'
print(json.dumps({'base': base, 'llm_commit': reference['commit'], 'contract_sha256': digest.hexdigest(),
                  'canonical_files': len(reference['canonical_files']), 'historical_core_unchanged': True}, indent=2))
