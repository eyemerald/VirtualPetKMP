"""Genera el instalador de Windows de Virtual Pet con Inno Setup.

    python packaging\\windows\\empaquetar.py

Necesita la app ya empaquetada por jpackage (`:desktopApp:createDistributable`) y el compilador de
Inno Setup (`ISCC.exe`). Este script **no instala nada**: si falta ISCC, lo dice y se para.

La versión **no se escribe aquí ni en el .iss**: se lee de `packageVersion` en
`desktopApp/build.gradle.kts`, que es la única fuente, y se le pasa al compilador con `/DVersion`.
Así no puede volver a pasar que el nombre del instalador y la versión instalada digan cosas
distintas.
"""
import hashlib
import re
import shutil
import subprocess
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
APP = RAIZ / "desktopApp" / "build" / "compose" / "binaries" / "main" / "app" / "Virtual Pet"
GRADLE = RAIZ / "desktopApp" / "build.gradle.kts"
GUION = RAIZ / "packaging" / "windows" / "VirtualPet.iss"
DIST = RAIZ / "dist"

# Dónde suele quedar ISCC.exe. Se prueban todos antes de rendirse.
SITIOS_ISCC = [
    Path(r"C:\Program Files\Inno Setup 6\ISCC.exe"),
    Path(r"C:\Program Files (x86)\Inno Setup 6\ISCC.exe"),
    Path.home() / "AppData" / "Local" / "Programs" / "Inno Setup 6" / "ISCC.exe",
    Path(r"C:\Program Files\Inno Setup 5\ISCC.exe"),
]


def version_de_la_app() -> str:
    """La versión del escritorio, tal y como la declara Gradle (`packageVersion`)."""
    if not GRADLE.is_file():
        sys.exit(f"no encuentro {GRADLE}")
    texto = GRADLE.read_text(encoding="utf-8")
    encontrado = re.search(r'packageVersion\s*=\s*"([0-9][0-9.]*)"', texto)
    if not encontrado:
        sys.exit(f"no hay packageVersion en {GRADLE}")
    return encontrado.group(1)


def buscar_iscc() -> Path:
    """El compilador de Inno Setup, o sale con un mensaje de qué instalar y de dónde."""
    en_path = shutil.which("ISCC") or shutil.which("iscc")
    if en_path:
        return Path(en_path)
    for sitio in SITIOS_ISCC:
        if sitio.is_file():
            return sitio
    sys.exit(
        "falta Inno Setup (ISCC.exe).\n"
        "Instálalo desde https://jrsoftware.org/isdl.php (Inno Setup 6) y vuelve a lanzar esto.\n"
        "No hace falta hacerlo a mano en el PATH: el script lo busca en su carpeta de siempre."
    )


def main() -> int:
    version = version_de_la_app()

    if not APP.is_dir() or not (APP / "Virtual Pet.exe").is_file():
        sys.exit(
            f"no está la app empaquetada: {APP}\n"
            "Ejecuta antes:  .\\gradlew.bat :desktopApp:createDistributable"
        )

    iscc = buscar_iscc()
    DIST.mkdir(exist_ok=True)
    salida = DIST / f"VirtualPet-{version}-setup.exe"
    if salida.exists():
        salida.unlink()

    orden = [str(iscc), f"/DVersion={version}", f"/O{DIST}", str(GUION)]
    resultado = subprocess.run(orden, capture_output=True, text=True, errors="replace")

    # La version de Inno Setup la dice el propio compilador ("Compiler engine version: Inno Setup
    # 6.7.3"); ISCC.exe no la lleva en las propiedades del fichero (marcan 0.0.0.0).
    motor = re.search(r"Inno Setup (\d+\.\d+\.\d+)", resultado.stdout)

    # ISCC escribe mucho; solo se enseña lo que ha ido mal.
    for linea in resultado.stdout.splitlines():
        limpia = linea.strip()
        if limpia.lower().startswith(("error", "warning")):
            print("  ", limpia)

    if resultado.returncode != 0:
        print(resultado.stdout[-3000:])
        print(resultado.stderr[-2000:])
        sys.exit(f"ISCC ha fallado (código {resultado.returncode})")

    if not salida.is_file():
        sys.exit(f"ISCC ha terminado bien pero no está {salida}")

    print("ISCC       :", iscc)
    print("Inno Setup :", motor.group(1) if motor else "desconocida")
    print("version    :", version)

    hash_ = hashlib.sha256()
    with open(salida, "rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            hash_.update(bloque)

    print("instalador :", salida)
    print("tamaño     :", round(salida.stat().st_size / 1024 / 1024, 1), "MB")
    print("sha256     :", hash_.hexdigest().upper())
    return 0


if __name__ == "__main__":
    sys.exit(main())
