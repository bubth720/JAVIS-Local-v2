#!/usr/bin/env python3
"""Read-only exact MVP002 base guard; never relax preceding mission guards."""
import json
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE='c7fec293791dc0b30c83d712e0fae0bbf59736a0'
def git(*a):return subprocess.check_output(['git',*a],cwd=ROOT)
allowed={'AGENTS.md','docs/PROJECT_KNOWLEDGE.md','app/build.gradle.kts',
 'app/src/mvp2/java/com/stark/jarvislocal/Mvp2Activity.kt',
 'app/src/mvp2/java/com/stark/jarvislocal/Mvp2Speech.kt',
 'app/src/testMvp2/java/com/stark/jarvislocal/Mvp2SpeechTest.kt'}
files=git('ls-tree','-r','--name-only',BASE).decode().splitlines()
for n in files:
 if n not in allowed:assert (ROOT/n).read_bytes()==git('show',BASE+':'+n),n
for n in ['AGENTS.md','docs/PROJECT_KNOWLEDGE.md']:
 assert (ROOT/n).read_bytes().startswith(git('show',BASE+':'+n)),n
n='app/build.gradle.kts';old=git('show',BASE+':'+n).decode()
expected=old.replace('        release {','''        create("mvp3") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".mvp003"
            versionNameSuffix = "-mvp003-diagnostic"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
        }
        release {''',1).replace('    compileOptions {','''    sourceSets.getByName("mvp3").java.srcDirs("src/mvp/java", "src/mvp2/java")
    sourceSets.getByName("mvp3").res.srcDirs("src/mvp/res", "src/mvp2/res")

    compileOptions {''',1)
assert (ROOT/n).read_text()==expected,n
n='app/src/mvp2/java/com/stark/jarvislocal/Mvp2Activity.kt';old=git('show',BASE+':'+n).decode()
start=old.index('    override fun introText()');end=old.index('\n',start)
assert (ROOT/n).read_text()==old[:start]+'    override fun introText() = getString(R.string.mvp2_intro, applicationInfo.loadLabel(packageManager))'+old[end:],n
n='app/src/mvp2/java/com/stark/jarvislocal/Mvp2Speech.kt';old=git('show',BASE+':'+n).decode()
expected=old.replace('    private fun offline(): Boolean = try { airplaneMode() } catch (_: Exception) { false }','''    private fun offline(): Boolean {
        val allowed = try { airplaneMode() } catch (_: Exception) { false }
        // An observed loss of the prerequisite permanently invalidates this probe/session.
        // A later airplane-mode flag alone must never resurrect the previous human confirmation.
        if (!allowed) { verified = false; probeAccepted = false }
        return allowed
    }''').replace('get() = verified && local.status.value == TtsStatus.READY && offline()',
 'get() = offline() && verified && local.status.value == TtsStatus.READY')
assert (ROOT/n).read_text()==expected,n
n='app/src/testMvp2/java/com/stark/jarvislocal/Mvp2SpeechTest.kt';old=git('show',BASE+':'+n).decode()
assert (ROOT/n).read_text().startswith(old[:old.rfind('\n}')]), 'Existing tests changed'
new_allowed={'.github/workflows/mvp-003.yml','app/src/mvp2/res/values/strings.xml',
 'app/src/mvp3/AndroidManifest.xml','app/src/testMvp3/java/com/stark/jarvislocal/Mvp3QualificationTest.kt',
 'tools/check-mvp-003.py','tools/mvp3_kit.py','tools/test-mvp3-apk.py'}
# Include committed and staged new files; unrelated untracked user material is never added.
new_files=set(git('ls-files').decode().splitlines())-set(files)
for n in new_files:
 assert n in new_allowed or n.startswith('docs/mvp-003/'), 'Unexpected new tracked file: '+n
print(json.dumps({'base':BASE,'unchanged_base_files':len(files)-len(allowed),
 'allowed_existing_files':sorted(allowed),'runtime_core_jni_sqlite_binder_identity_unchanged':True,
 'dependencies_and_resource_budgets_unchanged':True},indent=2))
