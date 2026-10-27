"""Lot2 passive-source scope and actual regression inventories; no runtime grant."""
from pathlib import Path
import argparse, subprocess, tempfile, json, xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[1]
BASE='cd2adea393287ea94eb3145e8463209f6fa37e9d'
ADDED={'tools/check-prefusion-lot2.py','.github/workflows/prefusion-lot2.yml','docs/prefusion/LOT2.md',
'app/src/main/java/com/stark/jarvislocal/integrations/PinnedDocumentReadiness.kt',
'app/src/test/java/com/stark/jarvislocal/integrations/PinnedDocumentReadinessTest.kt'}
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def check():
    subprocess.run(['git','merge-base','--is-ancestor',BASE,'HEAD'],cwd=ROOT,check=True)
    frozen=git('ls-tree','-r','--name-only',BASE).decode().splitlines()
    actual=set(git('ls-files','--cached','--others','--exclude-standard').decode().splitlines())
    assert not actual-set(frozen)-ADDED,'Unexpected additional source'
    for p in frozen:
        path=ROOT/p;assert path.is_file() and not path.is_symlink(),p
        old=git('cat-file','--filters',BASE+':'+p)
        if p in ('AGENTS.md','docs/PROJECT_KNOWLEDGE.md'):assert path.read_bytes().startswith(old),p
        else:assert path.read_bytes()==old,p
    for p in actual:
        assert not (ROOT/p).is_symlink(),p
    with tempfile.TemporaryDirectory() as temp:
        subprocess.run(['git','clone','--quiet','--shared','--no-checkout',str(ROOT),temp],check=True)
        subprocess.run(['git','checkout','--quiet','--detach','f6a3b2d0c280c69ef1570544fdbf5bcf5818810c'],cwd=temp,check=True)
        subprocess.run(['python3','tools/check-fusion-02.py','--self-test'],cwd=temp,check=True)
    print('PASS passive lot2 scope; historical guard checked only at its own SHA; no Android GO')
def results():
    totals={}
    for name,location in [('core','app/build/test-results/testDebugUnitTest'),('mvp2','app/build/test-results/testMvp2UnitTest'),('fixture','fixtures/p2-client/build/test-results/testDebugUnitTest'),('jvm','integration-lab/build/test-results/test')]:
        expected=set()
        for f in (ROOT/'docs/fusion-02/evidence/final'/name).glob('*.xml'):
            expected.update((c.get('classname'),c.get('name')) for c in E.parse(f).getroot().iter('testcase'))
        assert expected,'Missing historical inventory '+name
        current=set()
        for f in (ROOT/location).glob('TEST-*.xml'):
            for c in E.parse(f).getroot().iter('testcase'):
                assert not any(c.find(t) is not None for t in ('failure','error','skipped'))
                key=(c.get('classname'),c.get('name'));assert key not in current;current.add(key)
        assert expected<=current,'Missing historical case '+name
        if name=='core':assert sum(c.endswith('.PinnedDocumentReadinessTest') for c,_ in current)==3
        totals[name]=len(current)
    print(json.dumps(totals))
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--results',action='store_true');args=parser.parse_args()
    check()
    if args.results:results()
