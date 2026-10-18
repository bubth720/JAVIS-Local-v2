#!/usr/bin/env python3
"""Static review checker tests/mutations only. No library model, filesystem copy or JNI."""
import argparse
import json
from pathlib import Path
import sys
import unittest

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import review

UPSTREAM = None


class StaticReviewTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.originals, cls.previous, cls.revised = review.candidate(UPSTREAM)

    def mutate(self, old, new, key):
        changed = dict(self.revised)
        path = review.FOLDER + 'LlamaMeasured.kt'
        assert old in changed[path]
        changed[path] = changed[path].replace(old, new)
        self.assertFalse(review.assertions(changed)[key])

    def test_all_26_checks(self):
        checks = review.assertions(self.revised)
        self.assertEqual(26, len(checks))
        self.assertTrue(all(checks.values()))

    def test_seven_targets_no_core(self):
        diff = review.make_diff(self.originals, self.revised)
        names = {name for name, _ in review.parts(diff)}
        self.assertEqual(names, review.TARGETS)
        self.assertEqual(7, len(names))
        self.assertTrue(all(name.startswith('library/') for name in names))

    def test_all_hunks_roundtrip_in_memory(self):
        for name, body in review.parts(review.make_diff(self.originals, self.revised)):
            self.assertEqual(self.revised[name], review.reconstruct(self.originals[name], body))

    def test_destination_core_hidden_in_header_refused(self):
        diff = review.make_diff(self.originals, self.revised)
        diff = diff.replace('b/library/build.gradle.kts', 'b/core/app/build.gradle.kts', 1)
        with self.assertRaises(AssertionError):
            review.parts(diff)

    def test_missing_plus_header_refused(self):
        diff = review.make_diff(self.originals, self.revised).replace('+++ b/library/build.gradle.kts', '+++ b/other')
        with self.assertRaises(AssertionError):
            review.parts(diff)

    def test_three_native_units_identical_to_prior_proposal(self):
        for name in ['library/src/main/jni/llama_jni.cpp', 'library/src/main/jni/measured.inc', review.FOLDER + 'LlamaJNI.kt']:
            self.assertEqual(self.previous[name], self.revised[name])

    def test_private_root_removed_detected(self):
        self.mutate('app.noBackupFilesDir', 'otherDirectory', 'R01_context_private_root')

    def test_source_parent_destination_detected(self):
        self.mutate('val free = root.usableSpace', 'val free = source.parentFile.usableSpace', 'R01_no_source_parent_destination')

    def test_exclusive_flag_removed_detected(self):
        self.mutate('OsConstants.O_EXCL', '0', 'R01_exclusive_nofollow')

    def test_abandonment_hook_removed_detected(self):
        self.mutate('executor.execute { abandon(current) }', 'Unit', 'R02_cancellable_delivery_cleanup')

    def test_raw_handle_retention_moved_detected(self):
        changed = dict(self.revised)
        path = review.FOLDER + 'LlamaMeasured.kt'
        value = changed[path].replace('current.handle = handle // retained BEFORE constructing or delivering a wrapper', '')
        value = value.replace('current.model = model', 'current.handle = handle\n        current.model = model')
        changed[path] = value
        self.assertFalse(review.assertions(changed)['R02_raw_handle_retained_first'])

    def test_cancel_release_in_handler_detected(self):
        self.mutate('invokeOnCancellation { cancelled.set(true) }', 'invokeOnCancellation { releaseSomething() }', 'R02_cancel_does_not_release')

    def test_caller_gate_clobber_detected(self):
        self.mutate('finally { if (gateHeld) apiBusy.set(false) }', 'finally { apiBusy.set(false) }', 'R02_no_handoff_gate_clobber')

    def test_unbounded_read_detected(self):
        self.mutate('minOf(buffer.size.toLong(), remaining)', 'buffer.size.toLong()', 'R03_bounded_copy')

    def test_eof_probe_removed_detected(self):
        self.mutate('check(input.read() == -1)', 'check(true)', 'R03_bounded_copy')

    def test_model_hash_removed_detected(self):
        self.mutate('check(actual == expectedSha)', 'check(true)', 'R03_manifest_hash')

    def test_reentrancy_guard_removed_detected(self):
        self.mutate('CALLBACK_REENTRANCY', 'other', 'R05_guard_before_wait')

    def test_future_cancel_detected(self):
        self.mutate('// Never cancel the future', '// future.cancel(true); Never cancel the future', 'R05_no_future_cancel')

    def test_file_identity_check_removed_detected(self):
        self.mutate('stat.st_ino == inode', 'true', 'R06_identity_and_fsync')

    def test_directory_sync_removed_detected(self):
        self.mutate('Os.fsync(fd)', 'Unit', 'R06_identity_and_fsync')

    def test_retry_unknown_native_detected(self):
        self.mutate('current.abandoned && current.handle == 0L && !current.nativeUncertain', 'current.abandoned', 'R06_retry_file_only')

    def test_recursive_cleanup_detected(self):
        self.mutate('Os.remove(file.path)', 'file.deleteRecursively()', 'R06_no_reset_or_recursive_delete')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, required=True)
    args = parser.parse_args()
    UPSTREAM = args.upstream
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(StaticReviewTests))
    print(json.dumps({'scope': 'static-checker-mutations-only', 'tests': result.testsRun,
                      'failures': len(result.failures), 'errors': len(result.errors),
                      'jni_executed': False}))
    raise SystemExit(0 if result.wasSuccessful() else 1)
