import sys, shutil, datetime, os
import xml.etree.ElementTree as ET

KOTLIN_FILE = "/home/txurtxil/LMonitor/app/src/main/java/com/chiller3/mirrormobile/settings/SettingsScreen.kt"
STRINGS_EN = "/home/txurtxil/LMonitor/app/src/main/res/values/strings.xml"
STRINGS_ES = "/home/txurtxil/LMonitor/app/src/main/res/values-es/strings.xml"
BACKUP_DIR = "/home/txurtxil/LMonitor/backups"
DRY_ROOT = "/tmp/pruebatest"
TS = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")

APPLY = "--apply" in sys.argv


def paren_balance(s):
    return (s.count("("), s.count(")"), s.count("{"), s.count("}"))


def xml_valid(s):
    try:
        ET.fromstring(s.encode("utf-8"))
        return True
    except ET.ParseError as e:
        print(f"  XML invalido: {e}")
        return False


def apply_edits(path, edits):
    with open(path, "r", encoding="utf-8") as f:
        original = f.read()

    content = original
    for anchor, replacement in edits:
        count = content.count(anchor)
        if count != 1:
            print(f"ABORTO en {path}: ancla aparece {count} veces (se esperaba 1)")
            print(f"  ancla: {anchor[:80]!r}...")
            return None, None
        content = content.replace(anchor, replacement, 1)

    return original, content


def process(path, edits, kind):
    original, new = apply_edits(path, edits)
    if new is None:
        return False

    if kind == "kotlin":
        ob = paren_balance(original)
        nb = paren_balance(new)
        print(f"{path}: balance original {ob} -> nuevo {nb}")
        if nb[0] != nb[1] or nb[2] != nb[3]:
            print(f"ABORTO en {path}: parentesis o llaves desbalanceados tras el patch")
            return False
    elif kind == "xml":
        if not xml_valid(original):
            print(f"AVISO: {path} original ya no parseaba con ElementTree, salto la validacion")
        elif not xml_valid(new):
            print(f"ABORTO en {path}: el XML resultante no es valido")
            return False
        print(f"{path}: XML valido tras el patch")

    rel_marker = path.replace("/home/txurtxil/LMonitor/", "")

    if not APPLY:
        dry_path = os.path.join(DRY_ROOT, rel_marker)
        os.makedirs(os.path.dirname(dry_path), exist_ok=True)
        with open(dry_path, "w", encoding="utf-8") as f:
            f.write(new)
        print(f"  dry-run OK -> {dry_path}")
        print(f"  revisa con: diff -u {path} {dry_path}")
        return True

    os.makedirs(BACKUP_DIR, exist_ok=True)
    backup_path = os.path.join(BACKUP_DIR, f"{os.path.basename(path)}.bak_{TS}")
    shutil.copy2(path, backup_path)
    with open(path, "w", encoding="utf-8") as f:
        f.write(new)
    print(f"  APLICADO -> {path} (backup en {backup_path})")
    return True


# ============ SettingsScreen.kt ============

kt_edits = []

# 1. Declarar el remember del nuevo pref
kt_edits.append((
'''    val autoStart = remember(reloadPrefs) { prefs.autoStart }
    val wakeLock = remember(reloadPrefs) { prefs.wakeLock }''',
'''    val autoStart = remember(reloadPrefs) { prefs.autoStart }
    val wakeLock = remember(reloadPrefs) { prefs.wakeLock }
    val stopOnDisconnect = remember(reloadPrefs) { prefs.stopOnDisconnect }'''
))

# 2. Pasarlo a SettingsContent(...)
kt_edits.append((
'''            autoStart = autoStart,
            wakeLock = wakeLock,
            speedThreshold = speedThreshold,''',
'''            autoStart = autoStart,
            wakeLock = wakeLock,
            stopOnDisconnect = stopOnDisconnect,
            speedThreshold = speedThreshold,'''
))

# 3. Callback de escritura
kt_edits.append((
'''            onAutoStartChange = { enabled ->
                prefs.autoStart = enabled
                reloadPrefs++
            },
            onWakeLockChange = { enabled ->
                prefs.wakeLock = enabled
                reloadPrefs++
            },''',
'''            onAutoStartChange = { enabled ->
                prefs.autoStart = enabled
                reloadPrefs++
            },
            onWakeLockChange = { enabled ->
                prefs.wakeLock = enabled
                reloadPrefs++
            },
            onStopOnDisconnectChange = { enabled ->
                prefs.stopOnDisconnect = enabled
                reloadPrefs++
            },'''
))

# 4. Parametro del composable (tipo Boolean)
kt_edits.append((
'''    autoStart: Boolean,
    wakeLock: Boolean,
    speedThreshold: Float,''',
'''    autoStart: Boolean,
    wakeLock: Boolean,
    stopOnDisconnect: Boolean,
    speedThreshold: Float,'''
))

