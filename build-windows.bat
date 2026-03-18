@echo off
REM ================================================================
REM FacturApp · Build script para Windows (.exe)
REM Genera un instalador EXE listo para distribuir en Windows
REM Requisitos previos:
REM   - JDK 17+ con jpackage (incluido en JDK 14+)
REM   - WiX Toolset 3.x instalado (para --type exe)
REM     Descarga: https://wixtoolset.org/releases/
REM   - Maven 3.x en PATH (o ajusta MAVEN_CMD abajo)
REM Ejecutar desde la raíz del proyecto: build-windows.bat
REM ================================================================

setlocal enabledelayedexpansion

REM ── Configuración ─────────────────────────────────────────────
set APP_NAME=FacturApp
set APP_VERSION=1.0.0
set MAIN_CLASS=com.facturapp.FacturApp
set MAIN_JAR=facturapp-1.0.0.jar
set JFX_VER=21.0.2
set JFX_PLATFORM=win

REM Ajusta estas rutas si es necesario
set JAVA_HOME=C:\Program Files\Java\jdk-21
set MAVEN_CMD=mvn
set M2=%USERPROFILE%\.m2\repository\org\openjfx

REM ── Validaciones ──────────────────────────────────────────────
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   FacturApp · Build Windows EXE
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo ERROR: jpackage no encontrado en %JAVA_HOME%\bin\
    echo Instala JDK 17+ y ajusta JAVA_HOME en este script.
    exit /b 1
)

REM ── Paso 1: Compilar ──────────────────────────────────────────
echo.
echo [1/3] Compilando y generando JAR...
call %MAVEN_CMD% package -q
if errorlevel 1 ( echo ERROR en mvn package && exit /b 1 )
echo       OK JAR generado: target\%MAIN_JAR%

REM ── Paso 2: Preparar distribución ─────────────────────────────
echo.
echo [2/3] Preparando archivos de distribucion...

set DIST=target\dist-win
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%"

copy "target\%MAIN_JAR%" "%DIST%\" > nul

REM Copiar JavaFX JARs para Windows (contienen .dll nativas)
for %%M in (javafx-base javafx-graphics javafx-controls javafx-fxml) do (
    set JAR=%M2%\%%M\%JFX_VER%\%%M-%JFX_VER%-%JFX_PLATFORM%.jar
    if exist "!JAR!" (
        copy "!JAR!" "%DIST%\" > nul
        echo       + %%M-%JFX_VER%-%JFX_PLATFORM%.jar
    ) else (
        echo ERROR: No se encontro !JAR!
        echo Ejecuta 'mvn compile' primero para descargar dependencias JavaFX.
        exit /b 1
    )
)
echo       OK Archivos listos en %DIST%

REM ── Paso 3: Crear EXE con jpackage ────────────────────────────
echo.
echo [3/3] Creando instalador EXE con jpackage...

set INSTALLER_DIR=target\installer
if exist "%INSTALLER_DIR%" rmdir /s /q "%INSTALLER_DIR%"
mkdir "%INSTALLER_DIR%"

"%JAVA_HOME%\bin\jpackage" ^
    --type exe ^
    --input "%DIST%" ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS%" ^
    --java-options "--module-path $APPDIR" ^
    --java-options "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base" ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --vendor "FacturApp" ^
    --description "Sistema de Facturacion Desktop" ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "FacturApp" ^
    --dest "%INSTALLER_DIR%"

if errorlevel 1 ( echo ERROR generando instalador EXE && exit /b 1 )

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   OK Instalador generado en %INSTALLER_DIR%\
dir "%INSTALLER_DIR%\*.exe"
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
