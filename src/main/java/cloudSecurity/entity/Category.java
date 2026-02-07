package cloudSecurity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a category for organizing menu items.
 * Each category belongs to a restaurant.
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_category_position", columnList = "restaurant_id, position")
})
public class Category extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    public Restaurant restaurant;

    @Column(name = "name", nullable = false, length = 50)
    public String name;

    @Column(name = "description", columnDefinition = "text")
    public String description;

    @Column(name = "position", nullable = false)
    public Integer position;

    @Column(name = "active", nullable = false)
    public Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // Default constructor for JPA
    public Category() {
    }

    // Constructor for creating new categories
    public Category(Restaurant restaurant, String name, Integer position) {
        this.restaurant = restaurant;
        this.name = name;
        this.position = position;
        this.active = true;
    }
}