# 5. Parametro del composable (callback)
kt_edits.append((
'''    onAutoStartChange: (Boolean) -> Unit,
    onWakeLockChange: (Boolean) -> Unit,
    onSpeedThresholdCycle: () -> Unit,''',
'''    onAutoStartChange: (Boolean) -> Unit,
    onWakeLockChange: (Boolean) -> Unit,
    onStopOnDisconnectChange: (Boolean) -> Unit,
    onSpeedThresholdCycle: () -> Unit,'''
))

# 6. Nuevo item() en la lista, entre wake_lock y speed_threshold
#    OJO: incluye la linea en blanco real entre bloques (confirmado con cat -A)
kt_edits.append((
'''        item(key = "wake_lock") {
            SwitchPreference(
                checked = wakeLock,
                onCheckedChange = onWakeLockChange,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_wake_lock_name)) },
                summary = { Text(text = stringResource(R.string.pref_wake_lock_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "speed_threshold") {''',
'''        item(key = "wake_lock") {
            SwitchPreference(
                checked = wakeLock,
                onCheckedChange = onWakeLockChange,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_wake_lock_name)) },
                summary = { Text(text = stringResource(R.string.pref_wake_lock_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "stop_on_disconnect") {
            SwitchPreference(
                checked = stopOnDisconnect,
                onCheckedChange = onStopOnDisconnectChange,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_stop_on_disconnect_name)) },
                summary = { Text(text = stringResource(R.string.pref_stop_on_disconnect_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "speed_threshold") {'''
))

# 7. Preview: valor
kt_edits.append((
'''                autoStart = true,
                wakeLock = true,
                speedThreshold = Preferences.SPEED_THRESHOLD_PRESETS[0],''',
'''                autoStart = true,
                wakeLock = true,
                stopOnDisconnect = true,
                speedThreshold = Preferences.SPEED_THRESHOLD_PRESETS[0],'''
))

# 8. Preview: callback vacio
kt_edits.append((
'''                onAutoStartChange = {},
                onWakeLockChange = {},''',
'''                onAutoStartChange = {},
                onWakeLockChange = {},
                onStopOnDisconnectChange = {},'''
))

# ============ strings.xml (EN) ============

en_edits = [(
'''    <string name="pref_wake_lock_name">Keep screen on</string>
    <string name="pref_wake_lock_desc">Prevent the screen from turning off while mirroring is active. The screen will still dim when idle.</string>''',
'''    <string name="pref_wake_lock_name">Keep screen on</string>
    <string name="pref_wake_lock_desc">Prevent the screen from turning off while mirroring is active. The screen will still dim when idle.</string>
    <string name="pref_stop_on_disconnect_name">Stop mirroring on disconnect</string>
    <string name="pref_stop_on_disconnect_desc">Fully release screen capture when Android Auto disconnects. Turn off to keep the capture session running in the background so mirroring can resume without asking for permission again, at the cost of a persistent notification and battery usage while disconnected.</string>'''
)]

# ============ strings.xml (ES) ============

es_edits = [(
'''    <string name="pref_wake_lock_name">Mantener pantalla encendida</string>
    <string name="pref_wake_lock_desc">Evita que la pantalla se apague mientras el mirroring está activo. Seguirá atenuándose si no hay actividad.</string>''',
'''    <string name="pref_wake_lock_name">Mantener pantalla encendida</string>
    <string name="pref_wake_lock_desc">Evita que la pantalla se apague mientras el mirroring está activo. Seguirá atenuándose si no hay actividad.</string>
    <string name="pref_stop_on_disconnect_name">Detener mirroring al desconectar</string>
    <string name="pref_stop_on_disconnect_desc">Libera por completo la captura de pantalla al desconectar Android Auto. Desactívalo para mantener la sesión de captura en segundo plano y reanudar sin volver a pedir permiso, a costa de una notificación persistente y consumo de batería mientras esté desconectado.</string>'''
)]

if os.path.exists(DRY_ROOT) and not APPLY:
    shutil.rmtree(DRY_ROOT)

ok = True
ok &= process(KOTLIN_FILE, kt_edits, kind="kotlin")
ok &= process(STRINGS_EN, en_edits, kind="xml")
ok &= process(STRINGS_ES, es_edits, kind="xml")

print("")
if not ok:
    print("HUBO ERRORES. No se ha escrito nada definitivo. Revisa los mensajes de arriba.")
    sys.exit(1)
elif APPLY:
    print("Patch aplicado a los ficheros reales. Ahora: compilar y probar en dispositivo real.")
else:
    print("Dry-run OK. Revisa los diffs y, si cuadra, vuelve a correr con --apply.")
