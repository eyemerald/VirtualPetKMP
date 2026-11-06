; Instalador de Virtual Pet para Windows (Inno Setup 6).
;
; Sustituye al INSTALAR.bat + instalar.ps1 de antes: no abre una ventana negra, no pide
; administrador (se instala por usuario en %LOCALAPPDATA%\Programs\Virtual Pet), no descarga nada, no
; toca PowerShell ni Defender y no escribe fuera de su carpeta, los accesos directos y la clave de
; desinstalación que Inno crea en HKCU.
;
; Se compila con packaging\windows\empaquetar.py, que es quien pasa la versión:
;
;     ISCC.exe /DVersion=<version> /O<dist> packaging\windows\VirtualPet.iss
;
; La versión NO se escribe aquí: viene de `packageVersion` en desktopApp/build.gradle.kts, que es la
; única fuente. Si falta, la compilación se para con un error claro.

#ifndef Version
  #error Falta /DVersion=<version>. Lo pasa packaging\windows\empaquetar.py.
#endif

; Rutas deducidas de la situación de este fichero (packaging\windows\ -> raíz del proyecto).
#define Raiz SourcePath + "..\..\"
#define DirApp Raiz + "desktopApp\build\compose\binaries\main\app\Virtual Pet"
#define IconoApp Raiz + "desktopApp\icons\app.ico"

