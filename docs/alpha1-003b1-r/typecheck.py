#!/usr/bin/env python3
"""Compile only documentary Kotlin review files outside all source trees."""
from pathlib import Path
import sys
sys.dont_write_bytecode=True
sys.path.insert(0,str(Path(__file__).resolve().parent))
import proposal
checker=proposal.module('existing_typecheck',proposal.ROOT/'docs/alpha1-003a/typecheck.py')
def corrected(upstream):
    original,before=proposal.inputs(upstream)
    checker.b1_base=before
    return original,before,proposal.revise(before)
checker.review.candidate=corrected
checker.review.make_diff=lambda original,revised: proposal.diff(checker.b1_base,revised)
if __name__=='__main__':checker.main()
