package cloudSecurity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a dish/product in the menu.
 * Each dish belongs to a category and a restaurant.
 */
@Entity
@Table(name = "dishes", indexes = {
    @Index(name = "idx_dish_category", columnList = "category_id"),
    @Index(name = "idx_dish_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_dish_active", columnList = "restaurant_id, available")
})
@SQLDelete(sql = "UPDATE dishes SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Dish extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    public Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    public Category category;

    @Column(name = "name", nullable = false, length = 100)
    public String name;

    @Column(name = "description", length = 300)
    public String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    public BigDecimal price;

    @Column(name = "offer_price", precision = 10, scale = 2)
    public BigDecimal offerPrice;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    @Column(name = "available", nullable = false)
    public Boolean available = true;

    @Column(name = "featured", nullable = false)
    public Boolean featured = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    public List<String> tags;

    @Column(name = "position", nullable = false)
    public Integer position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;

    // Default constructor for JPA
    public Dish() {
    }

    // Constructor for creating new dishes
    public Dish(Restaurant restaurant, Category category, String name, BigDecimal price, Integer position) {
        this.restaurant = restaurant;
        this.category = category;
        this.name = name;
        this.price = price;
        this.position = position;
        this.available = true;
        this.featured = false;
    }
}

