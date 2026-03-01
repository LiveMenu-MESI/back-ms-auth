# Diagramas de Arquitectura — Backend LiveMenu

Este documento contiene diagramas de la **arquitectura del backend** LiveMenu (API Quarkus) en formato Mermaid: componentes, flujos, modelo de datos, despliegue con Docker Compose, procesamiento de imágenes y seguridad.

---

## Diagrama de Componentes (Backend)

```mermaid
graph TB
    subgraph "Clientes de la API"
        A[Clientes Admin<br/>Portal / Apps]
        B[Clientes Públicos<br/>Menú / QR]
    end

    subgraph "Capa de Aplicación — Quarkus Backend"
        C[REST Resources]
        D[Security Filters<br/>Token Validation]
        E[Services Layer]
        F[Data Layer<br/>Hibernate ORM / Panache]
    end

    subgraph "Infraestructura"
        G[(PostgreSQL<br/>livemenu DB)]
        H[(PostgreSQL<br/>keycloak DB)]
        I[Keycloak<br/>OAuth2 / OIDC]
        J[Google Cloud Storage<br/>Imágenes]
    end

    A -->|HTTPS / REST| C
    B -->|HTTPS / REST| C
    C --> D
    D --> E
    E --> F
    F --> G
    E --> I
    E --> J
    I --> H

    style A fill:#607D8B
    style B fill:#607D8B
    style C fill:#2196F3
    style D fill:#2196F3
    style E fill:#2196F3
    style F fill:#2196F3
    style G fill:#FF9800
    style H fill:#FF9800
    style I fill:#9C27B0
    style J fill:#F44336
```

---

## Diagrama de Flujo de Autenticación

```mermaid
sequenceDiagram
    participant U as Usuario
    participant C as Cliente
    participant B as Backend
    participant K as Keycloak

    U->>C: 1. Registro (email, password)
    C->>B: POST /api/v1/auth/register
    B->>K: Crear usuario (Admin API)
    K-->>B: Usuario creado
    B-->>C: 201 Created
    C-->>U: Registro exitoso

    U->>C: 2. Login (email, password)
    C->>B: POST /api/v1/auth/login
    B->>K: Token request (password grant)
    K-->>B: access_token + refresh_token
    B-->>C: Tokens (+ cookies si aplica)
    C-->>U: Login exitoso

    U->>C: 3. Request protegido
    C->>B: GET /api/v1/admin/restaurants<br/>Authorization: Bearer token
    B->>K: Introspect / validar token
    K-->>B: Token válido
    B-->>C: Datos del restaurante
    C-->>U: Mostrar datos
```

---

## Diagrama de Flujo de Gestión de Menú

```mermaid
sequenceDiagram
    participant A as Admin
    participant C as Cliente
    participant B as Backend
    participant DB as PostgreSQL
    participant GCS as Google Cloud Storage

    A->>C: 1. Crear restaurante
    C->>B: POST /api/v1/admin/restaurants
    B->>DB: INSERT restaurant
    DB-->>B: Restaurant creado
    B-->>C: 201 Created

    A->>C: 2. Crear categoría
    C->>B: POST .../restaurants/{id}/categories
    B->>DB: INSERT category
    DB-->>B: Category creada
    B-->>C: 201 Created

    A->>C: 3. Crear plato con imagen
    C->>B: POST .../dishes/with-image<br/>(multipart/form-data)
    B->>B: Procesar imagen<br/>(resize, compress)
    B->>GCS: Upload variantes<br/>(thumbnail, medium, large)
    GCS-->>B: URLs públicas
    B->>DB: INSERT dish con URLs
    DB-->>B: Dish creado
    B-->>C: 201 Created con URLs
    C-->>A: Plato creado
```

---

## Diagrama de Flujo de Vista Pública

```mermaid
sequenceDiagram
    participant U as Usuario
    participant C as Cliente
    participant B as Backend
    participant Cache as Caché
    participant DB as PostgreSQL

    U->>C: 1. Abrir menú (p. ej. por QR)
    C->>B: GET /api/v1/public/menu/{slug}

    B->>Cache: Verificar caché
    alt Caché disponible
        Cache-->>B: Menú cacheado
    else Caché no disponible
        B->>DB: Query restaurant + categories + dishes
        DB-->>B: Datos del menú
        B->>Cache: Guardar en caché
    end

    B->>DB: Registrar vista (analytics)
    B-->>C: JSON con menú completo
    C-->>U: Renderizar menú
```

---

## Diagrama de Arquitectura de Datos

Las tablas y campos coinciden con las entidades del backend (ver [HIBERNATE.md](HIBERNATE.md)).

