@echo off
rem Instalador de Virtual Pet. Doble clic y listo: no hace falta ser administrador.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar.ps1"
if errorlevel 1 pause
