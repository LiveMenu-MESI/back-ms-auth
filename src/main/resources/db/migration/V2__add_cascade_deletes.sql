-- V2__add_cascade_deletes.sql
-- Re-add foreign keys with ON DELETE CASCADE so that deleting a restaurant
-- automatically removes its categories, dishes, and analytics rows.

ALTER TABLE categories
    DROP CONSTRAINT fk_category_restaurant,
    ADD CONSTRAINT fk_category_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;

ALTER TABLE dishes
    DROP CONSTRAINT fk_dish_restaurant,
    ADD CONSTRAINT fk_dish_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    DROP CONSTRAINT fk_dish_category,
    ADD CONSTRAINT fk_dish_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;

ALTER TABLE dish_views
    DROP CONSTRAINT fk_dish_view_restaurant,
    ADD CONSTRAINT fk_dish_view_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    DROP CONSTRAINT fk_dish_view_dish,
    ADD CONSTRAINT fk_dish_view_dish
        FOREIGN KEY (dish_id) REFERENCES dishes(id) ON DELETE CASCADE;

ALTER TABLE menu_views
    DROP CONSTRAINT fk_menu_view_restaurant,
    ADD CONSTRAINT fk_menu_view_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;
