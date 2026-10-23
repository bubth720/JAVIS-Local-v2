#!/usr/bin/env python3
"""Scope guard against the exact CORE-03 base; does not weaken historical guards."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "49ad428e94edd75c2a1d115c8c2504f26609eace"
ALLOWED = {
    "AGENTS.md", "docs/PROJECT_KNOWLEDGE.md", "tools/check-core-03.py",
    "tools/prepare-core03-sources.py", ".github/workflows/core-03-intensive.yml",
}
PREFIXES = ("integration-lab/", "docs/intensive-2026-10-22/")

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)

changed = set(git("diff", "--name-only", BASE).splitlines())
# Kotlin creates transient untracked daemon sessions; tracked cache files are still rejected.
changed.update(p for p in git("ls-files", "--others", "--exclude-standard").splitlines() if not p.startswith(".kotlin/"))
violations = sorted(p for p in changed if p not in ALLOWED and not p.startswith(PREFIXES))
if violations:
    raise SystemExit("CORE-03 scope violation: " + ", ".join(violations))
base_paths = set(git("ls-tree", "-r", "--name-only", BASE).splitlines())
protected = base_paths - ALLOWED
for name in protected:
    if git("diff", "--name-only", BASE, "--", name).strip():
        raise SystemExit("Historical file modified: " + name)
print(json.dumps({"base": BASE, "protected_files": len(protected), "changed": sorted(changed),
                  "native_or_contract_changes": False}, ensure_ascii=False))
