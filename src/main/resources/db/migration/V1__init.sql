-- V1__init.sql
-- Schema inicial de LiveMenu: restaurants, categories, dishes, analytics.
-- Ejecutado por Flyway al arrancar la aplicación por primera vez.

-- Extensión para UUIDs (necesaria en PostgreSQL < 13)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── restaurants ─────────────────────────────────────────────────────────────
CREATE TABLE restaurants (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email  VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    logo        VARCHAR(500),
    phone       VARCHAR(50),
    address     VARCHAR(255),
    schedule    JSONB,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_restaurant_slug UNIQUE (slug)
);

CREATE INDEX idx_restaurant_user_email ON restaurants (user_email);

-- ─── categories ──────────────────────────────────────────────────────────────
CREATE TABLE categories (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID         NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    description   TEXT,
    position      INTEGER      NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT fk_category_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE INDEX idx_category_restaurant ON categories (restaurant_id);
CREATE INDEX idx_category_position   ON categories (restaurant_id, position);

-- ─── dishes ──────────────────────────────────────────────────────────────────
CREATE TABLE dishes (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID          NOT NULL,
    category_id   UUID          NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    description   VARCHAR(300),
    price         NUMERIC(10,2) NOT NULL,
    offer_price   NUMERIC(10,2),
    image_url     VARCHAR(500),
    available     BOOLEAN       NOT NULL DEFAULT TRUE,
    featured      BOOLEAN       NOT NULL DEFAULT FALSE,
    tags          JSONB,
    position      INTEGER       NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    deleted_at    TIMESTAMP,
    CONSTRAINT fk_dish_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_dish_category   FOREIGN KEY (category_id)   REFERENCES categories(id)
);

CREATE INDEX idx_dish_category   ON dishes (category_id);
CREATE INDEX idx_dish_restaurant ON dishes (restaurant_id);
CREATE INDEX idx_dish_active     ON dishes (restaurant_id, available);

-- ─── dish_views (analytics) ──────────────────────────────────────────────────
CREATE TABLE dish_views (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID         NOT NULL,
    dish_id       UUID         NOT NULL,
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT fk_dish_view_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_dish_view_dish       FOREIGN KEY (dish_id)       REFERENCES dishes(id)
);

CREATE INDEX idx_dish_view_restaurant      ON dish_views (restaurant_id);
CREATE INDEX idx_dish_view_dish            ON dish_views (dish_id);
CREATE INDEX idx_dish_view_created         ON dish_views (created_at);
CREATE INDEX idx_dish_view_restaurant_dish ON dish_views (restaurant_id, dish_id);

-- ─── menu_views (analytics) ──────────────────────────────────────────────────
CREATE TABLE menu_views (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID         NOT NULL,
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT fk_menu_view_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE INDEX idx_menu_view_restaurant         ON menu_views (restaurant_id);
CREATE INDEX idx_menu_view_created            ON menu_views (created_at);
CREATE INDEX idx_menu_view_restaurant_created ON menu_views (restaurant_id, created_at);
