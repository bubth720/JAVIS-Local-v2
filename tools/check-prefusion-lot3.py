"""Lot3 offline preservation and actual XML inventories. No mutation or native attestation."""
from pathlib import Path
import argparse,json,subprocess,xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[1]
BASE='af6fe97e2d3f1cded76a4c0e28754b2c21272692'
ADDED={'tools/check-prefusion-lot3.py','.github/workflows/prefusion-lot3-core.yml',
'app/src/main/java/com/stark/jarvislocal/coreui/lab/ReadOnlyCoreUiLabPort.kt',
'app/src/test/java/com/stark/jarvislocal/coreui/lab/ReadOnlyCoreUiLabPortTest.kt'}
def git(*a):return subprocess.check_output(['git',*a],cwd=ROOT)
def check():
    subprocess.run(['git','merge-base','--is-ancestor',BASE,'HEAD'],cwd=ROOT,check=True)
    frozen=git('ls-tree','-r','--name-only',BASE).decode().splitlines()
    actual=set(git('ls-files','--cached','--others','--exclude-standard').decode().splitlines())
    for p in actual-set(frozen):
        assert p in ADDED or p.startswith('docs/prefusion/lot3/'),'Unexpected addition '+p
    for p in frozen:
        path=ROOT/p;assert path.is_file() and not path.is_symlink(),p
        old=git('show',BASE+':'+p)
        if p in ['AGENTS.md','docs/PROJECT_KNOWLEDGE.md']:assert path.read_bytes().startswith(old),p
        else:assert path.read_bytes()==old,p
    assert all(not (ROOT/p).is_symlink() for p in actual)
    print('PASS: mission base preserved byte-for-byte except append-only instructions; no production composition')
def results():
    totals={}
    for name,location in [('core','app/build/test-results/testDebugUnitTest'),('mvp2','app/build/test-results/testMvp2UnitTest'),('fixture','fixtures/p2-client/build/test-results/testDebugUnitTest'),('jvm','integration-lab/build/test-results/test')]:
        expected=set()
        for f in (ROOT/'docs/fusion-02/evidence/final'/name).glob('*.xml'):
            expected.update((c.get('classname'),c.get('name')) for c in E.parse(f).getroot().iter('testcase'))
        assert expected,'Historical inventory missing '+name
        current=set()
        for f in (ROOT/location).glob('TEST-*.xml'):
            for c in E.parse(f).getroot().iter('testcase'):
                assert not any(c.find(t) is not None for t in ['failure','error','skipped'])
                key=(c.get('classname'),c.get('name'));assert key not in current;current.add(key)
        assert expected<=current,'Historical cases missing '+name
        if name=='core':
            assert sum(c.endswith('.PinnedDocumentReadinessTest') for c,_ in current)==3
            assert sum(c.endswith('.ReadOnlyCoreUiLabPortTest') for c,_ in current)==27
        totals[name]=len(current)
    assert sum(totals.values())>=629,totals
    print(json.dumps({'totals':totals,'historical_baseline_at_least':602,'new_readonly_lab':27,'scope':'JVM/Robolectric only; no real UID/IPC/native release'}))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--results',action='store_true');a=p.parse_args();check()
    if a.results:results()
