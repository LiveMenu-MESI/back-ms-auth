    #!/bin/bash

    # Script de verificación rápida de credenciales HMAC para GCP Storage
    # Ejecuta este script en Google Cloud Shell

    PROJECT_ID="livemenu-sec"
    SERVICE_ACCOUNT="livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com"
    BUCKET_NAME="livemenu-images"
    ACCESS_KEY="GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS"

    echo "=========================================="
    echo "Verificación de Credenciales HMAC GCP"
    echo "=========================================="
    echo ""

    # 1. Verificar proyecto
    echo "1. Verificando proyecto..."
    gcloud config set project $PROJECT_ID --quiet
    CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
    if [ "$CURRENT_PROJECT" == "$PROJECT_ID" ]; then
        echo "   ✓ Proyecto configurado: $CURRENT_PROJECT"
    else
        echo "   ✗ Error: Proyecto no configurado correctamente"
        exit 1
    fi

    echo ""

    # 2. Verificar bucket
    echo "2. Verificando bucket..."
    if gsutil ls gs://$BUCKET_NAME &>/dev/null; then
        echo "   ✓ Bucket existe: gs://$BUCKET_NAME"
        BUCKET_EXISTS=true
    else
        echo "   ✗ Bucket NO existe: gs://$BUCKET_NAME"
        echo "   → Crea el bucket con: gsutil mb gs://$BUCKET_NAME"
        BUCKET_EXISTS=false
    fi

    echo ""

    # 3. Verificar cuenta de servicio
    echo "3. Verificando cuenta de servicio..."
    if gcloud iam service-accounts describe $SERVICE_ACCOUNT &>/dev/null; then
        echo "   ✓ Cuenta de servicio existe: $SERVICE_ACCOUNT"
        SA_EXISTS=true
    else
        echo "   ✗ Cuenta de servicio NO existe: $SERVICE_ACCOUNT"
        SA_EXISTS=false
    fi

    echo ""

# 4. Verificar clave HMAC
echo "4. Verificando clave HMAC..."
HMAC_STATE=$(gcloud storage hmac describe $ACCESS_KEY \
  --service-account=$SERVICE_ACCOUNT \
  --project=$PROJECT_ID \
  --format="value(state)" 2>/dev/null)

    if [ $? -eq 0 ] && [ -n "$HMAC_STATE" ]; then
        echo "   ✓ Clave HMAC encontrada"
        echo "   Estado: $HMAC_STATE"
        if [ "$HMAC_STATE" == "ACTIVE" ]; then
            HMAC_ACTIVE=true
        else
            echo "   ⚠️  ADVERTENCIA: La clave NO está ACTIVE"
            echo "   → Activa con: gcloud storage hmac update $ACCESS_KEY --service-account=$SERVICE_ACCOUNT --state=ACTIVE"
            HMAC_ACTIVE=false
        fi
    else
        echo "   ✗ Clave HMAC NO encontrada o no accesible"
        echo "   → Verifica que el Access Key sea correcto"
        HMAC_ACTIVE=false
    fi

    echo ""

    # 5. Verificar permisos
    echo "5. Verificando permisos de la cuenta de servicio..."
    ROLES=$(gcloud projects get-iam-policy $PROJECT_ID \
    --flatten="bindings[].members" \
    --filter="bindings.members:$SERVICE_ACCOUNT" \
    --format="value(bindings.role)" 2>/dev/null)

    HAS_STORAGE_ROLE=false
    if echo "$ROLES" | grep -q "storage"; then
        echo "   ✓ La cuenta tiene roles de Storage:"
        echo "$ROLES" | grep "storage" | sed 's/^/      - /'
        HAS_STORAGE_ROLE=true
    else
        echo "   ✗ La cuenta NO tiene roles de Storage asignados"
        if [ -n "$ROLES" ]; then
            echo "   Roles actuales:"
            echo "$ROLES" | sed 's/^/      - /'
        else
            echo "   No hay roles asignados"
        fi
        echo ""
        echo "   → Asigna rol con:"
        echo "     gcloud projects add-iam-policy-binding $PROJECT_ID \\"
        echo "       --member=\"serviceAccount:$SERVICE_ACCOUNT\" \\"
        echo "       --role=\"roles/storage.admin\""
    fi

    echo ""

    # 6. Resumen
    echo "=========================================="
    echo "RESUMEN"
    echo "=========================================="
    echo ""

    ALL_OK=true

    if [ "$BUCKET_EXISTS" != "true" ]; then
        echo "✗ Bucket no existe"
        ALL_OK=false
    fi

    if [ "$SA_EXISTS" != "true" ]; then
        echo "✗ Cuenta de servicio no existe"
        ALL_OK=false
    fi

    if [ "$HMAC_ACTIVE" != "true" ]; then
        echo "✗ Clave HMAC no está activa o no existe"
        ALL_OK=false
    fi

    if [ "$HAS_STORAGE_ROLE" != "true" ]; then
        echo "✗ Falta rol de Storage"
        ALL_OK=false
    fi

    if [ "$ALL_OK" == "true" ]; then
        echo "✓ Todas las verificaciones pasaron"
        echo ""
        echo "Si aún tienes errores de SignatureDoesNotMatch:"
        echo "1. Verifica que GCP_STORAGE_HMAC_SECRET_KEY sea el valor RAW (no Base64)"
        echo "2. Verifica que Access Key y Secret Key sean del mismo par de claves"
        echo "3. Revisa los logs de la aplicación para comparar StringToSign"
    else
        echo ""
        echo "Corrige los problemas marcados con ✗ antes de continuar"
    fi

    echo ""

