FROM quay.io/keycloak/keycloak:26 AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres
# Debe estar antes de kc.sh build — en --optimized mode se hornea en la imagen
ENV KC_PROXY_HEADERS=xforwarded

WORKDIR /opt/keycloak

RUN keytool -genkeypair \
 -storepass password \
 -storetype PKCS12 \
 -keyalg RSA \
 -keysize 2048 \
 -dname "CN=server" \
 -alias server \
 -ext "SAN:c=DNS:localhost,IP:127.0.0.1" \
 -keystore conf/server.keystore

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26
COPY --from=builder /opt/keycloak/ /opt/keycloak/
# Realm import: Keycloak will import this on first start (--import-realm)
COPY realm/livemenu-realm.json /opt/keycloak/data/import/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm"]
