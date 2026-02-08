package cloudSecurity.service.menu;

import cloudSecurity.dto.AnalyticsDTO;
import cloudSecurity.entity.MenuView;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.entity.Dish;
import cloudSecurity.entity.Category;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for analytics (optional feature).
 * Provides statistics about menu views and popular dishes.
 */
@ApplicationScoped
public class AnalyticsService {

    @Inject
    EntityManager entityManager;

    /**
     * Records a menu view for analytics.
     */
    @Transactional
    public void recordMenuView(UUID restaurantId, String ipAddress, String userAgent) {
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant == null) {
            return; // Silently fail if restaurant doesn't exist
        }

        MenuView view = new MenuView(restaurant, ipAddress, userAgent);
        view.persist();
    }

    /**
     * Gets analytics dashboard for a restaurant.
     */
    public AnalyticsDTO.AnalyticsDashboardResponse getDashboard(UUID restaurantId) {
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant == null) {
            throw new jakarta.ws.rs.NotFoundException("Restaurant not found");
        }

        // Total views
        long totalViews = MenuView.find("restaurant.id", restaurantId).count();

        // Views in last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long viewsLast7Days = MenuView.find(
                "restaurant.id = ?1 and createdAt >= ?2",
                restaurantId,
                sevenDaysAgo
        ).count();

        // Views in last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long viewsLast30Days = MenuView.find(
                "restaurant.id = ?1 and createdAt >= ?2",
                restaurantId,
                thirtyDaysAgo
        ).count();

        // Unique visitors (by IP) in last 30 days
        long uniqueVisitors = entityManager.createQuery(
                "SELECT COUNT(DISTINCT mv.ipAddress) FROM MenuView mv WHERE mv.restaurant.id = :restaurantId AND mv.createdAt >= :since",
                Long.class
        )
        .setParameter("restaurantId", restaurantId)
        .setParameter("since", thirtyDaysAgo)
        .getSingleResult();

        // Dish statistics
        long totalDishes = Dish.find("restaurant.id = ?1 and deletedAt is null", restaurantId).count();
        long availableDishes = Dish.find("restaurant.id = ?1 and available = true and deletedAt is null", restaurantId).count();
        long totalCategories = Category.find("restaurant.id = ?1 and active = true", restaurantId).count();

        AnalyticsDTO.AnalyticsSummary summary = new AnalyticsDTO.AnalyticsSummary(
                totalViews,
                viewsLast7Days,
                viewsLast30Days,
                uniqueVisitors,
                (int) totalDishes,
                (int) availableDishes,
                (int) totalCategories
        );

        // Daily views for last 7 days
        List<AnalyticsDTO.DailyView> dailyViews = getDailyViews(restaurantId, 7);

        // Popular dishes (placeholder - would need dish view tracking for real data)
        List<AnalyticsDTO.PopularDish> popularDishes = getPopularDishes(restaurantId);

        return new AnalyticsDTO.AnalyticsDashboardResponse(
                restaurantId,
                restaurant.name,
                summary,
                dailyViews,
                popularDishes,
                LocalDateTime.now()
        );
    }

    /**
     * Gets daily view statistics.
     */
    private List<AnalyticsDTO.DailyView> getDailyViews(UUID restaurantId, int days) {
        List<AnalyticsDTO.DailyView> dailyViews = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            long views = MenuView.find(
                    "restaurant.id = ?1 and createdAt >= ?2 and createdAt <= ?3",
                    restaurantId,
                    startOfDay,
                    endOfDay
            ).count();

            long uniqueVisitors = entityManager.createQuery(
                    "SELECT COUNT(DISTINCT mv.ipAddress) FROM MenuView mv WHERE mv.restaurant.id = :restaurantId AND mv.createdAt >= :start AND mv.createdAt <= :end",
                    Long.class
            )
            .setParameter("restaurantId", restaurantId)
            .setParameter("start", startOfDay)
            .setParameter("end", endOfDay)
            .getSingleResult();

            dailyViews.add(new AnalyticsDTO.DailyView(
                    date.toString(),
                    views,
                    uniqueVisitors
            ));
        }

        return dailyViews;
    }

    /**
     * Gets popular dishes.
     * Note: This is a placeholder. For real popularity, we'd need to track dish views.
     * For now, returns featured dishes or most recently viewed.
     */
    private List<AnalyticsDTO.PopularDish> getPopularDishes(UUID restaurantId) {
        // Get featured dishes as "popular" (placeholder)
        List<Dish> featuredDishes = Dish.find(
                "restaurant.id = ?1 and featured = true and available = true and deletedAt is null order by updatedAt desc",
                restaurantId
        ).page(0, 10).list();

        return featuredDishes.stream()
                .map(dish -> new AnalyticsDTO.PopularDish(
                        dish.id,
                        dish.name,
                        0L, // Placeholder - would need dish view tracking
                        dish.available
                ))
                .collect(Collectors.toList());
    }

    /**
     * Exports analytics data to CSV format.
     */
    public String exportToCSV(UUID restaurantId, LocalDateTime startDate, LocalDateTime endDate) {
        List<MenuView> views = MenuView.find(
                "restaurant.id = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt",
                restaurantId,
                startDate != null ? startDate : LocalDateTime.now().minusDays(30),
                endDate != null ? endDate : LocalDateTime.now()
        ).list();

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Time,IP Address,User Agent\n");

        for (MenuView view : views) {
            csv.append(String.format("%s,%s,%s,%s\n",
                    view.createdAt.toLocalDate(),
                    view.createdAt.toLocalTime(),
                    view.ipAddress != null ? view.ipAddress : "",
                    view.userAgent != null ? view.userAgent.replace(",", ";") : ""
            ));
        }

        return csv.toString();
    }
}

