#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/models"
mkdir -p "$DEST"

LLM_URL="https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf?download=true"
WHISPER_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin?download=true"

echo "Téléchargement du LLM local…"
curl -L --fail --retry 3 --retry-delay 3 "$LLM_URL" -o "$DEST/jarvis.gguf"

echo "Téléchargement de Whisper…"
curl -L --fail --retry 3 --retry-delay 3 "$WHISPER_URL" -o "$DEST/whisper.bin"

test -s "$DEST/jarvis.gguf"
test -s "$DEST/whisper.bin"

echo "Modèles prêts :"
du -h "$DEST/jarvis.gguf" "$DEST/whisper.bin"
