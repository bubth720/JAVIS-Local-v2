#!/usr/bin/env python3
"""Read-only preservation and approved bundle checks. No APK/model/native execution."""
import hashlib
import json
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE='da40ed7155409b85860a22c1dbbfe35029cea29a'
BUNDLE='431b5212f742bd9028477a6a603acf328c414801873efdd529f45b6ed5e619de'
def git(*args):return subprocess.check_output(['git','-C',str(ROOT),*args])
def main():
    spec=ROOT/'docs/identity-002/contract';bundle=json.loads((spec/'BUNDLE.json').read_text());h=hashlib.sha256()
    for entry in bundle['files']:
        name=entry['path'];data=(spec/name).read_bytes();assert hashlib.sha256(data).hexdigest()==entry['sha256']
        h.update(name.encode('utf-8')+b'\0'+data+b'\0')
    assert h.hexdigest()==bundle['bundle_sha256']==BUNDLE
    vectors=json.loads((spec/'vectors.json').read_text());assert len(vectors['vectors'])==6
    assert (ROOT/'app/src/test/resources/identity/vectors.json').read_bytes()==(spec/'vectors.json').read_bytes()
    manifest=json.loads((ROOT/'app/src/main/assets/identity/manifest-b.json').read_text());assert manifest==vectors['vectors'][4]['value']
    canonical=json.dumps(manifest,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
    assert hashlib.sha256(b'jarvis.identity/1:001B.1\0'+canonical).hexdigest()==vectors['vectors'][4]['sha256']
    protected=git('ls-tree','-r','--name-only',BASE,'app','fixtures','settings.gradle.kts','build.gradle.kts','gradle.properties','.github').decode().splitlines()
    for name in protected:assert (ROOT/name).read_bytes()==git('show',BASE+':'+name),name
    assert not git('ls-files','--others','--exclude-standard','fixtures').strip()
    print(json.dumps({'base':BASE,'bundle_sha256':BUNDLE,'preserved_existing_files':len(protected),
        'vectors':6,'default_appgraph_and_fixtures_unchanged':True,'production_tokenizer':'UNAVAILABLE','native_integration':'BLOCKED'}))
if __name__=='__main__':main()
