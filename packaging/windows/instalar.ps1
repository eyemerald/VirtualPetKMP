# Instalador de Virtual Pet para Windows.
#
# Instala por usuario (en %LOCALAPPDATA%\Programs), así que **no pide permisos de administrador** y
# no toca nada del sistema fuera de tus accesos directos y su entrada en "Aplicaciones instaladas".
# Se puede desinstalar desde ahí o con "Desinstalar Virtual Pet" en el menú de inicio.
#
# Los DATOS (mascotas, historial, copias) NO viven aquí: están en %USERPROFILE%\.virtualpet y
# desinstalar la app no los borra.

$ErrorActionPreference = "Stop"

$nombre = "Virtual Pet"
# Versión que se muestra en "Aplicaciones instaladas". jpackage no deja la versión en ningún sitio
# que se pueda leer en tiempo de instalación, así que se escribe aquí en cada entrega.
$version = "1.12"
$origen = Join-Path $PSScriptRoot $nombre
$destino = Join-Path $env:LOCALAPPDATA "Programs\$nombre"
$ejecutable = Join-Path $destino "$nombre.exe"

if (-not (Test-Path (Join-Path $origen "$nombre.exe"))) {
    Write-Host "No encuentro la carpeta '$nombre' junto a este instalador." -ForegroundColor Red
    Write-Host "Descomprime el ZIP entero y vuelve a ejecutar INSTALAR.bat desde dentro." -ForegroundColor Red
    Read-Host "Pulsa Enter para salir"
    exit 1
}

Write-Host "Instalando $nombre en $destino ..."
New-Item -ItemType Directory -Force -Path $destino | Out-Null

# robocopy devuelve 0-7 cuando ha ido bien (1 = copió ficheros).
robocopy $origen $destino /MIR /NFL /NDL /NJH /NJS /R:1 /W:1 | Out-Null
if ($LASTEXITCODE -ge 8) {
    Write-Host "No se pudo copiar la aplicación (código $LASTEXITCODE)." -ForegroundColor Red
    Read-Host "Pulsa Enter para salir"
    exit 1
}

# --- Accesos directos -------------------------------------------------------------------------
$shell = New-Object -ComObject WScript.Shell

function Nuevo-AccesoDirecto($carpeta, $nombreLnk, $destinoExe) {
    if (-not (Test-Path $carpeta)) { return }
    $ruta = Join-Path $carpeta $nombreLnk
    $acceso = $shell.CreateShortcut($ruta)
    $acceso.TargetPath = $destinoExe
    $acceso.WorkingDirectory = Split-Path $destinoExe -Parent
    $acceso.IconLocation = "$destinoExe,0"
    $acceso.Description = "Diario de salud y peso de tus mascotas"
    $acceso.Save()
}

Nuevo-AccesoDirecto ([Environment]::GetFolderPath("Desktop")) "$nombre.lnk" $ejecutable
Nuevo-AccesoDirecto (Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs") "$nombre.lnk" $ejecutable

# --- Desinstalador ----------------------------------------------------------------------------
$desinstalador = Join-Path $destino "Desinstalar $nombre.cmd"
@"
@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0desinstalar.ps1"
"@ | Set-Content -Path $desinstalador -Encoding ASCII

Copy-Item (Join-Path $PSScriptRoot "desinstalar.ps1") (Join-Path $destino "desinstalar.ps1") -Force
Copy-Item (Join-Path $PSScriptRoot "LEEME.txt") (Join-Path $destino "LEEME.txt") -Force

# --- Entrada en "Aplicaciones instaladas" -----------------------------------------------------
$clave = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\VirtualPetKMP"
New-Item -Path $clave -Force | Out-Null
$tamanoKb = [int]((Get-ChildItem $destino -Recurse -File | Measure-Object Length -Sum).Sum / 1KB)
Set-ItemProperty -Path $clave -Name "DisplayName" -Value $nombre
Set-ItemProperty -Path $clave -Name "DisplayVersion" -Value $version
Set-ItemProperty -Path $clave -Name "Publisher" -Value "Virtual Pet"
Set-ItemProperty -Path $clave -Name "InstallLocation" -Value $destino
Set-ItemProperty -Path $clave -Name "DisplayIcon" -Value "$ejecutable,0"
Set-ItemProperty -Path $clave -Name "UninstallString" -Value "powershell -NoProfile -ExecutionPolicy Bypass -File `"$destino\desinstalar.ps1`""
Set-ItemProperty -Path $clave -Name "QuietUninstallString" -Value "powershell -NoProfile -ExecutionPolicy Bypass -File `"$destino\desinstalar.ps1`" -silencioso"
Set-ItemProperty -Path $clave -Name "EstimatedSize" -Value $tamanoKb -Type DWord
Set-ItemProperty -Path $clave -Name "NoModify" -Value 1 -Type DWord
Set-ItemProperty -Path $clave -Name "NoRepair" -Value 1 -Type DWord

Write-Host ""
Write-Host "Listo. $nombre esta instalado y tienes un acceso directo en el escritorio." -ForegroundColor Green
Write-Host "Tus datos siguen en $env:USERPROFILE\.virtualpet (no se tocan al desinstalar)."
Write-Host ""
Start-Process $ejecutable
