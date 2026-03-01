#!/usr/bin/env bash
# Generates a self-signed PKCS12 keystore for Quarkus HTTPS.
# Usage: ./gen-keystore.sh [output-dir]
# Optional: set KEYSTORE_DNS=*.naing.co (or comma-separated: *.naing.co,naing.co). No localhost unless you add it.
# Default output: compose/config/keystore.p12 (or current dir if run from elsewhere)

set -e
OUT_DIR="${1:-$(dirname "$0")/../config}"
KEYSTORE_FILE="$OUT_DIR/keystore.p12"
PASSWORD="${HTTPS_KEYSTORE_PASSWORD:-changeit}"
ALIAS="server"
VALIDITY_DAYS="${KEYSTORE_VALIDITY_DAYS:-365}"
# DNS names for SAN only (e.g. *.naing.co or *.naing.co,naing.co). No localhost unless you add it here.
KEYSTORE_DNS="${KEYSTORE_DNS:-}"

mkdir -p "$OUT_DIR"

# Build SAN from KEYSTORE_DNS only (no localhost)
SAN=""
if [ -n "$KEYSTORE_DNS" ]; then
  first=1
  for d in $(echo "$KEYSTORE_DNS" | tr ',' ' '); do
    [ -n "$d" ] || continue
    if [ "$first" -eq 1 ]; then SAN="dns:$d"; first=0; else SAN="$SAN,dns:$d"; fi
  done
fi
if [ -z "$SAN" ]; then
  echo "Error: set KEYSTORE_DNS (e.g. KEYSTORE_DNS=*.naing.co)"
  exit 1
fi

echo "Generating PKCS12 keystore: $KEYSTORE_FILE"
echo "Password: $PASSWORD (set HTTPS_KEYSTORE_PASSWORD to override)"
echo "Validity: $VALIDITY_DAYS days"
echo "SAN: $SAN"

keytool -genkeypair \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_FILE" \
  -storepass "$PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -dname "CN=naing.co, OU=LiveMenu, O=LiveMenu, L=City, ST=State, C=ES" \
  -ext "SAN=$SAN"

echo "Done. Use in .env:"
echo "  HTTPS_ENABLED=true"
echo "  HTTPS_KEYSTORE_FILE=/app/config/keystore.p12"
echo "  HTTPS_KEYSTORE_PASSWORD=$PASSWORD"
echo ""
echo "Mount in docker-compose (example):"
echo "  - \$(pwd)/compose/config/keystore.p12:/app/config/keystore.p12:ro"
