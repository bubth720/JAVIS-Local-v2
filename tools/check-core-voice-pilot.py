#!/usr/bin/env python3
"""Read-only scope and contract checks, without device, model or personal data."""
import argparse,hashlib,json,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
base='115dd3fe9b8e82b295b076dc045601a04d21a06f'
parser=argparse.ArgumentParser();parser.add_argument('--voice-root',type=Path);args=parser.parse_args()
protected=['.github','build.gradle.kts','settings.gradle.kts','app/build.gradle.kts','app/src/main/AndroidManifest.xml','app/src/main/assets','app/src/test/resources']
for p in (root/'app/src/main/java/com/stark/jarvislocal').glob('*.kt'):
 if p.name not in {'AppGraph.kt','MainActivity.kt','SpeechOutputPort.kt','NativeHandoffGate.kt'}:protected.append(str(p.relative_to(root)))
assert not subprocess.check_output(['git','diff',base,'--',*protected],cwd=root),'protected CORE files changed'
contract=root/'docs/mission-core-voice-pilot/fixture-contract';pin=json.loads((contract/'PIN.json').read_text());h=hashlib.sha256()
for name,expected in sorted(pin['files'].items()):
 data=(contract/name).read_bytes();assert hashlib.sha256(data).hexdigest()==expected
 h.update(name.encode()+b'\0'+data+b'\0')
assert h.hexdigest()==pin['sha256']
if args.voice_root:
 voice=args.voice_root.resolve()
 for name in pin['files']:
  counterpart=voice/('voiceBridgePilot/contract/'+name if name=='CONTRACT.md' else 'voiceBridgePilot/src/main/aidl/com/jarvis/voice/bridgepilot/ipc/'+name)
  assert counterpart.read_bytes()==(contract/name).read_bytes(),name
 assert (voice/'voiceBridgePilot/contract/PIN.json').read_bytes()==(contract/'PIN.json').read_bytes()
print(json.dumps({'base':base,'protected_core_unchanged':True,'fixture_contract_sha256':h.hexdigest(),'cross_repository_checked':bool(args.voice_root),'native_handoff':'UNQUALIFIED_BLOCKED','binder_client_in_core':False},indent=2))
