"""Synthetic git fixtures for the new guard; no historical tests changed."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[2] / 'tools/check-m50-s1-core.py'
SPEC = importlib.util.spec_from_file_location('guard', SCRIPT)
guard = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(guard)


class ScopeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.old = guard.ROOT, guard.BASE
        self.addCleanup(self.restore)
        guard.ROOT = self.root
        self.git('init', '-q')
        self.git('config', 'user.email', 'synthetic@example.invalid')
        self.git('config', 'user.name', 'M50 synthetic')
        self.write('app/src/main/AndroidManifest.xml', '<manifest/>')
        self.write('app/src/main/Core.kt', 'runtime frozen')
        self.write('app/src/test/HistoricalTest.kt', 'historical test frozen')
        self.write('.github/workflows/historical.yml', 'historical workflow frozen')
        self.write('tools/old-guard.py', 'historical guard frozen')
        self.git('add', '.')
        self.git('commit', '-qm', 'synthetic baseline')
        guard.BASE = self.git('rev-parse', 'HEAD').strip()
        for name in guard.ADDED:
            self.write(name, 'reviewed new file')

    def restore(self):
        guard.ROOT, guard.BASE = self.old

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True)

    def write(self, name, content):
        target = self.root / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)

    def test_exact_additions_pass(self):
        guard.check()

    def test_runtime_change_refused(self):
        self.write('app/src/main/Core.kt', 'changed')
        with self.assertRaises(AssertionError): guard.check()

    def test_manifest_permission_change_refused(self):
        self.write('app/src/main/AndroidManifest.xml', '<uses-permission/>')
        with self.assertRaises(AssertionError): guard.check()

    def test_historical_test_change_refused(self):
        self.write('app/src/test/HistoricalTest.kt', 'changed')
        with self.assertRaises(AssertionError): guard.check()

    def test_historical_guard_change_refused(self):
        self.write('tools/old-guard.py', 'changed')
        with self.assertRaises(AssertionError): guard.check()

    def test_workflow_change_refused(self):
        self.write('.github/workflows/historical.yml', 'changed')
        with self.assertRaises(AssertionError): guard.check()

    def test_deletion_refused(self):
        (self.root / 'app/src/main/Core.kt').unlink()
        with self.assertRaises(AssertionError): guard.check()

    def test_new_runtime_refused(self):
        self.write('app/src/main/New.kt', 'new runtime')
        with self.assertRaises(AssertionError): guard.check()

    def test_new_workflow_refused(self):
        self.write('.github/workflows/unreviewed.yml', 'new workflow')
        with self.assertRaises(AssertionError): guard.check()

    def test_missing_reviewed_file_refused(self):
        (self.root / 'docs/m50/M50-S1-CORE.md').unlink()
        with self.assertRaises(AssertionError): guard.check()

    def test_symlink_refused(self):
        target = self.root / 'app/src/main/Core.kt'
        target.unlink()
        target.symlink_to('AndroidManifest.xml')
        with self.assertRaises(AssertionError): guard.check()

    def test_mode_change_refused(self):
        (self.root / 'app/src/main/Core.kt').chmod(0o755)
        with self.assertRaises(AssertionError): guard.check()

    def test_symlink_parent_directory_refused(self):
        outside = tempfile.TemporaryDirectory()
        self.addCleanup(outside.cleanup)
        directory = self.root / 'app/src'
        moved = Path(outside.name) / 'src'
        directory.rename(moved)
        directory.symlink_to(moved, target_is_directory=True)
        with self.assertRaises(AssertionError): guard.check()

    def test_new_allowlisted_symlink_refused(self):
        target = self.root / 'docs/m50/M50-S1-CORE.md'
        target.unlink()
        target.symlink_to(self.root / 'app/src/main/Core.kt')
        with self.assertRaises(AssertionError): guard.check()

    def test_push_route(self):
        self.assertTrue(guard.route('push', 'refs/heads/' + guard.BRANCH))
        self.assertFalse(guard.route('push', 'refs/heads/main'))
        self.assertFalse(guard.route('workflow_dispatch', 'refs/heads/' + guard.BRANCH))

    def test_pr_route(self):
        self.assertTrue(guard.route('pull_request', base=guard.MISSION, head=guard.BRANCH, same_repo=True))
        self.assertFalse(guard.route('pull_request', base='main', head=guard.BRANCH, same_repo=True))
        self.assertFalse(guard.route('pull_request', base=guard.MISSION, head='unknown', same_repo=True))
        self.assertFalse(guard.route('pull_request', base=guard.MISSION, head=guard.BRANCH, same_repo=False))

    def test_actual_xml_requires_pass(self):
        self.write('xml/TEST-case.xml', '<testsuite><testcase classname="c" name="t"><failure/></testcase></testsuite>')
        with self.assertRaises(AssertionError): guard.read_cases(self.root / 'xml')

    def test_actual_xml_requires_execution(self):
        with self.assertRaises(AssertionError): guard.read_cases(self.root / 'missing')
        self.write('xml/TEST-case.xml', '<testsuite><testcase classname="c" name="t"/></testsuite>')
        self.assertEqual(guard.read_cases(self.root / 'xml'), {('c', 't')})

    def test_actual_xml_skipped_refused(self):
        self.write('xml/TEST-case.xml', '<testsuite><testcase classname="c" name="t"><skipped/></testcase></testsuite>')
        with self.assertRaises(AssertionError): guard.read_cases(self.root / 'xml')

    def test_actual_xml_duplicate_refused(self):
        self.write('xml/TEST-case.xml', '<testsuite><testcase classname="c" name="t"/><testcase classname="c" name="t"/></testsuite>')
        with self.assertRaises(AssertionError): guard.read_cases(self.root / 'xml')


if __name__ == '__main__':
    unittest.main()
