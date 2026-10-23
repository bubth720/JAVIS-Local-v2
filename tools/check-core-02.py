#!/usr/bin/env python3
"""Scope guard against the exact CORE-02 base; does not weaken historical guards."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "b98bd1c4722cd053021d4021717b73ff595d5fed"
ALLOWED = {
    "AGENTS.md", "docs/PROJECT_KNOWLEDGE.md", "tools/check-core-02.py",
    ".github/workflows/core-02-intensive.yml",
    "app/src/main/java/com/stark/jarvislocal/AppGraph.kt",
    "app/src/main/java/com/stark/jarvislocal/SessionCoordinator.kt",
}
PREFIXES = (
    "app/src/main/java/com/stark/jarvislocal/events/",
    "app/src/test/java/com/stark/jarvislocal/events/",
    "docs/intensive-2026-10-22/",
)

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)

changed = set(git("diff", "--name-only", BASE).splitlines())
# Kotlin creates transient untracked daemon sessions; tracked cache files are still rejected.
changed.update(p for p in git("ls-files", "--others", "--exclude-standard").splitlines() if not p.startswith(".kotlin/"))
violations = sorted(p for p in changed if p not in ALLOWED and not p.startswith(PREFIXES))
if violations:
    raise SystemExit("CORE-02 scope violation: " + ", ".join(violations))
base_paths = set(git("ls-tree", "-r", "--name-only", BASE).splitlines())
protected = base_paths - ALLOWED
for name in protected:
    if git("diff", "--name-only", BASE, "--", name).strip():
        raise SystemExit("Historical file modified: " + name)
print(json.dumps({"base": BASE, "protected_files": len(protected), "changed": sorted(changed),
                  "native_or_contract_changes": False}, ensure_ascii=False))
