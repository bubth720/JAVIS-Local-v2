#!/usr/bin/env bash
# Unmodified upstream ONLY. Run separately for each fresh source tree.
set -euo pipefail
if [ "$#" -ne 4 ]; then
    echo 'Usage: baseline_build.sh FRESH_UPSTREAM GRADLE_8_9 EVIDENCE_DIR RUN_ID' >&2
    exit 2
fi
alpha_source=$(realpath "$1")
alpha_gradle=$(realpath "$2")
alpha_evidence=$(realpath "$3")
alpha_run=$4
alpha_core=$(cd "$(dirname "$0")/../.." && pwd)
case "$alpha_source/" in "$alpha_core/"*) echo 'Source must be outside CORE' >&2; exit 2;; esac
test ! -e "$alpha_source/library/build"
test ! -e "$alpha_source/library/.cxx"
test ! -e "$alpha_source/library/src/main/jni/measured.inc"
test "$(sha256sum "$alpha_source/library/src/main/jni/llama_jni.cpp" | cut -d' ' -f1)" = a1f5c8bd8e68b83f6bbd1f1eee5a52170ea3c3f7123b22989e52cebda803b31f
"$alpha_gradle" --version | grep -Fx 'Gradle 8.9' >/dev/null
export ALPHA1_INVENTORY="$alpha_evidence/dependencies-$alpha_run.json"
export CMAKE_BUILD_PARALLEL_LEVEL=4
cd "$alpha_source"
"$alpha_gradle" :library:assembleRelease :library:alpha1DependencyInventory \
    --init-script "$alpha_core/tools/alpha1-003/inventory.init.gradle" \
    --no-daemon --no-build-cache --rerun-tasks --max-workers=4 --console=plain