```mermaid
erDiagram
    RESTAURANT ||--o{ CATEGORY : has
    CATEGORY ||--o{ DISH : contains
    RESTAURANT ||--o{ MENU_VIEW : tracks
    DISH ||--o{ DISH_VIEW : tracks

    RESTAURANT {
        uuid id PK
        string user_email
        string name
        string slug UK
        string description
        string address
        string phone
        string logo
        jsonb schedule
        timestamp created_at
        timestamp updated_at
    }

    CATEGORY {
        uuid id PK
        uuid restaurant_id FK
        string name
        text description
        int position
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    DISH {
        uuid id PK
        uuid restaurant_id FK
        uuid category_id FK
        string name
        string description
        decimal price
        decimal offer_price
        string image_url
        boolean available
        boolean featured
        jsonb tags
        int position
        timestamp deleted_at
        timestamp created_at
        timestamp updated_at
    }

    MENU_VIEW {
        uuid id PK
        uuid restaurant_id FK
        string ip_address
        string user_agent
        timestamp created_at
    }

    DISH_VIEW {
        uuid id PK
        uuid restaurant_id FK
        uuid dish_id FK
        string ip_address
        string user_agent
        timestamp created_at
    }
```

---

## Diagrama de Deployment (Docker Compose — Backend)

Solo servicios del backend y su infraestructura (sin frontend).

```mermaid
graph TB
    subgraph "Docker Compose — Backend e infraestructura"
        subgraph "Contenedor Backend"
            A[Quarkus API<br/>Puertos 8080 / 8444]
        end

        subgraph "Contenedor Keycloak"
            B[Keycloak<br/>8180 / 8443]
        end

        subgraph "Contenedor PostgreSQL"
            C[(PostgreSQL<br/>5432)]
            D[(livemenu DB)]
            E[(keycloak DB)]
        end

        F[Volume:<br/>postgres_data]
    end

    G[Google Cloud Storage]
    H[Clientes HTTP/HTTPS]

    H -->|API| A
    A --> B
    A --> C
    B --> C
    C --> D
    C --> E
    C --> F
    A -->|Imágenes| G

    style A fill:#2196F3
    style B fill:#9C27B0
    style C fill:#FF9800
    style D fill:#FF9800
    style E fill:#FF9800
    style F fill:#FFC107
    style G fill:#F44336
    style H fill:#607D8B
```

---

## Diagrama de Procesamiento de Imágenes

```mermaid
flowchart TD
    A[Cliente sube imagen] --> B{Tipo MIME válido?}
    B -->|No| C[Error 400]
    B -->|Sí| D{Tamaño válido?}
    D -->|> límite| E[Error 400]
    D -->|OK| F[Leer imagen]

    F --> G[Generar UUID para nombre]

    G --> H[Variante THUMBNAIL]
    G --> I[Variante MEDIUM]
    G --> J[Variante LARGE]
    G --> K[ORIGINAL]

    H --> L[Comprimir JPEG]
    I --> M[Comprimir JPEG]
    J --> N[Comprimir JPEG]

    L --> O[Upload GCS]
    M --> P[Upload GCS]
    N --> Q[Upload GCS]
    K --> R[Upload GCS]

    O --> S[URLs públicas]
    P --> S
    Q --> S
    R --> S

    S --> T[Respuesta al cliente]

    style A fill:#4CAF50
    style C fill:#F44336
    style E fill:#F44336
    style T fill:#2196F3
```

---

## Diagrama de Seguridad (Token y Filtros)

```mermaid
flowchart TD
    A[Request HTTP] --> B{Endpoint público?}

    B -->|Sí| C[Continuar]
    B -->|No| D[Token Validation Filter]

    D --> E{Token presente?}
    E -->|No| F[401 Unauthorized]
    E -->|Sí| G[Validar con Keycloak]

    G --> H{Token válido?}
    H -->|No| F
    H -->|Sí| I{Rol requerido?}

    I -->|No| J[403 Forbidden]
    I -->|Sí| C

    C --> K[Procesar request]
    K --> L[Response]

    style A fill:#607D8B
    style F fill:#F44336
    style J fill:#F44336
    style L fill:#2196F3
```

---

## Notas

### Convenciones de color
- **Gris (#607D8B):** Clientes / entrada.
- **Azul (#2196F3):** Backend (Quarkus).
- **Naranja (#FF9800):** Base de datos.
- **Morado (#9C27B0):** Keycloak / autenticación.
- **Rojo (#F44336):** GCS / errores.
- **Amarillo (#FFC107):** Volúmenes / persistencia.

### Cómo visualizar
Los diagramas están en **Mermaid** y se renderizan en:
- GitHub / GitLab (preview del `.md`).
- VS Code con extensión "Markdown Preview Mermaid Support".
- [mermaid.live](https://mermaid.live).

### Relación con el código
- Entidades y tablas: [docs/HIBERNATE.md](HIBERNATE.md).
- Despliegue y variables: [README.md](../README.md) y [compose/CONFIG-LOGIN-DOCKER.md](../compose/CONFIG-LOGIN-DOCKER.md).
