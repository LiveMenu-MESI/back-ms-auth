@echo off
REM Generates a self-signed PKCS12 keystore for Quarkus HTTPS.
REM Usage: gen-keystore.bat [output-dir]
REM Default output: compose\config\keystore.p12

set OUT_DIR=%~1
if "%OUT_DIR%"=="" set OUT_DIR=%~dp0..\config
set KEYSTORE_FILE=%OUT_DIR%\keystore.p12
set PASSWORD=%HTTPS_KEYSTORE_PASSWORD%
if "%PASSWORD%"=="" set PASSWORD=changeit
set VALIDITY_DAYS=%KEYSTORE_VALIDITY_DAYS%
if "%VALIDITY_DAYS%"=="" set VALIDITY_DAYS=365

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

echo Generating PKCS12 keystore: %KEYSTORE_FILE%
echo Password: %PASSWORD% (set HTTPS_KEYSTORE_PASSWORD to override)
echo Validity: %VALIDITY_DAYS% days

keytool -genkeypair ^
  -storetype PKCS12 ^
  -keystore "%KEYSTORE_FILE%" ^
  -storepass "%PASSWORD%" ^
  -alias server ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity %VALIDITY_DAYS% ^
  -dname "CN=localhost, OU=LiveMenu, O=LiveMenu, L=City, ST=State, C=ES"

echo.
echo Done. Use in .env:
echo   HTTPS_ENABLED=true
echo   HTTPS_KEYSTORE_FILE=/app/config/keystore.p12
echo   HTTPS_KEYSTORE_PASSWORD=%PASSWORD%
echo.
echo Mount in docker-compose: add volume
echo   - .\compose\config\keystore.p12:/app/config/keystore.p12:ro
