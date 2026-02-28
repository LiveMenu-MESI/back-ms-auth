package cloudSecurity.service.menu;

import cloudSecurity.dto.AnalyticsDTO;
import cloudSecurity.entity.MenuView;
import cloudSecurity.entity.DishView;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.entity.Dish;
import cloudSecurity.entity.Category;
import cloudSecurity.util.IpAnonymization;

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

    @Inject
    IpAnonymization ipAnonymization;

    /**
     * Records a menu view for analytics.
     * IP is stored as a one-way hash (anonimizado) for privacy.
     */
    @Transactional
    public void recordMenuView(UUID restaurantId, String ipAddress, String userAgent) {
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant == null) {
            return; // Silently fail if restaurant doesn't exist
        }

        String ipHash = ipAnonymization.hash(ipAddress);
        MenuView view = new MenuView(restaurant, ipHash, userAgent);
        view.persist();
    }

    /**
     * Records a dish view for "popular dishes" analytics.
     * Called when the public dish endpoint is hit.
     */
    @Transactional
    public void recordDishView(UUID restaurantId, UUID dishId, String ipAddress, String userAgent) {
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant == null) {
            return;
        }
        Dish dish = Dish.findById(dishId);
        if (dish == null || !dish.restaurant.id.equals(restaurantId)) {
            return;
        }
        String ipHash = ipAnonymization.hash(ipAddress);
        DishView view = new DishView(restaurant, dish, ipHash, userAgent);
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
     * Gets popular dishes by view count from dish_views (last 30 days).
     * Falls back to featured dishes when there are no views.
     */
    private List<AnalyticsDTO.PopularDish> getPopularDishes(UUID restaurantId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dishCounts = entityManager.createQuery(
                "SELECT dv.dish.id, COUNT(dv.id) FROM DishView dv " +
                "WHERE dv.restaurant.id = :restaurantId AND dv.createdAt >= :since " +
                "GROUP BY dv.dish.id ORDER BY COUNT(dv.id) DESC",
                Object[].class
        )
        .setParameter("restaurantId", restaurantId)
        .setParameter("since", thirtyDaysAgo)
        .setMaxResults(10)
        .getResultList();

        if (dishCounts.isEmpty()) {
            // No views yet: return featured dishes with 0 views
            List<Dish> featuredDishes = Dish.find(
                    "restaurant.id = ?1 and featured = true and available = true and deletedAt is null order by updatedAt desc",
                    restaurantId
            ).page(0, 10).list();
            return featuredDishes.stream()
                    .map(dish -> new AnalyticsDTO.PopularDish(dish.id, dish.name, 0L, dish.available))
                    .collect(Collectors.toList());
        }

        List<AnalyticsDTO.PopularDish> result = new ArrayList<>();
        for (Object[] row : dishCounts) {
            UUID dishId = (UUID) row[0];
            long views = (Long) row[1];
            Dish dish = Dish.findById(dishId);
            if (dish != null && dish.restaurant.id.equals(restaurantId) && dish.deletedAt == null) {
                result.add(new AnalyticsDTO.PopularDish(dish.id, dish.name, views, dish.available));
            }
        }
        return result;
    }

    /**
     * Exports analytics data to CSV format.
     * Includes both menu views and dish views in a single file, sorted by date.
     * Columns: Type, Date, Time, IP Hash, User Agent, Dish ID, Dish Name
     */
    public String exportToCSV(UUID restaurantId, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<MenuView> menuViews = MenuView.find(
                "restaurant.id = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt",
                restaurantId,
                start,
                end
        ).list();

        List<DishView> dishViews = DishView.find(
                "restaurant.id = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt",
                restaurantId,
                start,
                end
        ).list();

        // Build unified rows (timestamp, type, ip, userAgent, dishId, dishName)
        List<ExportRow> rows = new ArrayList<>();
        for (MenuView v : menuViews) {
            rows.add(new ExportRow(v.createdAt, "menu", v.ipAddress, v.userAgent, null, null));
        }
        for (DishView v : dishViews) {
            String dishName = v.dish != null ? v.dish.name : null;
            rows.add(new ExportRow(v.createdAt, "dish", v.ipAddress, v.userAgent,
                    v.dish != null ? v.dish.id.toString() : null, dishName));
        }
        rows.sort((a, b) -> a.createdAt.compareTo(b.createdAt));

        StringBuilder csv = new StringBuilder();
        csv.append("Type,Date,Time,IP Hash,User Agent,Dish ID,Dish Name\n");
        for (ExportRow row : rows) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    row.type,
                    row.createdAt.toLocalDate(),
                    row.createdAt.toLocalTime(),
                    row.ipAddress != null ? row.ipAddress : "",
                    row.userAgent != null ? escapeCsv(row.userAgent) : "",
                    row.dishId != null ? row.dishId : "",
                    row.dishName != null ? escapeCsv(row.dishName) : ""
            ));
        }
        return csv.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        String s = value.replace("\"", "\"\"");
        return s.contains(",") || s.contains("\n") || s.contains("\"") ? "\"" + s + "\"" : s;
    }

    private static final class ExportRow {
        final LocalDateTime createdAt;
        final String type;
        final String ipAddress;
        final String userAgent;
        final String dishId;
        final String dishName;

        ExportRow(LocalDateTime createdAt, String type, String ipAddress, String userAgent, String dishId, String dishName) {
            this.createdAt = createdAt;
            this.type = type;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.dishId = dishId;
            this.dishName = dishName;
        }
    }
}

