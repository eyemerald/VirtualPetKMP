# Desinstalador de Virtual Pet (Windows).
#
# Quita la aplicación, sus accesos directos y su entrada en "Aplicaciones instaladas".
# **No borra tus datos**: están en %USERPROFILE%\.virtualpet (mascotas, historial, copias de
# seguridad). Si quieres borrarlos también, tienes que eliminar esa carpeta a mano, y la app avisa
# de ello en el propio instalador.

param([switch]$silencioso)

$ErrorActionPreference = "Continue"

$nombre = "Virtual Pet"
$destino = Join-Path $env:LOCALAPPDATA "Programs\$nombre"

Write-Host "Desinstalando $nombre ..."

# Cerrar la app si está abierta, para poder borrar sus ficheros.
Get-Process -Name $nombre -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  cerrando la aplicación (proceso $($_.Id))"
    $_.CloseMainWindow() | Out-Null
    Start-Sleep -Milliseconds 800
    if (-not $_.HasExited) { $_.Kill() }
}
Start-Sleep -Milliseconds 500

# Accesos directos
$accesos = @(
    (Join-Path ([Environment]::GetFolderPath("Desktop")) "$nombre.lnk"),
    (Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\$nombre.lnk")
)
foreach ($acceso in $accesos) {
    if (Test-Path $acceso) {
        Remove-Item $acceso -Force
        Write-Host "  quitado $acceso"
    }
}

# Entrada de "Aplicaciones instaladas"
$clave = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\VirtualPetKMP"
if (Test-Path $clave) {
    Remove-Item $clave -Recurse -Force
    Write-Host "  quitada la entrada de Aplicaciones instaladas"
}

# Carpeta de la aplicación
if (Test-Path $destino) {
    Remove-Item $destino -Recurse -Force
    Write-Host "  borrada la carpeta $destino"
}

$datos = Join-Path $env:USERPROFILE ".virtualpet"
Write-Host ""
Write-Host "$nombre se ha desinstalado." -ForegroundColor Green
if (Test-Path $datos) {
    Write-Host "Tus datos siguen en: $datos" -ForegroundColor Yellow
    Write-Host "Si querías borrarlo TODO, elimina esa carpeta a mano (dentro está la base de datos y las copias)."
}

if (-not $silencioso) {
    Write-Host ""
    Read-Host "Pulsa Enter para cerrar"
}
