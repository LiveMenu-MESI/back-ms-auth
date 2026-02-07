#!/bin/sh
printf '"keycloak" "%s"\n' "${KC_DB_PASSWORD}" > /etc/pgbouncer/userlist.txt
printf '"livemenu" "%s"\n' "${DB_PASSWORD}" >> /etc/pgbouncer/userlist.txt
printf '"%s" "%s"\n' "${POSTGRES_USER}" "${POSTGRES_PASSWORD}" >> /etc/pgbouncer/userlist.txt
CONFIG=/etc/pgbouncer/pgbouncer.ini
exec /usr/bin/pgbouncer "$CONFIG"
