#!/usr/bin/env python3
"""Documentary patch and verifier tests, in-memory reconstruction only."""
from pathlib import Path
import tempfile
import unittest
import zipfile
import sys
sys.dont_write_bytecode=True
import proposal
import check_future_aar
UPSTREAM=Path('/workspace/alpha1-003-isolated/sources/llama-android-823d1de2b3ef85ba9ef4847a03bac423f4aa1c44')
# CLI override, no hidden upstream downloads.
if '--upstream' in sys.argv:
 i=sys.argv.index('--upstream');UPSTREAM=Path(sys.argv[i+1]);del sys.argv[i:i+2]
class Review(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.original,cls.before=proposal.inputs(UPSTREAM);cls.after=proposal.revise(cls.before)
 def test_exact_two_file_scope(self):
  self.assertEqual({n for n in self.before if self.before[n]!=self.after[n]},{proposal.GRADLE,proposal.MEASURED})
 def test_no_stream_constructor_or_double_owner(self):
  s=self.after[proposal.MEASURED]
  self.assertNotIn('FileInputStream',s);self.assertNotIn('FileOutputStream',s)
  self.assertEqual(s.count('Os.open('),1);self.assertEqual(s.count('Os.close('),1)
 def test_quarantine_and_native_transaction_unchanged(self):
  for start,end in [('internal class OwnerSlot','/** Sole FD owner'),('class MeasuredReceipt',None)]:
   before=self.before[proposal.MEASURED];after=self.after[proposal.MEASURED]
   oldend='/** Private filesystem ownership' if start.startswith('internal') else end
   old=before[before.index(start):before.index(oldend) if oldend else len(before)]
   new=after[after.index(start):after.index(end) if end else len(after)]
   self.assertEqual(old,new)
 def test_identity_before_first_write_and_cancel_in_partial_write_loop(self):
  s=self.after[proposal.MEASURED]
  self.assertLess(s.index('inode = own.st_ino'),s.index('Os.write(outFd'))
  self.assertIn('while (written < count) {\n                            checkCancelled()',s)
 def test_proguard_exact_path_and_rules_source(self):
  self.assertIn('consumerProguardFiles("src/proguard-rules.pro")',self.after[proposal.GRADLE])
  lines=set((UPSTREAM/'library/src/proguard-rules.pro').read_text().splitlines())
  self.assertTrue(check_future_aar.REQUIRED<=lines)
 def test_reconstruct_overlay_and_pristine_composition(self):
  review=proposal.module('overlay_parser',proposal.ROOT/'docs/alpha1-003a/review.py')
  for before in [self.before,self.original]:
   reconstructed=dict(before)
   for name,body in review.parts(proposal.diff(before,self.after)):
    reconstructed[name]=review.reconstruct(before[name],body)
   self.assertEqual(reconstructed,self.after)
 def test_published_patch_reproducible(self):
  b=proposal.diff(self.before,self.after).encode()
  self.assertEqual(b,(proposal.HERE/'PATCH-CORRECTIONS-A-VALIDER.patch').read_bytes())
 def test_future_checker_accepts_synthetic_correct_rules(self):
  with tempfile.TemporaryDirectory() as t:
   p=Path(t)/'rules-only.zip'
   with zipfile.ZipFile(p,'w') as z:z.writestr('proguard.txt','\n'.join(sorted(check_future_aar.REQUIRED)))
   self.assertTrue(check_future_aar.verify(p).startswith('PASS'))
 def test_future_checker_rejects_missing_entry(self):
  with tempfile.TemporaryDirectory() as t:
   p=Path(t)/'rules-only.zip'
   with zipfile.ZipFile(p,'w') as z:z.writestr('unrelated','x')
   with self.assertRaisesRegex(ValueError,'absent'):check_future_aar.verify(p)
 def test_future_checker_rejects_missing_rule(self):
  for rule in check_future_aar.REQUIRED:
   with self.subTest(rule=rule),tempfile.TemporaryDirectory() as t:
    p=Path(t)/'rules-only.zip'
    with zipfile.ZipFile(p,'w') as z:z.writestr('proguard.txt','\n'.join(check_future_aar.REQUIRED-{rule}))
    with self.assertRaisesRegex(ValueError,'missing'):check_future_aar.verify(p)
 def test_b1_artifact_expected_negative_control(self):
  p=proposal.ROOT.parent/'alpha1-003b1-isolated/evidence/candidate-a.aar'
  self.assertTrue(p.is_file(),'Provide preserved B1 artifact for negative control')
  with self.assertRaisesRegex(ValueError,'absent'):check_future_aar.verify(p)
if __name__=='__main__':unittest.main(verbosity=2)
