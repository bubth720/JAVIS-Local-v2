#!/usr/bin/env python3
"""Scope guard against the exact ID-01 base; does not weaken historical guards."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "e86107b8213b0aeaf9d22025cf6bef91a8e544a5"
ALLOWED = {
    "AGENTS.md", "docs/PROJECT_KNOWLEDGE.md", "tools/check-id-01.py",
    ".github/workflows/id-01-intensive.yml",
    "app/src/main/java/com/stark/jarvislocal/identity/IdentityComposer.kt",
    "app/src/main/java/com/stark/jarvislocal/identity/IdentityStyleFormatter.kt",
}
PREFIXES = (
    "app/src/main/java/com/stark/jarvislocal/identity/profiles/",
    "app/src/test/java/com/stark/jarvislocal/identity/profiles/",
    "docs/intensive-2026-10-22/",
)

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)

changed = set(git("diff", "--name-only", BASE).splitlines())
# Kotlin creates transient untracked daemon sessions; tracked cache files are still rejected.
changed.update(p for p in git("ls-files", "--others", "--exclude-standard").splitlines() if not p.startswith(".kotlin/"))
violations = sorted(p for p in changed if p not in ALLOWED and not p.startswith(PREFIXES))
if violations:
    raise SystemExit("ID-01 scope violation: " + ", ".join(violations))
base_paths = set(git("ls-tree", "-r", "--name-only", BASE).splitlines())
protected = base_paths - ALLOWED
for name in protected:
    if git("diff", "--name-only", BASE, "--", name).strip():
        raise SystemExit("Historical file modified: " + name)
print(json.dumps({"base": BASE, "protected_files": len(protected), "changed": sorted(changed),
                  "native_or_contract_changes": False}, ensure_ascii=False))

# The only authorized old-composer change is a pure extraction of its exact fixed renderer.
path = "app/src/main/java/com/stark/jarvislocal/identity/IdentityComposer.kt"
old = git("show", BASE + ":" + path)
start = old.index("        val styles = snapshot.styles.values()")
end = old.index("        requireIdentity(IdentityCodec.utf8(fragment)", start)
expected = old[:start] + "        val fragment = IdentityStyleFormatter.render(snapshot.styles)\n" + old[end:]
assert (ROOT / path).read_text() == expected, "Composer changed beyond exact pure extraction"
block = old[start:end]
body = block[block.index("        val fragment = buildString {"):].replace("        val fragment = buildString {", "        return buildString {", 1)
formatter = (ROOT / "app/src/main/java/com/stark/jarvislocal/identity/IdentityStyleFormatter.kt").read_text()
assert body in formatter, "Original SYSTEM bytes/renderer modified"
