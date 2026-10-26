#!/usr/bin/env python3
"""Offline C0 scope guard. A passing check never qualifies an Android producer."""
import argparse
import copy
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BASE = '36761cea101b5f88b87cd4136dc075d28a1adb96'
GRAPH = 'app/src/main/java/com/stark/jarvislocal/AppGraph.kt'
ANCHOR = '    val connectors = com.stark.jarvislocal.events.ConnectorRegistry.defaults()\n'
ADDITION = ('    val documentToolsReadiness: com.stark.jarvislocal.integrations.DocumentToolReadinessPort =\n'
            '        com.stark.jarvislocal.integrations.UnavailableDocumentToolReadiness(connectors)\n')
APPEND_ONLY = {'AGENTS.md', 'docs/PROJECT_KNOWLEDGE.md'}
NEW = {'tools/check-fusion-02.py', '.github/workflows/fusion-02-core.yml',
       'app/src/main/java/com/stark/jarvislocal/integrations/DocumentToolReadiness.kt',
       'app/src/test/java/com/stark/jarvislocal/integrations/DocumentToolReadinessTest.kt'}

def require(value, message):
    if not value:
        raise ValueError(message)

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()

def baseline(path):
    return subprocess.check_output(['git', 'show', BASE + ':' + path], cwd=ROOT)

def check_graph(data):
    original = baseline(GRAPH)
    require(original.count(ANCHOR.encode()) == 1, 'Graph anchor changed')
    expected = original.replace(ANCHOR.encode(), (ANCHOR + ADDITION).encode())
    require(data == expected, 'AppGraph must only expose the passive refused port')

def metadata(d):
    require(d['base'] == BASE, 'Wrong base')
    require(d['branch'] == 'feat/fusion-02-core-readiness', 'Wrong branch')
    require(d['producer_integration_enabled'] is False, 'Producer activation forbidden')
    require(d['gates'] == {'C0': 'SOFTWARE_ONLY', 'C1_C4': 'BLOCKED_PRODUCERS'}, 'Unproven GO')
    expected = json.loads((ROOT / 'docs/intensive-2026-10-22/CORE03-SOURCES.json').read_text())
    require(d['consumed_jvm_sources'] == expected, 'Historical dependencies substituted')

def executed_cases(directory):
    files = sorted(directory.glob('TEST-*.xml'))
    require(bool(files), 'No XML results: ' + str(directory))
    cases = set()
    for file in files:
        for case in ET.parse(file).getroot().findall('testcase'):
            require(all(case.find(tag) is None for tag in ['failure', 'error', 'skipped']), 'Nonpassing test: ' + str(case.attrib))
            key = (case.get('classname'), case.get('name'))
            require(key not in cases, 'Duplicate case: ' + str(key))
            cases.add(key)
    return cases

def results():
    groups = [('core', 'app/build/test-results/testDebugUnitTest', 448, 480),
              ('mvp2', 'app/build/test-results/testMvp2UnitTest', 53, 53),
              ('fixture', 'fixtures/p2-client/build/test-results/testDebugUnitTest', 36, 36),
              ('jvm', 'integration-lab/build/test-results/test', 30, 30)]
    counts = {}
    for name, target, before, after in groups:
        old = json.loads((ROOT / f'docs/fusion-02/evidence/baseline/{name}.json').read_text())
        expected = {(c['class'], c['name']) for c in old['cases']}
        require(len(expected) == old['passed'] == before, 'Baseline inventory mismatch')
        current = executed_cases(ROOT / target)
        require(expected <= current and len(current) == after, 'Case regression: ' + name)
        if name == 'core':
            require(all(c.endswith('.DocumentToolReadinessTest') for c, _ in current - expected), 'Unexpected new test scope')
        counts[name] = {'passed': len(current), 'baseline': before, 'failed': 0, 'skipped': 0}
    return counts

def validate():
    d = json.loads((ROOT / 'docs/fusion-02/PROVENANCE.json').read_text()); metadata(d)
    require(git('branch', '--show-current') in ('', d['branch']), 'Wrong checkout branch')
    subprocess.run(['git', 'merge-base', '--is-ancestor', BASE, 'HEAD'], cwd=ROOT, check=True)
    paths = set(git('ls-tree', '-r', '--name-only', BASE).splitlines())
    changed = set(git('diff', '--name-only', BASE).splitlines())
    untracked = set(git('ls-files', '--others', '--exclude-standard').splitlines())
    ignored = {p for p in untracked if p.startswith(('docs/identity-002n/', '.kotlin/'))}
    for p in changed | (untracked - ignored):
        require(p == GRAPH or p in APPEND_ONLY or p in NEW or p.startswith('docs/fusion-02/'), 'Unexpected change: ' + p)
    check_graph((ROOT / GRAPH).read_bytes())
    for p in APPEND_ONLY:
        require((ROOT / p).read_bytes().startswith(baseline(p)), 'Historical instructions edited: ' + p)
    protected = paths - APPEND_ONLY - {GRAPH}
    require(not (protected & changed), 'Historical source/dependency/fixture modified')
    return {'base': BASE, 'protected_files': len(protected), 'app_graph': 'EXACT_PASSIVE_ADDITION',
            'producer_integration_enabled': False, 'ignored_preexisting_untracked': sorted(ignored)}

def self_test():
    correct = baseline(GRAPH).replace(ANCHOR.encode(), (ANCHOR + ADDITION).encode())
    check_graph(correct)
    refused = []
    for name, data in [('missing_refusal_port', baseline(GRAPH)),
                       ('warmup_inserted', correct + b'// warm up or other unrelated change\n'),
                       ('second_registry', correct.replace(b'UnavailableDocumentToolReadiness(connectors)', b'UnavailableDocumentToolReadiness(com.stark.jarvislocal.events.ConnectorRegistry.defaults())'))]:
        try:
            check_graph(data)
        except ValueError:
            refused.append(name)
        else:
            raise ValueError('Negative graph accepted: ' + name)
    original = json.loads((ROOT / 'docs/fusion-02/PROVENANCE.json').read_text())
    for name in ['fake_go', 'enabled_flag', 'wrong_base']:
        d = copy.deepcopy(original)
        if name == 'fake_go': d['gates']['C1_C4'] = 'GO'
        if name == 'enabled_flag': d['producer_integration_enabled'] = True
        if name == 'wrong_base': d['base'] = '0' * 40
        try:
            metadata(d)
        except ValueError:
            refused.append(name)
        else:
            raise ValueError('Negative metadata accepted: ' + name)
    return refused

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify-results', action='store_true')
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    result = validate()
    if args.verify_results: result['tests'] = results()
    if args.self_test: result['negative_vectors_refused'] = self_test()
    print(json.dumps(result, ensure_ascii=False, indent=2))
