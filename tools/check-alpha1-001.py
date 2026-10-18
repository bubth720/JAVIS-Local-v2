#!/usr/bin/env python3
"""Offline ALPHA1-001 scope/bundle preservation, without replacing legacy guard outcomes."""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = '71831913a772bcc51b20885b52b965f5d3e9f61b'
ALLOWED = {
    'AGENTS.md', 'docs/PROJECT_KNOWLEDGE.md',
    'app/src/main/java/com/stark/jarvislocal/AppGraph.kt',
    'app/src/main/java/com/stark/jarvislocal/JarvisCore.kt',
    'app/src/main/java/com/stark/jarvislocal/MainActivity.kt',
    'app/src/main/java/com/stark/jarvislocal/SessionCoordinator.kt',
    'app/src/main/res/layout/activity_main.xml',
}
NEW = {
    'app/src/main/java/com/stark/jarvislocal/TextLabConversation.kt',
    'app/src/test/java/com/stark/jarvislocal/TextLabConversationTest.kt',
    'app/src/test/java/com/stark/jarvislocal/TextLabCoreTest.kt',
    'app/src/test/java/com/stark/jarvislocal/TextLabActivityTest.kt',
    'tools/check-alpha1-001.py',
}


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


def main():
    original = git('ls-tree', '-r', '--name-only', BASE).decode().splitlines()
    protected = [n for n in original if n not in ALLOWED]
    for name in protected:
        if not (ROOT / name).is_file() or (ROOT / name).read_bytes() != git('show', BASE + ':' + name):
            raise SystemExit('UNAUTHORIZED_BASELINE_CHANGE: ' + name)
    current = set(git('ls-files').decode().splitlines()) | set(git('ls-files', '--others', '--exclude-standard').decode().splitlines())
    additions = current - set(original)
    unexpected = [n for n in additions if n not in NEW and not n.startswith('.kotlin/') and not (
        n.startswith('docs/alpha1-001/') and Path(n).suffix in {'.md', '.json'})]
    if unexpected:
        raise SystemExit('UNREVIEWED_ADDITION: ' + repr(unexpected))
    bundle_dir = ROOT / 'docs/identity-002/contract'
    bundle = json.loads((bundle_dir / 'BUNDLE.json').read_text())
    digest = hashlib.sha256()
    for entry in bundle['files']:
        name = entry['path']; data = (bundle_dir / name).read_bytes()
        assert hashlib.sha256(data).hexdigest() == entry['sha256']
        digest.update(name.encode() + b'\0' + data + b'\0')
    assert digest.hexdigest() == '431b5212f742bd9028477a6a603acf328c414801873efdd529f45b6ed5e619de'
    print(json.dumps({'base': BASE, 'protected_files_identical': len(protected),
        'approved_identity_bundle': digest.hexdigest(), 'existing_tests_unchanged': True,
        'native_backend_controller_admission_manifests_fixtures_unchanged': True,
        'persona_enabled': False, 'native_or_physical_conversation_validated': False}, indent=2))


if __name__ == '__main__':
    main()
