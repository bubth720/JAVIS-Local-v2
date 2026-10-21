#!/usr/bin/env python3
"""Read-only exact-base guard for native-qualification preparations; no application edits."""
import json
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE='90215258ad3914cd6576971334ecb8ca48db74ff'
def git(*a):return subprocess.check_output(['git',*a],cwd=ROOT)
files=git('ls-tree','-r','--name-only',BASE).decode().splitlines()
append_only={'AGENTS.md','docs/PROJECT_KNOWLEDGE.md'}
for n in files:
 old=git('show',BASE+':'+n);now=(ROOT/n).read_bytes()
 assert now.startswith(old) if n in append_only else now==old,n
new=set(git('ls-files').decode().splitlines())-set(files)
allowed={'tools/check-mvp-004.py','tools/mvp4_model_probe.py','tools/tests/test_mvp4_model_probe.py'}
for n in new:assert n in allowed or n.startswith('docs/mvp-004/'),n
print(json.dumps({'base':BASE,'unchanged_base_files':len(files)-len(append_only),'append_only':sorted(append_only),
 'all_application_files_build_configuration_native_dependencies_and_fixtures_identical':True,
 'no_data_admission_signature_or_budget_change':True},indent=2))
