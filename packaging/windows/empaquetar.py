"""Empaqueta la app de escritorio con su instalador en un ZIP para Windows.

Estructura del ZIP:
    INSTALAR.bat
    instalar.ps1
    desinstalar.ps1
    LEEME.txt
    Virtual Pet/        (la aplicación con su runtime, generada por jpackage)
"""
import hashlib
import shutil
import zipfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
APP = RAIZ / "desktopApp" / "build" / "compose" / "binaries" / "main" / "app" / "Virtual Pet"
EMPAQUETADO = RAIZ / "packaging" / "windows"
SALIDA = RAIZ / "dist" / "VirtualPet-1.10-windows.zip"

if not APP.is_dir():
    raise SystemExit(f"no está la app empaquetada: {APP}\nEjecuta antes :desktopApp:createDistributable")

archivos = [
    (EMPAQUETADO / "INSTALAR.bat", "INSTALAR.bat"),
    (EMPAQUETADO / "instalar.ps1", "instalar.ps1"),
    (EMPAQUETADO / "desinstalar.ps1", "desinstalar.ps1"),
    (EMPAQUETADO / "LEEME.txt", "LEEME.txt"),
]

if SALIDA.exists():
    SALIDA.unlink()

contador = 0
with zipfile.ZipFile(SALIDA, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    for origen, nombre in archivos:
        if not origen.exists():
            raise SystemExit(f"falta {origen}")
        z.write(origen, nombre)
        contador += 1

    for ruta in sorted(APP.rglob("*")):
        if ruta.is_file():
            z.write(ruta, f"Virtual Pet/{ruta.relative_to(APP).as_posix()}")
            contador += 1

hash_ = hashlib.sha256()
with open(SALIDA, "rb") as f:
    for bloque in iter(lambda: f.read(1 << 20), b""):
        hash_.update(bloque)

print("zip        :", SALIDA)
print("ficheros   :", contador)
print("tamaño     :", round(SALIDA.stat().st_size / 1024 / 1024, 1), "MB")
print("sha256     :", hash_.hexdigest().upper())

# Comprobación: el ZIP contiene el ejecutable y los cuatro ficheros del instalador.
with zipfile.ZipFile(SALIDA) as z:
    nombres = set(z.namelist())
    malo = z.testzip()
for critico in ["INSTALAR.bat", "instalar.ps1", "desinstalar.ps1", "LEEME.txt", "Virtual Pet/Virtual Pet.exe"]:
    print(f"  {'OK ' if critico in nombres else 'FALTA'} {critico}")
if malo is not None:
    raise SystemExit(f"entrada corrupta: {malo}")
print("verificado: sin corrupción")
