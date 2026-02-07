package cloudSecurity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

/**
 * Entity representing a restaurant in the system.
 * Each restaurant belongs to a user (identified by userEmail from Keycloak).
 */
@Entity
@Table(name = "restaurants", indexes = {
    @Index(name = "idx_restaurant_slug", columnList = "slug", unique = true),
    @Index(name = "idx_restaurant_user_email", columnList = "user_email")
})
public class Restaurant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @Column(name = "user_email", nullable = false, length = 255)
    public String userEmail;

    @Column(name = "name", nullable = false, length = 100)
    public String name;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    public String slug;

    @Column(name = "description", length = 500)
    public String description;

    @Column(name = "logo", length = 500)
    public String logo;

    @Column(name = "phone", length = 50)
    public String phone;

    @Column(name = "address", length = 255)
    public String address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schedule", columnDefinition = "jsonb")
    public Map<String, Object> schedule;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // Default constructor for JPA
    public Restaurant() {
    }

    // Constructor for creating new restaurants
    public Restaurant(String userEmail, String name) {
        this.userEmail = userEmail;
        this.name = name;
    }
}

