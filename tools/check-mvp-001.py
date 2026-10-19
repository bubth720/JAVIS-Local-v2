#!/usr/bin/env python3
"""Preserve exact Preview base and historical JNI/admissions; no network/build/model action."""
import json
from pathlib import Path
import subprocess
BASE='86ea8bfa94deea0bab94914cd7214bc395a83f9b'
ROOT=Path(__file__).resolve().parents[1]
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
allowed={'app/build.gradle.kts','app/src/main/java/com/stark/jarvislocal/LlamaLocalEngine.kt'}
files=git('ls-tree','-r','--name-only',BASE).decode().splitlines()
for name in files:
 if name in {'AGENTS.md','docs/PROJECT_KNOWLEDGE.md'}:
  assert (ROOT/name).read_bytes().startswith(git('show',BASE+':'+name)),name
 elif name not in allowed:assert (ROOT/name).read_bytes()==git('show',BASE+':'+name),name
engine='app/src/main/java/com/stark/jarvislocal/LlamaLocalEngine.kt'
extra='''    /** MVP imports use a separate validator/path; historical asset rules remain unchanged. */
    internal constructor(path: String, ready: () -> Boolean) : this(ready, path,
        AndroidLlamaBackend(), ProcessInferenceAdmission.process)

'''
assert (ROOT/engine).read_text().count(extra)==1
assert (ROOT/engine).read_text().replace(extra,'').encode()==git('show',BASE+':'+engine)
build='app/build.gradle.kts'
extra='''        create("mvp") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".mvp001"
            versionNameSuffix = "-mvp001"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
        }
'''
assert (ROOT/build).read_text().count(extra)==1
assert (ROOT/build).read_text().replace(extra,'').encode()==git('show',BASE+':'+build)
print(json.dumps({'base':BASE,'unchanged_base_files':len(files)-4,'append_only_knowledge_files':['AGENTS.md','docs/PROJECT_KNOWLEDGE.md'],'only_existing_file_additions':sorted(allowed),
    'legacy_acceptance_jni_admission_preserved':True},indent=2))
