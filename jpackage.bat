@echo off
REM =====================================================
REM  FacturApp · Script de empaquetado Windows
REM  Genera instalador .msi / .exe para Windows
REM  Requisitos: JDK 21+, Maven, WiX Toolset (para MSI)
REM =====================================================

echo ============================================
echo  FacturApp - Generando instalador Windows
echo ============================================

REM Verificar que existe Java 21+
java -version 2>&1 | findstr "21" >NUL
IF ERRORLEVEL 1 (
    echo ERROR: Se requiere Java 21 o superior.
    echo Descarga JDK en: https://adoptium.net
    exit /b 1
)

REM 1. Compilar y empaquetar con Maven
echo.
echo [1/3] Compilando proyecto con Maven...
call mvn clean package -DskipTests
IF ERRORLEVEL 1 (
    echo ERROR: Falló la compilación Maven.
    exit /b 1
)

REM 2. Crear runtime personalizado con jlink
echo.
echo [2/3] Creando runtime Java personalizado...
IF EXIST "target\java-runtime" RMDIR /S /Q "target\java-runtime"

jlink --no-header-files --no-man-pages --compress=2 --strip-debug ^
  --add-modules java.base,java.sql,java.desktop,java.logging,java.naming,^
javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
  --output target\java-runtime

IF ERRORLEVEL 1 (
    echo ERROR: Falló la creación del runtime. Asegúrate de tener JavaFX SDK en el PATH.
    exit /b 1
)

REM 3. Generar instalador con jpackage
echo.
echo [3/3] Generando instalador con jpackage...
IF EXIST "target\installer" RMDIR /S /Q "target\installer"

jpackage ^
  --type msi ^
  --name "FacturApp" ^
  --app-version "1.0.0" ^
  --vendor "FacturApp" ^
  --description "Sistema de Facturacion Profesional" ^
  --input "target" ^
  --main-jar "facturapp-1.0.0.jar" ^
  --main-class "com.facturapp.FacturApp" ^
  --runtime-image "target\java-runtime" ^
  --dest "target\installer" ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-menu-group "FacturApp" ^
  --icon "src\main\resources\com\facturapp\icon.ico"

IF ERRORLEVEL 1 (
    echo AVISO: jpackage falló. Intentando sin icono...
    jpackage ^
      --type msi ^
      --name "FacturApp" ^
      --app-version "1.0.0" ^
      --vendor "FacturApp" ^
      --input "target" ^
      --main-jar "facturapp-1.0.0.jar" ^
      --main-class "com.facturapp.FacturApp" ^
      --runtime-image "target\java-runtime" ^
      --dest "target\installer" ^
      --win-menu --win-shortcut --win-dir-chooser
)

echo.
echo ============================================
echo  Instalador generado en: target\installer\
echo ============================================
pause
