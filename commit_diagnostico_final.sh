#!/usr/bin/env bash
set -euo pipefail
cd ~/LMonitor

mkdir -p ~/sesion_lmonitor_scripts

mv aplicar_diagnostico_y_compilar.sh preparar_commit_diagnostico.sh \
   ver_diffs_diagnostico.sh ver_logcat_y_relacionados.sh \
   ver_mainapplication.sh ver_strings_alertas.sh \
   ~/sesion_lmonitor_scripts/ 2>/dev/null || true

echo "=== vista previa final ==="
git add -A -n

echo ""
echo "=== comiteando ==="
git add -A
git commit -m "Add session log, driving speed setting, last-display-info diagnostics

- New SessionLog: append-only file written by this app, immune to the
  system logcat ring buffer being overwritten by other apps. Exportable
  from Settings > Debug > Save session log.
- Records every DisplayState transition and the real SurfaceContainer
  size/dpi reported by the car, shown in Settings > About > Last display
  info.
- Fix: manifest used shorthand \".ClassName\" component references, which
  resolve against the AGP namespace (com.txurtxil.lmonitor) rather than
  the actual source package (com.chiller3.mirrormobile). This broke app
  startup entirely after the identity rename. Fully qualified all 5
  affected android:name attributes.
- Fix: SessionLog.dump() threw FileNotFoundException if exported before
  the first record() call, since the log file didn't exist yet. Now
  created eagerly in init()."

echo ""
echo "=== push ==="
git push

echo ""
echo "=== log final ==="
git log --oneline -8
