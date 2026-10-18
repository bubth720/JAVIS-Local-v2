#!/usr/bin/env python3
"""Generate/check a documentary seven-file diff IN MEMORY. Never apply or compile JNI."""
import argparse
import difflib
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import sys

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
BASE = '84571ae6058518e75d63077dfda15a0ef60a0e46'
OLD_SHA = '94f06d0179b95c332f5842b73b69d103e09ca319befc319a0aeca341735c6df7'
FOLDER = 'library/src/main/kotlin/dev/ffmpegkit/llama/'
TARGETS = {'library/build.gradle.kts', 'library/src/main/jni/llama_jni.cpp',
           'library/src/main/jni/measured.inc', *(FOLDER + n for n in
           ['Llama.kt', 'LlamaJNI.kt', 'LlamaModel.kt', 'LlamaMeasured.kt'])}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def parts(patch):
    sections = re.split(r'^diff --git a/(\S+) b/(\S+)\n', patch, flags=re.M)
    result = []
    for name, destination, body in zip(sections[1::3], sections[2::3], sections[3::3]):
        assert name == destination and '..' not in Path(name).parts
        assert f'+++ b/{name}\n' in body
        assert f'--- a/{name}\n' in body or '--- /dev/null\n' in body
        result.append((name, body))
    return result


def reconstruct(original, body):
    helper = module('original_review_parser', ROOT / 'docs/alpha1-002/verify_review.py')
    lines = body.splitlines(keepends=True)
    index = next(i for i, line in enumerate(lines) if line.startswith('@@ '))
    return helper.reconstruct(original, lines[index:])


def candidate(upstream):
    old = (ROOT / 'docs/alpha1-002/PATCH-A-VALIDER.patch').read_bytes()
    assert sha(old) == OLD_SHA
    manifest = json.loads((ROOT / 'docs/alpha1-002/PATCH_FILES.json').read_text())
    originals = {}
    previous = {}
    for name, body in parts(old.decode()):
        if not name.startswith('upstream/'):
            continue  # all five CORE hunks excluded
        relative = name.removeprefix('upstream/')
        assert relative in TARGETS
        original = '' if manifest[name]['base_sha256'] is None else (upstream / relative).read_text()
        if original:
            assert sha(original.encode()) == manifest[name]['base_sha256'], name
        value = reconstruct(original, body)
        assert sha(value.encode()) == manifest[name]['proposed_sha256'], name
        originals[relative] = original
        previous[relative] = value
    assert set(previous) == TARGETS
    revised = module('library_proposal', HERE / 'proposal.py').revise(previous)
    assert set(revised) == TARGETS
    return originals, previous, revised


def make_diff(originals, revised):
    result = []
    for name in sorted(TARGETS):
        result.append(f'diff --git a/{name} b/{name}\n')
        if not originals[name]:
            result.append('new file mode 100644\n')
        result.extend(difflib.unified_diff(originals[name].splitlines(keepends=True),
            revised[name].splitlines(keepends=True), fromfile='a/' + name if originals[name] else '/dev/null',
            tofile='b/' + name))
    return ''.join(result)


