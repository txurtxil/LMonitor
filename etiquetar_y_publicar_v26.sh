#!/usr/bin/env bash
set -euo pipefail
cd ~/LMonitor

mv arreglar_commit_y_tag_v25.sh ~/sesion_lmonitor_scripts/ 2>/dev/null || true

echo "=== creando tag v2.6 (mismo contenido que v2.5, marca limpia) ==="
git tag -a v2.6 -m "LMonitor 2.6

Misma app que v2.5 (launcher de accesos directos + enlace de apoyo
Ko-fi en About) - version limpia tras el reetiquetado de v2.5."

echo ""
echo "=== empujando la tag ==="
git push origin v2.6

echo ""
echo "=== compilando ==="
./gradlew assembleDebug 2>&1 | tail -10
ls -la app/build/outputs/apk/debug/

echo ""
echo "=== publicando la release ==="
bash release_apk.sh v2.6 "LMonitor 2.6"
