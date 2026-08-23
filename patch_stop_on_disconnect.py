import sys, shutil, datetime, os

REPO = "/home/txurtxil/LMonitor/app/src/main/java/com/chiller3/mirrormobile"
BACKUP_DIR = "/home/txurtxil/LMonitor/backups"
DRY_ROOT = "/tmp/pruebatest"
TS = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")

APPLY = "--apply" in sys.argv


def paren_balance(s):
    return (s.count("("), s.count(")"), s.count("{"), s.count("}"))


def apply_edits(relpath, edits):
    path = os.path.join(REPO, relpath)
    with open(path, "r", encoding="utf-8") as f:
        original = f.read()

    content = original
    for anchor, replacement in edits:
        count = content.count(anchor)
        if count != 1:
            print(f"ABORTO en {relpath}: ancla aparece {count} veces (se esperaba 1)")
            print(f"  ancla: {anchor[:80]!r}...")
            return None, None
        content = content.replace(anchor, replacement, 1)

    return original, content


def process(relpath, edits):
    original, new = apply_edits(relpath, edits)
    if new is None:
        return False

    ob = paren_balance(original)
    nb = paren_balance(new)
    print(f"{relpath}: balance original {ob} -> nuevo {nb}")

    if nb[0] != nb[1] or nb[2] != nb[3]:
        print(f"ABORTO en {relpath}: parentesis o llaves desbalanceados tras el patch")
        return False

    if not APPLY:
        dry_path = os.path.join(DRY_ROOT, relpath)
        os.makedirs(os.path.dirname(dry_path), exist_ok=True)
        with open(dry_path, "w", encoding="utf-8") as f:
            f.write(new)
        print(f"  dry-run OK -> {dry_path}")
        print(f"  revisa con: diff -u {os.path.join(REPO, relpath)} {dry_path}")
        return True

    os.makedirs(BACKUP_DIR, exist_ok=True)
    backup_path = os.path.join(BACKUP_DIR, f"{os.path.basename(relpath)}.bak_{TS}")
    shutil.copy2(os.path.join(REPO, relpath), backup_path)
    with open(os.path.join(REPO, relpath), "w", encoding="utf-8") as f:
        f.write(new)
    print(f"  APLICADO -> {relpath} (backup en {backup_path})")
    return True


# --- Preferences.kt: nueva constante + nueva propiedad ---

prefs_anchor_const = '''        private const val PREF_LAUNCHER_APPS = "launcher_apps"'''
prefs_replacement_const = '''        private const val PREF_LAUNCHER_APPS = "launcher_apps"
        private const val PREF_STOP_ON_DISCONNECT = "stop_on_disconnect"'''

prefs_anchor_prop = '''    var launcherApps: Set<String>
        get() = prefs.getStringSet(PREF_LAUNCHER_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(PREF_LAUNCHER_APPS, value) }
}'''
prefs_replacement_prop = '''    var launcherApps: Set<String>
        get() = prefs.getStringSet(PREF_LAUNCHER_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(PREF_LAUNCHER_APPS, value) }

    // true (default, comportamiento original): al desconectar de Android Auto se
    // libera el permiso de MediaProjection por completo.
    // false: se mantiene la sesion de captura viva en segundo plano para reanudar
    // sin volver a pedir permiso al reconectar -- coste: notificacion persistente
    // y wakelock activos mientras el coche este desconectado.
    var stopOnDisconnect: Boolean
        get() = prefs.getBoolean(PREF_STOP_ON_DISCONNECT, true)
        set(enabled) = prefs.edit { putBoolean(PREF_STOP_ON_DISCONNECT, enabled) }
}'''

# --- DisplayScreen.kt: usar el toggle en onDestroy() ---

display_anchor = '''    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(TAG, "onDestroy()")

        unregisterSpeedListener()

        state.getTransitionOrNull(DisplayState.StopMirroring::class.java)
            ?.stopMirroring()
            ?.let { state = it }

        onBinderGone()'''
display_replacement = '''    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(TAG, "onDestroy()")

        unregisterSpeedListener()

        if (prefs.stopOnDisconnect) {
            state.getTransitionOrNull(DisplayState.StopMirroring::class.java)
                ?.stopMirroring()
                ?.let { state = it }
        } else {
            // Mantiene la sesion de captura viva (solo se separa la Surface) para
            // poder reanudar sin volver a pedir permiso cuando Android Auto reconecte.
            state.getTransitionOrNull(DisplayState.DetachSurface::class.java)
                ?.detachSurface()
                ?.let { state = it }
        }

        onBinderGone()'''

if os.path.exists(DRY_ROOT) and not APPLY:
    shutil.rmtree(DRY_ROOT)

ok = True
ok &= process("Preferences.kt", [
    (prefs_anchor_const, prefs_replacement_const),
    (prefs_anchor_prop, prefs_replacement_prop),
])
ok &= process("mirror/DisplayScreen.kt", [
    (display_anchor, display_replacement),
])

print("")
if not ok:
    print("HUBO ERRORES. No se ha escrito nada definitivo. Revisa los mensajes de arriba.")
    sys.exit(1)
elif APPLY:
    print("Patch aplicado a los ficheros reales. Ahora: compilar y probar en dispositivo real.")
else:
    print("Dry-run OK. Revisa los diffs y, si cuadra, vuelve a correr con --apply.")