[Setup]
; AppId fijo y escrito aquí, no generado: es lo que hace que una versión nueva se reconozca como
; actualización de la misma app y no como otra distinta.
AppId={{D6C352D6-FAD5-4D6F-B64C-54A5344E2639}
AppName=Virtual Pet
AppVersion={#Version}
AppVerName=Virtual Pet {#Version}
AppPublisher=Virtual Pet
; Carpeta fija: no se pregunta dónde instalar, para que no acabe en otro sitio (ni en una carpeta de
; red ni en Program Files, que pediría administrador). El desinstalador solo borra ESA carpeta, y
; además comprueba que se llama "Virtual Pet" antes de tocarla (ver [Code]).
DefaultDirName={localappdata}\Programs\Virtual Pet
DisableDirPage=yes
DefaultGroupName=Virtual Pet
DisableProgramGroupPage=yes
AllowNoIcons=yes

; Instalación por usuario: ni UAC ni permisos de administrador en ningún momento.
PrivilegesRequired=lowest

OutputDir={#Raiz}dist
OutputBaseFilename=VirtualPet-{#Version}-setup
SetupIconFile={#IconoApp}
WizardStyle=modern
Compression=lzma2/max
SolidCompression=yes
ArchitecturesAllowed=x64compatible

; Cierra la app si está abierta (Restart Manager) y no la vuelve a abrir sola al terminar: de eso se
; encarga la casilla final "Abrir Virtual Pet".
CloseApplications=yes
RestartApplications=no

; Propiedades del setup.exe que se ven en Propiedades -> Detalles.
VersionInfoVersion={#Version}
VersionInfoProductVersion={#Version}
VersionInfoProductName=Virtual Pet
VersionInfoCompany=Virtual Pet
VersionInfoDescription=Diario de salud y peso para tus mascotas
VersionInfoCopyright=Virtual Pet

UninstallDisplayName=Virtual Pet
UninstallDisplayIcon={app}\Virtual Pet.exe

[Languages]
Name: "espanol"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[CustomMessages]
espanol.GrupoAccesos=Accesos directos:
espanol.TareaEscritorio=Crear un acceso directo en el escritorio
espanol.AbrirApp=Abrir Virtual Pet
espanol.Desinstalar=Desinstalar Virtual Pet
espanol.ComentarioApp=Diario de salud y peso para tus mascotas
espanol.DatosSeQuedan=Tus datos (mascotas, historial, fotos e informes) NO se han borrado. Siguen en:%n%n%1%n%nSi quieres borrarlos también, elimina esa carpeta a mano. Virtual Pet no tiene ningún otro dato tuyo.
english.GrupoAccesos=Shortcuts:
english.TareaEscritorio=Create a desktop shortcut
english.AbrirApp=Open Virtual Pet
english.Desinstalar=Uninstall Virtual Pet
english.ComentarioApp=Health and weight diary for your pets
english.DatosSeQuedan=Your data (pets, history, photos and reports) has NOT been deleted. It is still in:%n%n%1%n%nIf you want to delete it too, remove that folder by hand. Virtual Pet keeps no other data of yours.

[Tasks]
Name: "escritorio"; Description: "{cm:TareaEscritorio}"; GroupDescription: "{cm:GrupoAccesos}"; Flags: checkedonce

[InstallDelete]
; Al instalar encima de una versión anterior se quitan los ficheros del programa viejo para no dejar
; restos (por ejemplo jars de versiones pasadas). Aquí NO hay datos del usuario: los datos viven en
; %USERPROFILE%\.virtualpet y este instalador no toca esa carpeta.
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Files]
Source: "{#DirApp}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Virtual Pet"; Filename: "{app}\Virtual Pet.exe"; Comment: "{cm:ComentarioApp}"
Name: "{group}\{cm:Desinstalar}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\Virtual Pet"; Filename: "{app}\Virtual Pet.exe"; Comment: "{cm:ComentarioApp}"; Tasks: escritorio

[Run]
Filename: "{app}\Virtual Pet.exe"; Description: "{cm:AbrirApp}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Al desinstalar se borran SOLO las dos carpetas que crea este instalador. Nada de borrar {app} entero:
; así el desinstalador no puede llevarse por delante ninguna otra carpeta aunque los datos del
; desinstalador estuvieran manipulados. Inno quita {app} al final, y solo si se ha quedado vacía.
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Code]
{ Cierra la aplicación si está abierta: con el proceso vivo, Windows deja bloqueados el .exe y las
  .dll del runtime y la instalación o la desinstalación fallarían a medias.
  Va por pasos, de lo más suave a lo más brusco:
    1. `taskkill` SIN /F: es lo mismo que pulsar la X de la ventana, y la app se cierra ordenadamente.
    2. se espera hasta 3 segundos, mirando cada medio segundo si el proceso sigue.
    3. solo si sigue abierta, `taskkill /F`.
  Son dos comandos nativos de Windows con la ventana oculta: no se ejecuta ningún script.
  Se usa al instalar y al desinstalar. Además sigue puesto CloseApplications=yes, que es el mecanismo
  de Inno (Restart Manager) para los ficheros en uso; pero ese solo actúa al instalar y no fuerza el
  cierre, así que el que decide es este. }
function AppSigueAbierta: Boolean;
var
  Codigo: Integer;
begin
  Exec(ExpandConstant('{cmd}'),
    '/C tasklist /FI "IMAGENAME eq Virtual Pet.exe" /NH | find /I "Virtual Pet.exe" >nul',
    '', SW_HIDE, ewWaitUntilTerminated, Codigo);
  Result := Codigo = 0;
end;

procedure CerrarAplicacion;
var
  Codigo: Integer;
  Intentos: Integer;
begin
  if not AppSigueAbierta then
    exit;

  Exec(ExpandConstant('{sys}\taskkill.exe'), '/IM "Virtual Pet.exe"', '',
    SW_HIDE, ewWaitUntilTerminated, Codigo);

  for Intentos := 1 to 6 do
  begin
    Sleep(500);
    if not AppSigueAbierta then
      exit;
  end;

  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /IM "Virtual Pet.exe"', '',
    SW_HIDE, ewWaitUntilTerminated, Codigo);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  CerrarAplicacion;
  Result := '';
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  Mensaje: String;
  Datos: String;
begin
  if CurUninstallStep = usUninstall then
    CerrarAplicacion;
  if CurUninstallStep = usPostUninstall then
  begin
    { La constante del perfil solo existe en el instalador: en el desinstalador, ExpandConstant
      lanzaba "Unknown constant userprofile". Se usa la variable de entorno, que sí está. }
    Datos := GetEnv('USERPROFILE');
    if Datos <> '' then
      Datos := Datos + '\.virtualpet';
    Log('Datos que se conservan: ' + Datos);
    Mensaje := FmtMessage(ExpandConstant('{cm:DatosSeQuedan}'), [Datos]);
    Log(Mensaje);

    { SuppressibleMsgBox y no MsgBox: en una desinstalación silenciosa (/VERYSILENT) no puede
      quedarse esperando a que alguien pulse Aceptar. }
    SuppressibleMsgBox(Mensaje, mbInformation, MB_OK, IDOK);
  end;
end;
