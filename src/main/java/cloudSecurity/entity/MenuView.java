package cloudSecurity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking menu views (analytics).
 * Records each time a public menu is viewed.
 */
@Entity
@Table(name = "menu_views", indexes = {
    @Index(name = "idx_menu_view_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_menu_view_created", columnList = "created_at"),
    @Index(name = "idx_menu_view_restaurant_created", columnList = "restaurant_id, created_at")
})
public class MenuView extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    public Restaurant restaurant;

    @Column(name = "ip_address", length = 45)
    public String ipAddress;

    @Column(name = "user_agent", length = 500)
    public String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    // Default constructor for JPA
    public MenuView() {
    }

    // Constructor for creating new views
    public MenuView(Restaurant restaurant, String ipAddress, String userAgent) {
        this.restaurant = restaurant;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}

