#!/usr/bin/env python3
"""Scope guard against the exact CORE-01 base; does not weaken historical guards."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "a6f13dd101990c6b5e3992be506c9a5b99873621"
ALLOWED = {
    "AGENTS.md", "docs/PROJECT_KNOWLEDGE.md", "tools/check-core-01.py",
    ".github/workflows/core-01-intensive.yml",
    "app/src/mvp/java/com/stark/jarvislocal/MvpRuntime.kt",
    "app/src/mvp2/java/com/stark/jarvislocal/Mvp2Activity.kt",
    "app/src/mvp2/java/com/stark/jarvislocal/Mvp2Application.kt",
    "app/src/testMvp2/java/com/stark/jarvislocal/MvpIntentUiTest.kt",
}
PREFIXES = (
    "app/src/main/java/com/stark/jarvislocal/orchestration/",
    "app/src/test/java/com/stark/jarvislocal/orchestration/",
    "docs/intensive-2026-10-22/",
)

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)

changed = set(git("diff", "--name-only", BASE).splitlines())
# Kotlin creates transient untracked daemon sessions; tracked cache files are still rejected.
changed.update(p for p in git("ls-files", "--others", "--exclude-standard").splitlines() if not p.startswith(".kotlin/"))
violations = sorted(p for p in changed if p not in ALLOWED and not p.startswith(PREFIXES))
if violations:
    raise SystemExit("CORE-01 scope violation: " + ", ".join(violations))
base_paths = set(git("ls-tree", "-r", "--name-only", BASE).splitlines())
protected = base_paths - ALLOWED
for name in protected:
    if git("diff", "--name-only", BASE, "--", name).strip():
        raise SystemExit("Historical file modified: " + name)
print(json.dumps({"base": BASE, "protected_files": len(protected), "changed": sorted(changed),
                  "native_or_contract_changes": False}, ensure_ascii=False))
