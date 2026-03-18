#!/bin/bash
# ================================================================
# FacturApp · Build script para macOS (.dmg)
# Genera un instalador DMG listo para distribuir en macOS
# Ejecutar desde la raíz del proyecto: ./build-mac.sh
# ================================================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

export JAVA_HOME="$PROJECT_DIR/oracleJdk-26.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:/tmp/apache-maven-3.9.9/bin:$PATH"

JPACKAGE="$JAVA_HOME/bin/jpackage"
APP_NAME="FacturApp"
APP_VERSION="1.0.0"
MAIN_CLASS="com.facturapp.FacturApp"
MAIN_JAR="facturapp-1.0.0.jar"
JFX_VER="21.0.2"
M2="$HOME/.m2/repository/org/openjfx"

# Detectar arquitectura macOS
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    JFX_PLATFORM="mac-aarch64"
else
    JFX_PLATFORM="mac"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  FacturApp · Build macOS DMG"
echo "  Arquitectura: $ARCH ($JFX_PLATFORM)"
echo "  Java: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ── Paso 1: Compilar y generar fat JAR (sin JavaFX) ──────────────
echo ""
echo "[1/3] Compilando y generando JAR..."
mvn package -q
echo "      ✓ JAR generado: target/$MAIN_JAR ($(du -sh target/$MAIN_JAR | cut -f1))"

# ── Paso 2: Preparar directorio de distribución ──────────────────
echo ""
echo "[2/3] Preparando archivos de distribución..."

DIST="$PROJECT_DIR/target/dist-mac"
rm -rf "$DIST"
mkdir -p "$DIST"

# App JAR (fat, sin JavaFX)
cp "target/$MAIN_JAR" "$DIST/"

# JavaFX platform JARs (contienen las librerías nativas .dylib)
for MODULE in javafx-base javafx-graphics javafx-controls javafx-fxml; do
    JAR="$M2/$MODULE/$JFX_VER/$MODULE-$JFX_VER-$JFX_PLATFORM.jar"
    if [ -f "$JAR" ]; then
        cp "$JAR" "$DIST/"
        echo "      + $MODULE-$JFX_VER-$JFX_PLATFORM.jar"
    else
        echo "ERROR: No se encontró $JAR"
        echo "Ejecuta primero 'mvn compile' para descargar las dependencias de JavaFX."
        exit 1
    fi
done

echo "      ✓ Archivos listos en $DIST/"

# ── Paso 3: Crear DMG con jpackage ───────────────────────────────
echo ""
echo "[3/3] Creando DMG con jpackage..."

INSTALLER_DIR="$PROJECT_DIR/target/installer"
rm -rf "$INSTALLER_DIR"
mkdir -p "$INSTALLER_DIR"

"$JPACKAGE" \
    --type dmg \
    --input "$DIST" \
    --main-jar "$MAIN_JAR" \
    --main-class "$MAIN_CLASS" \
    --java-options "--module-path \$APPDIR" \
    --java-options "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base" \
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" \
    --java-options "--add-opens java.base/java.io=ALL-UNNAMED" \
    --java-options "-Dfile.encoding=UTF-8" \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "FacturApp" \
    --description "Sistema de Facturación Desktop" \
    --dest "$INSTALLER_DIR"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ✓ DMG generado correctamente:"
ls -lh "$INSTALLER_DIR"/*.dmg
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
