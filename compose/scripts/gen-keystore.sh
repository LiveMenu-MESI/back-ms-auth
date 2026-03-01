#!/usr/bin/env bash
# Generates a self-signed PKCS12 keystore for Quarkus HTTPS.
# Usage: ./gen-keystore.sh [output-dir]
# Default output: compose/config/keystore.p12 (or current dir if run from elsewhere)

set -e
OUT_DIR="${1:-$(dirname "$0")/../config}"
KEYSTORE_FILE="$OUT_DIR/keystore.p12"
PASSWORD="${HTTPS_KEYSTORE_PASSWORD:-changeit}"
ALIAS="server"
VALIDITY_DAYS="${KEYSTORE_VALIDITY_DAYS:-365}"

mkdir -p "$OUT_DIR"

echo "Generating PKCS12 keystore: $KEYSTORE_FILE"
echo "Password: $PASSWORD (set HTTPS_KEYSTORE_PASSWORD to override)"
echo "Validity: $VALIDITY_DAYS days"

keytool -genkeypair \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_FILE" \
  -storepass "$PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -dname "CN=localhost, OU=LiveMenu, O=LiveMenu, L=City, ST=State, C=ES"

echo "Done. Use in .env:"
echo "  HTTPS_ENABLED=true"
echo "  HTTPS_KEYSTORE_FILE=/app/config/keystore.p12"
echo "  HTTPS_KEYSTORE_PASSWORD=$PASSWORD"
echo ""
echo "Mount in docker-compose (example):"
echo "  - \$(pwd)/compose/config/keystore.p12:/app/config/keystore.p12:ro"
