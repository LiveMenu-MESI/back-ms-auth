package cloudSecurity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records each time a specific dish is viewed via the public dish endpoint.
 * Used for "popular dishes" analytics.
 */
@Entity
@Table(name = "dish_views", indexes = {
    @Index(name = "idx_dish_view_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_dish_view_dish", columnList = "dish_id"),
    @Index(name = "idx_dish_view_created", columnList = "created_at"),
    @Index(name = "idx_dish_view_restaurant_dish", columnList = "restaurant_id, dish_id")
})
public class DishView extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    public Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    public Dish dish;

    /** SHA-256 hash of IP (anonimizado). */
    @Column(name = "ip_address", length = 64)
    public String ipAddress;

    @Column(name = "user_agent", length = 500)
    public String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    public DishView() {
    }

    public DishView(Restaurant restaurant, Dish dish, String ipAddress, String userAgent) {
        this.restaurant = restaurant;
        this.dish = dish;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
