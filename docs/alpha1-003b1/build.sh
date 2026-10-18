#!/usr/bin/env bash
# Build an ALREADY patched isolated copy. No model/endpoint/install/publication task.
set -euo pipefail
if [ "$#" -ne 4 ]; then
  echo 'Usage: build.sh ISOLATED_COPY GRADLE89 EVIDENCE_DIRECTORY RUN_ID' >&2
  exit 2
fi
core_root=$(cd "$(dirname "$0")/../.." && pwd)
lab_source=$(realpath "$1")
lab_gradle=$(realpath "$2")
lab_evidence=$(realpath "$3")
lab_run=$4
case "$lab_source/" in "$core_root/"*) echo 'Refusing CORE checkout' >&2; exit 2;; esac
python3 - "$core_root" "$lab_source" <<'PY'
import pathlib,json,sys,hashlib
core,lab=map(pathlib.Path,sys.argv[1:])
patch=core/'docs/alpha1-003a/PATCH-LIBRARY-A-VALIDER.patch'
assert hashlib.sha256(patch.read_bytes()).hexdigest()=='329fc1959a2ac990f84e7b63f61239b75dd63d13b478b1f974f37b9c4a3ef97b'
manifest=json.loads((core/'docs/alpha1-003a/PATCH_FILES.json').read_text())
assert len(manifest)==7 and all(n.startswith('library/') for n in manifest)
for n,v in manifest.items():assert hashlib.sha256((lab/n).read_bytes()).hexdigest()==v['proposed_sha256'],n
assert not (lab/'library/build').exists() and not (lab/'library/.cxx').exists(), 'Clean outputs required'
PY
"$lab_gradle" --version | grep -Fx 'Gradle 8.9' >/dev/null
export ALPHA1_DEPENDENCY_LOCK="$core_root/docs/alpha1-003/RESULTS.json"
export ALPHA1_INVENTORY="$lab_evidence/dependencies-$lab_run.json"
export CMAKE_BUILD_PARALLEL_LEVEL=4
cd "$lab_source"
"$lab_gradle" :library:assembleRelease :library:alpha1DependencyInventory \
  --init-script "$core_root/tools/alpha1-003/inventory.init.gradle" \
  --offline --no-daemon --no-build-cache --rerun-tasks --max-workers=4 --console=plain
