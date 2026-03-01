# Hibernate ORM y persistencia (Quarkus)

Este documento describe el uso de **Hibernate ORM** con **Panache** en el backend LiveMenu: configuración del esquema, entidades y estrategia de base de datos.

---

## Configuración

### Datasource

La aplicación usa **PostgreSQL**. Las propiedades se inyectan por variables de entorno:

| Variable   | Uso                          |
|-----------|-------------------------------|
| `DB_USER` | Usuario de la base de datos   |
| `DB_PASSWORD` | Contraseña                |
| `DB_URL`  | JDBC URL (ej. `jdbc:postgresql://host:5432/livemenu`) |

En `application.properties`:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.datasource.jdbc.url=${DB_URL}
```

### Estrategia del esquema

El manejo del esquema se controla con:

```properties
quarkus.hibernate-orm.schema-management.strategy=${HIBERNATE_SCHEMA_STRATEGY:drop-and-create}
```

| Valor (variable `HIBERNATE_SCHEMA_STRATEGY`) | Comportamiento |
|---------------------------------------------|----------------|
| **drop-and-create** (por defecto) | Al arrancar, Hibernate borra y vuelve a crear tablas según las entidades. Útil en desarrollo; **no usar en producción** (pérdida de datos). |
| **create** | Crea tablas si no existen; no las borra. |
| **update** | Crea tablas nuevas y añade columnas; no elimina columnas ni tablas obsoletas. |
| **validate** | No modifica el esquema; solo comprueba que las entidades coincidan con la BD. Recomendado en producción si el esquema se gestiona con migraciones externas. |
| **none** | Hibernate no toca el esquema. |

En **producción** conviene usar `validate` o `none` y mantener el esquema con migraciones (por ejemplo Flyway o Liquibase), o al menos `update` si se aceptan sus limitaciones.

---

## Entidades (modelo de dominio)

Todas las entidades extienden `PanacheEntityBase` y usan **UUID** como identificador.

### Tablas principales

| Entidad    | Tabla        | Descripción |
|-----------|---------------|-------------|
| `Restaurant` | `restaurants` | Restaurante; asociado a un usuario por `user_email`. Índices: `slug` (único), `user_email`. |
| `Category`   | `categories` | Categoría del menú; pertenece a un `Restaurant`. Índices: `restaurant_id`, `(restaurant_id, position)`. |
| `Dish`       | `dishes`     | Plato/producto; pertenece a un `Restaurant` y a una `Category`. Soft delete con `deleted_at`. Índices: `category_id`, `restaurant_id`, `(restaurant_id, available)`. |
| `MenuView`   | `menu_views` | Registro de cada vista del menú público (analytics). Índices: `restaurant_id`, `created_at`, `(restaurant_id, created_at)`. |
| `DishView`   | `dish_views` | Registro de cada vista de un plato (analytics, platos populares). Índices: `restaurant_id`, `dish_id`, `created_at`, `(restaurant_id, dish_id)`. |

### Relaciones

- **Restaurant** → una a muchas **Category** y **Dish** (y vistas asociadas).
- **Category** → muchas **Dish**.
- **Dish**: soft delete con `@SQLDelete` que actualiza `deleted_at` en lugar de borrar la fila.
- **MenuView** y **DishView**: `@ManyToOne` a `Restaurant`; `DishView` además a `Dish`. Campos de auditoría: IP anonimizada (hash) y `user_agent`.

### Tipos especiales

- **JSON/JSONB**: en `Restaurant.schedule` (mapa) y `Dish.tags` (lista) se usa `@JdbcTypeCode(SqlTypes.JSON)` con `columnDefinition = "jsonb"` para PostgreSQL.
- **Timestamps**: `@CreationTimestamp` y `@UpdateTimestamp` de Hibernate en `created_at` y `updated_at` donde aplica.

---

## Ubicación en el código

- **Entidades:** `src/main/java/cloudSecurity/entity/`
- **Configuración Hibernate/datasource:** `src/main/resources/application.properties`
- **Servicios que usan las entidades:** `src/main/java/cloudSecurity/service/`

---

## Migraciones versionadas (Flyway / Liquibase)

El proyecto **no incluye actualmente** Flyway ni Liquibase. El esquema se obtiene de las entidades y de la estrategia Hibernate (`drop-and-create`, `update` o `validate`). Esta sección sirve como guía para cuando se quiera introducir migraciones versionadas (recomendado en producción).

### Por qué usarlas

- **Historial del esquema:** cambios de BD versionados y repetibles.
- **Producción segura:** sin `drop-and-create`; despliegues que solo aplican migraciones pendientes.
- **Rollback/documentación:** cada cambio queda registrado en el repositorio.

### Flyway (recomendado con Quarkus)

1. **Dependencia** en `pom.xml`:
   ```xml
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-flyway</artifactId>
   </dependency>
   ```

2. **Carpeta de migraciones:** `src/main/resources/db/migration/`.  
   Nombres: `V1__descripcion.sql`, `V2__otro_cambio.sql` (doble guión bajo; orden por prefijo numérico).

3. **Primera migración:** crear el esquema inicial (tablas `restaurants`, `categories`, `dishes`, `menu_views`, `dish_views`) en un único `V1__initial_schema.sql` o generar el DDL desde Hibernate una vez y pegarlo ahí.

4. **Configuración en producción:**  
   - `quarkus.flyway.migrate-at-start=true` (por defecto).  
   - `quarkus.hibernate-orm.schema-management.strategy=validate` (o `none`) para que Hibernate no modifique el esquema y Flyway sea la única fuente de cambios.

5. **Documentación:** [Quarkus - Flyway](https://quarkus.io/guides/flyway).

### Liquibase (alternativa)

1. **Dependencia:** `quarkus-liquibase`.  
2. **Ubicación:** por ejemplo `src/main/resources/db/changelog.xml` (o la ruta configurada en `quarkus.liquibase.change-log`).  
3. En producción usar igualmente `schema-management.strategy=validate` (o `none`).  
4. **Documentación:** [Quarkus - Liquibase](https://quarkus.io/guides/liquibase).

### Orden de ejecución

Con Flyway o Liquibase activos, las migraciones se ejecutan **al arrancar** la aplicación (antes de que Hibernate valide o actualice el esquema). Conviene que el esquema resultante coincida con las entidades JPA para que `validate` no falle.

### Resumen

| Paso | Acción |
|------|--------|
| 1 | Añadir `quarkus-flyway` (o `quarkus-liquibase`) al proyecto. |
| 2 | Crear scripts en `db/migration` (Flyway) o el change log (Liquibase). |
| 3 | En producción: `HIBERNATE_SCHEMA_STRATEGY=validate` y migrar con Flyway/Liquibase al inicio. |
| 4 | Nuevos cambios de esquema: añadir una nueva migración (V2, V3, …) en lugar de depender de `update`. |
