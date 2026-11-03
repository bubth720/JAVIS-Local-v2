#!/usr/bin/env python3
"""M50-S1: freeze the mission tree; inventory real XML, never Android/native evidence."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BASE = 'd754a6c19e3c94e415e8506d0d37b8d943951d88'
LOT3 = 'aed84860f2ca85713140a687ba7d86efc0203dc8'
BRANCH = 'codex/m50-s1-core-ci-20261029'
MISSION = 'mission/m50-integrated-beta-20261029'
ADDED = {
    'tools/check-m50-s1-core.py',
    'tests/m50_s1/test_scope.py',
    '.github/workflows/m50-s1-core.yml',
    'docs/m50/M50-S1-CORE.md',
}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)


def check():
    subprocess.run(['git', 'merge-base', '--is-ancestor', BASE, 'HEAD'], cwd=ROOT, check=True)
    frozen = {}
    for entry in git('ls-tree', '-rz', BASE).split(b'\0'):
        if entry:
            metadata, path = entry.split(b'\t', 1)
            mode, kind, sha = metadata.decode().split()
            assert kind == 'blob' and mode in ('100644', '100755'), 'Unsupported baseline entry'
            frozen[path.decode()] = (mode, sha)
    actual = set(git('ls-files', '-z', '--cached', '--others', '--exclude-standard').decode().split('\0')) - {''}
    assert actual == set(frozen) | ADDED, 'Unexpected addition or deletion: ' + str(actual ^ (set(frozen) | ADDED))
    for relative in actual:
        path = ROOT / relative
        assert path.is_file() and not path.is_symlink(), 'Not a regular file: ' + relative
        assert not any(parent.is_symlink() for parent in path.parents if parent != ROOT and ROOT in parent.parents), 'Symlink directory: ' + relative
        if relative in frozen:
            mode, sha = frozen[relative]
            assert bool(path.stat().st_mode & 0o111) == (mode == '100755'), 'Mode changed: ' + relative
            assert path.read_bytes() == git('cat-file', 'blob', sha), 'Frozen file changed: ' + relative
    print('PASS M50-S1 exact scope; production, manifests, tests, old workflows and guards unchanged')


def route(event, ref='', base='', head='', same_repo=False):
    if event == 'push':
        return ref == 'refs/heads/' + BRANCH
    if event == 'pull_request':
        return base == MISSION and head == BRANCH and same_repo
    return False


def historical():
    for sha, command in [
        (LOT3, ['python3', 'tools/check-prefusion-lot3.py']),
        ('a3113797a3c481bc516c5ef1fd4d7a74b919de40', ['python3', 'tools/check-prefusion-lot2.py']),
        ('f6a3b2d0c280c69ef1570544fdbf5bcf5818810c', ['python3', 'tools/check-fusion-02.py', '--self-test']),
    ]:
        with tempfile.TemporaryDirectory() as temporary:
            subprocess.run(['git', 'clone', '--quiet', '--shared', '--no-checkout', str(ROOT), temporary], check=True)
            subprocess.run(['git', 'checkout', '--quiet', '--detach', sha], cwd=temporary, check=True)
            subprocess.run(command, cwd=temporary, check=True)
        print('PASS historical guard at immutable source ' + sha)


def read_cases(directory):
    cases = set()
    files = list(directory.glob('TEST-*.xml'))
    assert files, 'Missing actual XML: ' + str(directory)
    for source in files:
        for case in ET.parse(source).getroot().iter('testcase'):
            assert not any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')), 'Non-PASS actual case'
            key = (case.get('classname'), case.get('name'))
            assert all(key) and key not in cases, 'Duplicate or unnamed case'
            cases.add(key)
    assert cases, 'No executed cases'
    return cases


def results():
    # Reuse the untouched lot3 inventory reader, without calling its out-of-scope guard.
    spec = importlib.util.spec_from_file_location('historical_inventory', ROOT / 'tools/check-prefusion-lot3.py')
    previous = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(previous)
    previous.results()
    totals = {}
    for name, relative in [
        ('core', 'app/build/test-results/testDebugUnitTest'),
        ('mvp2', 'app/build/test-results/testMvp2UnitTest'),
        ('fixture', 'fixtures/p2-client/build/test-results/testDebugUnitTest'),
        ('jvm', 'integration-lab/build/test-results/test'),
    ]:
        cases = read_cases(ROOT / relative)
        if name == 'core':
            assert sum(c.endswith('.CoreLot4SyntheticAuthorityTest') for c, _ in cases) == 12, 'Missing lot4 cases'
        totals[name] = len(cases)
    assert sum(totals.values()) >= 641, totals
    identity = {'commit': git('rev-parse', 'HEAD').decode().strip(),
                'tree': git('rev-parse', 'HEAD^{tree}').decode().strip()}
    report = {'identity': identity, 'counts': totals, 'pass': sum(totals.values()), 'fail': 0, 'skip': 0,
              'scope': 'JVM/Robolectric only; unique cases per suite, no sum of repeated runs',
              'android_instrumentation': 'NOT_RUN', 'native_generation': 'NOT_RUN', 'physical_device': 'NOT_RUN'}
    destination = ROOT / 'app/build/reports/m50-s1/RESULTS.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--historical', action='store_true')
    parser.add_argument('--results', action='store_true')
    args = parser.parse_args()
    check()
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        event = json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
        pull = event.get('pull_request', {})
        assert route(os.environ['GITHUB_EVENT_NAME'], os.environ.get('GITHUB_REF', ''),
                     pull.get('base', {}).get('ref', ''), pull.get('head', {}).get('ref', ''),
                     pull.get('head', {}).get('repo', {}).get('full_name') == os.environ['GITHUB_REPOSITORY']), 'Unreviewed M50 route'
    if args.historical:
        historical()
    if args.results:
        results()


if __name__ == '__main__':
    main()
