#!/bin/bash

# Script para listar todas las claves HMAC de la cuenta de servicio

PROJECT_ID="livemenu-sec"
SERVICE_ACCOUNT="livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com"

echo "=========================================="
echo "Listando Claves HMAC de la Cuenta de Servicio"
echo "=========================================="
echo ""

echo "Cuenta de servicio: $SERVICE_ACCOUNT"
echo "Proyecto: $PROJECT_ID"
echo ""

# Listar todas las claves HMAC
echo "Claves HMAC encontradas:"
echo ""

gcloud storage hmac list \
  --service-account=$SERVICE_ACCOUNT \
  --project=$PROJECT_ID \
  --format="table(accessId,state,timeCreated)"

echo ""
echo "=========================================="
echo "Si no hay claves o la que buscas no aparece:"
echo "=========================================="
echo ""
echo "1. Verifica que el Access Key sea correcto"
echo "2. O crea una nueva clave HMAC con:"
echo ""
echo "   gcloud storage hmac create $SERVICE_ACCOUNT"
echo ""
echo "   ⚠️  IMPORTANTE: Guarda el Secret Access Key que se muestra"
echo "   (solo se muestra una vez al crear la clave)"