def assertions(revised):
    measured = revised[FOLDER + 'LlamaMeasured.kt']
    native = revised['library/src/main/jni/measured.inc']
    legacy = revised[FOLDER + 'Llama.kt']
    checks = {
        'R01_context_private_root': all(s in measured for s in ['privateRoot(context: Context)', 'app.noBackupFilesDir',
             'app.applicationInfo.dataDir', 'stat.st_uid == Process.myUid()', '(stat.st_mode and 0x3f) == 0']),
        'R01_no_source_parent_destination': 'source.parentFile' not in measured,
        'R01_exclusive_nofollow': all(s in measured for s in ['OsConstants.O_EXCL', 'OsConstants.O_NOFOLLOW', 'current.snapshot = snapshot']),
        'R02_cancellable_delivery_cleanup': all(s in measured for s in ['suspendCancellableCoroutine',
             'continuation.resume(model) { _, _, _ ->', 'executor.execute { abandon(current) }']),
        'R02_raw_handle_retained_first': measured.index('current.handle = handle') < measured.index('val model = LlamaModel(handle'),
        'R02_cancel_does_not_release': 'invokeOnCancellation { cancelled.set(true) }' in measured,
        'R02_no_handoff_gate_clobber': 'finally { if (gateHeld) apiBusy.set(false) }' in measured,
        'R03_bounded_copy': all(s in measured for s in ['var remaining = expectedBytes',
             'minOf(buffer.size.toLong(), remaining)', 'remaining -= count.toLong()', 'check(input.read() == -1)',
             'original.st_size == expectedBytes', 'Os.fstat(input.fd).st_size == expectedBytes']),
        'R03_manifest_hash': 'check(actual == expectedSha)' in measured,
        'R05_guard_before_wait': all(s in measured for s in ['CALLBACK_REENTRANCY', 'OWNER_REENTRANCY',
             'SYNC_MAIN_THREAD_FORBIDDEN', 'apiBusy.compareAndSet(false, true)']),
        'R05_getinfo_owned': 'getSystemInfo(): String = LlamaOwner.blocking' in legacy,
        'R05_no_future_cancel': 'future.cancel(' not in measured,
        'R06_unload_before_zero': measured.index('LlamaJNI.nativeReleaseModel(current.handle)') < measured.index('current.handle = 0L'),
        'R06_cleanup_even_handle_zero': measured.index('current.snapshot?.deleteOwned()') > measured.index('current.model?.handle = 0L'),
        'R06_clear_after_file_sync': measured.index('current.snapshot?.deleteOwned()') < measured.index('slot = null //'),
        'R06_identity_and_fsync': all(s in measured for s in ['stat.st_ino == inode', 'stat.st_dev == device', 'Os.remove(file.path)', 'Os.fsync(fd)']),
        'R06_retry_file_only': 'current.abandoned && current.handle == 0L && !current.nativeUncertain' in measured,
        'R06_no_reset_or_recursive_delete': not any(s in measured for s in ['deleteRecursively', 'resetAdmission', 'nativeUncertain = false // reset']),
        'legacy_three_scoped_calls': all(s in legacy for s in ['LlamaOwner.load {', 'LlamaOwner.operation {', 'LlamaOwner.release(model)']),
        'legacy_constructor_preserved': 'constructor(handle: Long, config: LlamaConfig)' in revised[FOLDER + 'LlamaModel.kt'],
        'three_jni_additions_only': len(re.findall(r'external fun native', revised[FOLDER + 'LlamaJNI.kt'])) == 8,
        'native_no_second_load': 'require_measured(liveModels.empty(), "MODEL_ALREADY_RESIDENT")' in revised['library/src/main/jni/llama_jni.cpp'],
        'system_roles_distinct': '{"system",system.c_str()}' in native and '{"user",user.c_str()}' in native,
        'native_budgets_unchanged': all(s in native for s in ['output <= 256', 'margin == 128', 'requested <= 2048', 'ttlMs <= 5000']),
    }
    prepare = native.split('Java_dev_ffmpegkit_llama_LlamaJNI_nativePrepareMeasured', 1)[1].split('Java_dev_ffmpegkit_llama_LlamaJNI_nativeConsumeMeasured', 1)[0]
    consume = native.split('Java_dev_ffmpegkit_llama_LlamaJNI_nativeConsumeMeasured', 1)[1].split('Java_dev_ffmpegkit_llama_LlamaJNI_nativeDiscardMeasured', 1)[0]
    checks['prepare_no_decode_load_or_clear'] = not any(s in prepare for s in ['llama_decode(', 'llama_memory_clear(', 'llama_init_from_model(', 'llama_model_load_from_file('])
    checks['consume_no_render_or_tokenize'] = 'render_strict(' not in consume and 'tokenize_strict(' not in consume
    return checks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, required=True)
    parser.add_argument('--write-documents', action='store_true')
    args = parser.parse_args()
    originals, previous, revised = candidate(args.upstream)
    diff = make_diff(originals, revised)
    manifest = {name: {'base_sha256': sha(originals[name].encode()) if originals[name] else None,
                      'prior_proposal_sha256': sha(previous[name].encode()),
                      'proposed_sha256': sha(revised[name].encode())} for name in sorted(TARGETS)}
    checks = assertions(revised)
    assert all(checks.values()), checks
    if args.write_documents:
        # Only documentary diff/manifest under HERE, never original sources or source-set files.
        (HERE / 'PATCH-LIBRARY-A-VALIDER.patch').write_text(diff)
        (HERE / 'PATCH_FILES.json').write_text(json.dumps(manifest, indent=2) + '\n')
        (HERE / 'PATCH.sha256').write_text(sha(diff.encode()) + '  PATCH-LIBRARY-A-VALIDER.patch\n')
    assert (HERE / 'PATCH-LIBRARY-A-VALIDER.patch').read_text() == diff
    assert (HERE / 'PATCH.sha256').read_text() == sha(diff.encode()) + '  PATCH-LIBRARY-A-VALIDER.patch\n'
    assert json.loads((HERE / 'PATCH_FILES.json').read_text()) == manifest
    names = [n for n, _ in parts(diff)]
    assert len(names) == 7 and set(names) == TARGETS
    for name, body in parts(diff):
        assert reconstruct(originals[name], body) == revised[name], name
    for name in ['library/src/main/jni/llama_jni.cpp', 'library/src/main/jni/measured.inc', FOLDER + 'LlamaJNI.kt']:
        assert revised[name] == previous[name], 'JNI proposal body changed outside corrections scope'
    protected = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', BASE], cwd=ROOT, text=True).splitlines()
    for name in protected:
        assert (ROOT / name).read_bytes() == subprocess.check_output(['git', 'show', BASE + ':' + name], cwd=ROOT), name
    report = {'mission': 'ALPHA1-003A', 'base': BASE, 'patch_sha256': sha(diff.encode()),
              'library_files': len(names), 'core_hunks': 0, 'protected_files_unchanged': len(protected),
              'checks': checks, 'static_checks_passed': len(checks),
              'candidate_native_materialized': False, 'patch_applied': False, 'native_aar_built': False,
              'JNI_executed': False, 'phase_A_rerun': False}
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
