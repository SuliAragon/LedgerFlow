#!/bin/bash
# =====================================================
#  FacturApp · Script de empaquetado macOS
#  Genera un bundle .app / .dmg para macOS
#  Requisitos: JDK 21+, Maven
# =====================================================

set -e

echo "============================================"
echo " FacturApp - Generando bundle macOS"
echo "============================================"

# Verificar Java 21+
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 21 ]; then
    echo "ERROR: Se requiere Java 21 o superior. Versión actual: $JAVA_VER"
    echo "Descarga JDK en: https://adoptium.net"
    exit 1
fi

echo ""
echo "[1/3] Compilando proyecto con Maven..."
mvn clean package -DskipTests

echo ""
echo "[2/3] Creando runtime Java personalizado con jlink..."
rm -rf target/java-runtime

jlink --no-header-files --no-man-pages --compress=2 --strip-debug \
  --add-modules java.base,java.sql,java.desktop,java.logging,java.naming,\
javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
  --output target/java-runtime

echo ""
echo "[3/3] Generando bundle con jpackage..."
rm -rf target/installer
mkdir -p target/installer

jpackage \
  --type dmg \
  --name "FacturApp" \
  --app-version "1.0.0" \
  --vendor "FacturApp" \
  --description "Sistema de Facturación Profesional" \
  --input "target" \
  --main-jar "facturapp-1.0.0.jar" \
  --main-class "com.facturapp.FacturApp" \
  --runtime-image "target/java-runtime" \
  --dest "target/installer" \
  --mac-package-name "FacturApp"

echo ""
echo "============================================"
echo " Bundle generado en: target/installer/"
echo "============================================"
