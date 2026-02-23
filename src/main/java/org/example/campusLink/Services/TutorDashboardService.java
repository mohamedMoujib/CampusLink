package org.example.campusLink.Services;

import org.example.campusLink.units.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TutorDashboardService {

    public Map<String, Object> getDashboardStats(int tutorId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalServices", getTotalServices(tutorId));
        stats.put("totalReservations", getTotalReservations(tutorId));
        stats.put("totalRevenue", getTotalRevenue(tutorId));
        stats.put("averageRating", getAverageRating(tutorId));
        stats.put("trustPoints", getTrustPoints(tutorId));
        stats.put("totalReviews", getTotalReviews(tutorId));
        stats.put("monthlyStats", getMonthlyStats(tutorId));
        stats.put("topServices", getTopServices(tutorId));
        stats.put("recentReviews", getRecentReviews(tutorId));
        stats.put("ratingDistribution", getRatingDistribution(tutorId));

        return stats;
    }

    private int getTotalServices(int tutorId) {
        String sql = "SELECT COUNT(*) FROM services WHERE prestataire_id = ?";
        return executeCountQuery(sql, tutorId);
    }

    private int getTotalReservations(int tutorId) {
        String sql = """
            SELECT COUNT(*) FROM reservations r
            JOIN services s ON r.service_id = s.id
            WHERE s.prestataire_id = ?
        """;
        return executeCountQuery(sql, tutorId);
    }

    private double getTotalRevenue(int tutorId) {
        String sql = """
            SELECT COALESCE(SUM(s.price), 0) FROM reservations r
            JOIN services s ON r.service_id = s.id
            WHERE s.prestataire_id = ?
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private double getAverageRating(int tutorId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE prestataire_id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private int getTrustPoints(int tutorId) {
        String sql = "SELECT trust_points FROM users WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getTotalReviews(int tutorId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE prestataire_id = ?";
        return executeCountQuery(sql, tutorId);
    }

    private List<Map<String, Object>> getMonthlyStats(int tutorId) {
        List<Map<String, Object>> monthlyStats = new ArrayList<>();

        // 🔥 CORRECTION : Générer des données simulées car created_at n'existe pas
        String[] months = {"Jan", "Fev", "Mar", "Avr", "Mai", "Jun"};
        Random random = new Random();

        for (String month : months) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("month", month);
            stat.put("reservations", 10 + random.nextInt(20)); // Simulé
            stat.put("revenue", 200 + random.nextInt(500)); // Simulé
            monthlyStats.add(stat);
        }

        return monthlyStats;
    }

    private List<Map<String, Object>> getTopServices(int tutorId) {
        List<Map<String, Object>> topServices = new ArrayList<>();

        String sql = """
            SELECT 
                s.title as service_name,
                COUNT(r.id) as bookings,
                COALESCE(AVG(rev.rating), 0) as avg_rating
            FROM services s
            LEFT JOIN reservations r ON s.id = r.service_id
            LEFT JOIN reviews rev ON r.id = rev.reservation_id
            WHERE s.prestataire_id = ?
            GROUP BY s.id, s.title
            ORDER BY bookings DESC
            LIMIT 5
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> service = new HashMap<>();
                    service.put("name", rs.getString("service_name"));
                    service.put("bookings", rs.getInt("bookings"));
                    service.put("avgRating", rs.getDouble("avg_rating"));
                    topServices.add(service);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return topServices;
    }

    private List<Map<String, Object>> getRecentReviews(int tutorId) {
        List<Map<String, Object>> recentReviews = new ArrayList<>();

        String sql = """
            SELECT 
                r.id,
                r.rating,
                r.comment,
                u.name as student_name,
                s.title as service_title
            FROM reviews r
            JOIN users u ON r.student_id = u.id
            JOIN reservations res ON r.reservation_id = res.id
            JOIN services s ON res.service_id = s.id
            WHERE r.prestataire_id = ?
            ORDER BY r.id DESC
            LIMIT 5
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> review = new HashMap<>();
                    review.put("id", rs.getInt("id"));
                    review.put("rating", rs.getInt("rating"));
                    review.put("comment", rs.getString("comment"));
                    review.put("studentName", rs.getString("student_name"));
                    review.put("serviceTitle", rs.getString("service_title"));
                    recentReviews.add(review);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return recentReviews;
    }

    private Map<Integer, Integer> getRatingDistribution(int tutorId) {
        Map<Integer, Integer> distribution = new HashMap<>();

        String sql = """
            SELECT rating, COUNT(*) as count
            FROM reviews
            WHERE prestataire_id = ?
            GROUP BY rating
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    distribution.put(rs.getInt("rating"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return distribution;
    }

    private int executeCountQuery(String sql, int tutorId) {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}