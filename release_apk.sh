#!/usr/bin/env bash
set -euo pipefail

# Uso: bash release_apk.sh <tag> "<titulo>" [notas.md]
# Ejemplo: bash release_apk.sh diag-1 "Session log + resolucion real" /tmp/notas.md

TAG="${1:?Uso: release_apk.sh <tag> \"<titulo>\" [notas.md]}"
TITLE="${2:?Uso: release_apk.sh <tag> \"<titulo>\" [notas.md]}"
NOTES_FILE="${3:-}"

echo "=== compilando ==="
./gradlew assembleDebug

APK=$(ls app/build/outputs/apk/debug/*.apk | head -1)
echo "APK: $APK"

echo ""
echo "=== creando release $TAG ==="
if [ -n "$NOTES_FILE" ]; then
    gh release create "$TAG" "$APK" --title "$TITLE" --notes-file "$NOTES_FILE"
else
    gh release create "$TAG" "$APK" --title "$TITLE" --generate-notes
fi

echo ""
echo "=== URL de la release ==="
gh release view "$TAG" --json url -q .url
