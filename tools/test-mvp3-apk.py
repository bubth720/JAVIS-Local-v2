#!/usr/bin/env python3
"""Real signed-APK inspection and negative controls; no install/JNI/model/network."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--sdk',type=Path,required=True)
p.add_argument('--report',type=Path,required=True)
a=p.parse_args()
rows=[]
with tempfile.TemporaryDirectory(prefix='mvp3-negative-apk-') as td:
    temp=Path(td)
    def inspect(apk,out):
        return subprocess.run([sys.executable,str(ROOT/'tools/mvp3_kit.py'),'--sdk',str(a.sdk),
            '--apk',str(apk),'--out',str(out),'--allow-dirty-local'],cwd=ROOT,capture_output=True,text=True)
    candidate=ROOT/'app/build/outputs/apk/mvp3/app-mvp3.apk'
    result=inspect(candidate,temp/'valid')
    ok=result.returncode==0
    if ok:
        info=json.loads((temp/'valid/APK-INFO.json').read_text())
        ok=info['package']=='com.stark.jarvislocal.mvp003' and info['classification']=='DIAGNOSTIC_NOT_OPERATIONAL'
    rows.append({'case':'authentic_signed_candidate_accepted','passed':ok,'exit':result.returncode})
    if not ok:print(result.stdout+result.stderr,file=sys.stderr)
    result=inspect(ROOT/'app/build/outputs/apk/mvp2/app-mvp2.apk',temp/'wrong')
    ok=result.returncode!=0 and 'Unexpected APK package' in result.stderr and not (temp/'wrong').exists()
    rows.append({'case':'other_signed_variant_refused','passed':ok,'exit':result.returncode})
    if not ok:print(result.stdout+result.stderr,file=sys.stderr)
    tampered=temp/'tampered.apk';shutil.copyfile(candidate,tampered)
    with zipfile.ZipFile(tampered,'a') as z:z.writestr('DIAGNOSTIC-TAMPER-PROBE.txt','synthetic signature-integrity test')
    result=inspect(tampered,temp/'tampered-output')
    ok=result.returncode!=0 and 'DOES NOT VERIFY' in result.stderr+result.stdout and not (temp/'tampered-output').exists()
    rows.append({'case':'modified_signed_archive_refused','passed':ok,'exit':result.returncode})
    if not ok:print(result.stdout+result.stderr,file=sys.stderr)
report={'kind':'Linux Android build-tools inspection, not Android runtime or JNI',
        'tests':len(rows),'passed':sum(r['passed'] for r in rows),'failed':sum(not r['passed'] for r in rows),'cases':rows}
a.report.parent.mkdir(parents=True,exist_ok=True)
a.report.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
sys.exit(0 if not report['failed'] else 1)
