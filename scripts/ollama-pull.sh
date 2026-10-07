#!/bin/sh
# Baixa o modelo do Ollama no host. Usado quando o backend roda FORA do
# Docker (Ollama local ou externo) — dentro do compose quem puxa o modelo e
# o servico one-shot "ollama-pull" (docker-compose.yml).
#
# Variaveis lidas: AI_MODEL (default qwen2.5:7b) e OLLAMA_HOST
# (default http://localhost:11434).
set -eu

OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"
MODEL="${AI_MODEL:-qwen2.5:7b}"

if ! command -v ollama >/dev/null 2>&1; then
  echo "ERRO: binario 'ollama' nao encontrado no PATH." >&2
  echo "Instale em https://ollama.com/download ou rode por docker (docker compose run --rm ollama-pull)." >&2
  exit 1
fi

echo "OLLAMA_HOST=${OLLAMA_HOST}"
echo "puxando o modelo ${MODEL} (pode demorar na primeira vez)..."
OLLAMA_HOST="${OLLAMA_HOST}" ollama pull "${MODEL}"
echo "modelo ${MODEL} pronto"