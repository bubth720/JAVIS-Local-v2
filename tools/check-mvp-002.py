#!/usr/bin/env python3
"""Exact-base preservation for MVP002; read-only, no build/model/download."""
from pathlib import Path
import subprocess,json
ROOT=Path(__file__).resolve().parents[1]
BASE='7368f41bbcec389e4b76c99ea27819645d94a50d'
allowed={'AGENTS.md','docs/PROJECT_KNOWLEDGE.md','app/build.gradle.kts',
 'app/src/main/java/com/stark/jarvislocal/LocalTts.kt',
 *('app/src/mvp/java/com/stark/jarvislocal/'+n+'.kt' for n in ['MvpActivity','MvpApplication','MvpRuntime','MvpModelStore'])}
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
files=git('ls-tree','-r','--name-only',BASE).decode().splitlines()
for n in files:
 if n not in allowed:assert (ROOT/n).read_bytes()==git('show',BASE+':'+n),n
for n in ['AGENTS.md','docs/PROJECT_KNOWLEDGE.md']:assert (ROOT/n).read_bytes().startswith(git('show',BASE+':'+n))
n='app/src/main/java/com/stark/jarvislocal/LocalTts.kt'
old=git('show',BASE+':'+n).decode()
assert (ROOT/n).read_text()==old.replace('it.locale.language == "fr" && !it.isNetworkConnectionRequired',
 'it.locale.language == "fr" && !it.isNetworkConnectionRequired &&\n        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()')
build=(ROOT/'app/build.gradle.kts').read_text();old=git('show',BASE+':app/build.gradle.kts').decode()
assert build[build.index('dependencies {'):]==old[old.index('dependencies {'):], 'Dependency changed'
expected = old.replace('        release {', '\n'.join([
 '        create("mvp2") {', '            initWith(getByName("debug"))',
 '            applicationIdSuffix = ".mvp002"', '            versionNameSuffix = "-mvp002"',
 '            signingConfig = signingConfigs.getByName("debug")', '            matchingFallbacks += listOf("debug")',
 '        }', '        release {']), 1).replace('    compileOptions {',
 '    sourceSets.getByName("mvp2").java.srcDir("src/mvp/java")\n    sourceSets.getByName("mvp2").res.srcDir("src/mvp/res")\n\n    compileOptions {',1)
assert build == expected, 'Unapproved build change'
print(json.dumps({'base':BASE,'unchanged_base_files':len(files)-len(allowed),'allowed_existing_files':sorted(allowed),
 'jni_llama_whisper_db_schema_modelmanager_admission_binder_unchanged':True,'dependencies_unchanged':True},indent=2))
