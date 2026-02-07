package cloudSecurity.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for analytics (optional feature).
 */
public class AnalyticsDTO {

    /**
     * Analytics dashboard response.
     */
    public record AnalyticsDashboardResponse(
            UUID restaurantId,
            String restaurantName,
            AnalyticsSummary summary,
            List<DailyView> dailyViews,
            List<PopularDish> popularDishes,
            LocalDateTime lastUpdated
    ) {
    }

    /**
     * Summary statistics.
     */
    public record AnalyticsSummary(
            long totalViews,
            long viewsLast7Days,
            long viewsLast30Days,
            long uniqueVisitors,
            int totalDishes,
            int availableDishes,
            int totalCategories
    ) {
    }

    /**
     * Daily view statistics.
     */
    public record DailyView(
            String date,
            long views,
            long uniqueVisitors
    ) {
    }

    /**
     * Popular dish statistics.
     */
    public record PopularDish(
            UUID dishId,
            String dishName,
            long views,
            boolean available
    ) {
    }

    /**
     * Export request parameters.
     */
    public record ExportRequest(
            String format,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
    }
}

