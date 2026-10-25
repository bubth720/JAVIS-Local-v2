#!/usr/bin/env python3
"""Offline FUSION-01 scope/lineage/lock/test-inventory validator. Does not authorize activation."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BASE = "aea5ac859ac551d54fe16187c1883a487dbee9d6"
ALLOWED_EXISTING = {
    "AGENTS.md", "docs/PROJECT_KNOWLEDGE.md", "integration-lab/README.md",
    "integration-lab/src/main/kotlin/com/stark/jarvislocal/integrations/CoreIntegrationLab.kt",
    "integration-lab/src/main/kotlin/com/stark/jarvislocal/integrations/ServicesToolAdapter.kt",
    "integration-lab/src/main/kotlin/com/stark/jarvislocal/integrations/NexusSearchAdapter.kt",
}
NEW = {"tools/check-fusion-01.py", ".github/workflows/fusion-01-core.yml"}
PREFIXES = ("docs/fusion-01/", "integration-lab/src/")

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()

def validate(lock):
    assert lock["schema"] == "jarvis.core.fusion-lock/1"
    assert lock["base"] == BASE
    assert lock["global_f0"] == "NOT_ATTESTED"  # local lock is not UNIFIED approval
    assert lock["android_integration"] == "BLOCKED"
    rows = lock["observed_candidates"]
    assert {r["module"] for r in rows} == {"CORE", "LLM", "VOICE", "INTERFACE", "NEXUS", "SERVICES", "EVOLUTION", "UNIFIED"}
    assert len(rows) == 8
    for row in rows:
        assert re.fullmatch(r"[0-9a-f]{40}", row["sha"])
        assert re.fullmatch(r"[0-9a-f]{40}", row["git_tree"])
        assert row["reference"] and row["repository"].startswith("bubth720/")
        assert row["role"] in {"BASE", "READ_ONLY_REFERENCE"}
    delivery = lock["unified_f0_delivery"]
    assert delivery["commit"] == "3caa57d91ce0dba31043e7cb6373a5e2df8b9fa4"
    assert set(delivery["files"]) == {"docs/fusion-01/reference/UNIFIED-integration-lock.json", "docs/fusion-01/reference/UNIFIED-REPORT.md"}
    for path, digest in delivery["files"].items():
        assert hashlib.sha256((ROOT / path).read_bytes()).hexdigest() == digest, path
    unified = json.loads((ROOT / "docs/fusion-01/reference/UNIFIED-integration-lock.json").read_text())
    assert lock["primary_candidate_composition"] == {c["id"]: c["primary"]["commit"] for c in unified["components"]}
    sources = json.loads((ROOT / "docs/intensive-2026-10-22/CORE03-SOURCES.json").read_text())
    assert lock["consumed_jvm_sources"] == sources
    for name, digest in lock["preserved_contract_files"].items():
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest, name
    assert lock["gates"] == {"C2": "BLOCKED", "C3": "DESIGN_ONLY", "C4": "BLOCKED"}
    edges = lock["dependency_edges"]
    def visit(node, path):
        assert node not in path, "Dependency cycle"
        for parent, child in edges:
            if parent == node:
                visit(child, path + [node])
    for node, _ in edges:
        visit(node, [])
    for ancestor in ["49ad428e94edd75c2a1d115c8c2504f26609eace", "283423cb8b9d567539f33cf7f2f1fafa602cf974"]:
        subprocess.run(["git", "merge-base", "--is-ancestor", ancestor, BASE], cwd=ROOT, check=True)
    protected = set(git("ls-tree", "-r", "--name-only", BASE).splitlines()) - ALLOWED_EXISTING
    changed = set(git("diff", "--name-only", BASE).splitlines())
    assert not (changed & protected), "Protected existing files modified: " + str(sorted(changed & protected))
    assert all(p in ALLOWED_EXISTING or p in NEW or p.startswith(PREFIXES) for p in changed), changed
    for suffix, expected in [("core-501", 501), ("fixture-36", 36), ("jvm-baseline", 10)]:
        result = json.loads((ROOT / ("docs/fusion-01/evidence/" + suffix + ".json")).read_text())
        assert result["count"] == len(result["cases"]) == expected
        assert all(c["status"] == "PASS" for c in result["cases"])
        assert len({(c["target"], c["class"], c["name"]) for c in result["cases"]}) == expected
    return {"base": BASE, "protected_existing_files": len(protected), "changed_tracked": sorted(changed),
            "baseline_cases": 501, "fixture_cases": 36, "jvm_baseline_cases": 10,
            "android_runtime_changed": False, "global_f0": "NOT_ATTESTED"}

def verify_results():
    groups = {
        "core-501": ["app/build/test-results/testDebugUnitTest", "app/build/test-results/testMvp2UnitTest"],
        "fixture-36": ["fixtures/p2-client/build/test-results/testDebugUnitTest"],
        "jvm-baseline": ["integration-lab/build/test-results/test"],
    }
    result = {}
    for name, targets in groups.items():
        actual = set()
        for target in targets:
            files = list((ROOT / target).glob("TEST-*.xml"))
            assert files, "No XML tests: " + target
            for f in files:
                for c in ET.parse(f).getroot().findall("testcase"):
                    assert all(c.find(tag) is None for tag in ["failure", "error", "skipped"]), c.attrib
                    actual.add((f.parent.name, c.get("classname"), c.get("name")))
        old = json.loads((ROOT / ("docs/fusion-01/evidence/" + name + ".json")).read_text())
        expected = {(c["target"], c["class"], c["name"]) for c in old["cases"]}
        assert expected <= actual, "Historical tests missing: " + str(expected - actual)
        if name != "jvm-baseline":
            assert actual == expected, "Baseline case inventory changed"
        result[name] = {"executed": len(actual), "failed": 0, "skipped": 0, "baseline_preserved": len(expected)}
    return result

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--lock", type=Path, default=ROOT / "docs/fusion-01/integration-lock.json")
    parser.add_argument("--verify-results", action="store_true", help="Verify actual local XML, not only committed inventory")
    args = parser.parse_args()
    result = validate(json.loads(args.lock.read_text()))
    if args.verify_results:
        result["actual_tests"] = verify_results()
    print(json.dumps(result, ensure_ascii=False, indent=2))
