#!/usr/bin/env bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "gradle is not installed. Install it first (e.g. brew install gradle)." >&2
  exit 1
fi

gradle wrapper --gradle-version 8.10.2
