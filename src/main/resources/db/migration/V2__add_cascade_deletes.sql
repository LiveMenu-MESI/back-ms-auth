-- V2__add_cascade_deletes.sql
-- Re-add foreign keys with ON DELETE CASCADE so that deleting a restaurant
-- automatically removes its categories, dishes, and analytics rows.

DO $$
DECLARE r RECORD;
BEGIN
    -- Drop ALL existing FK constraints on child tables (whatever names Hibernate/Flyway gave them)
    FOR r IN
        SELECT conname, conrelid::regclass::text AS tbl
        FROM   pg_constraint
        WHERE  contype = 'f'
          AND  conrelid IN (
                  'categories'::regclass,
                  'dishes'::regclass,
                  'dish_views'::regclass,
                  'menu_views'::regclass
               )
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tbl, r.conname);
    END LOOP;
END;
$$;

-- Re-add with ON DELETE CASCADE
ALTER TABLE categories
    ADD CONSTRAINT fk_category_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;

ALTER TABLE dishes
    ADD CONSTRAINT fk_dish_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dish_category
        FOREIGN KEY (category_id)   REFERENCES categories(id)  ON DELETE CASCADE;

ALTER TABLE dish_views
    ADD CONSTRAINT fk_dish_view_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dish_view_dish
        FOREIGN KEY (dish_id)       REFERENCES dishes(id)      ON DELETE CASCADE;

ALTER TABLE menu_views
    ADD CONSTRAINT fk_menu_view_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;
