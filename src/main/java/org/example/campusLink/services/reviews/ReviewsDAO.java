package org.example.campusLink.Services.reviews;

import org.example.campusLink.entities.Reviews;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewsDAO {

    // ===================== CREATE =====================

    public void save(Reviews r) {
        String sql = """
            INSERT INTO reviews
            (student_id, prestataire_id, reservation_id, rating, comment, is_reported)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.getStudentId());
                ps.setInt(2, r.getPrestataireId());
                ps.setInt(3, r.getReservationId());
                ps.setInt(4, r.getRating());
                ps.setString(5, r.getComment());
                ps.setBoolean(6, false);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) r.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== CHECK EXISTENCE =====================

    public boolean existsByStudentAndService(int studentId, int reservationId) {
        String sql = "SELECT id FROM reviews WHERE student_id = ? AND reservation_id = ?";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setInt(2, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ===================== READ BY STUDENT =====================
    // reservations n'a pas prestataire_id → on passe par services.prestataire_id

    public List<Reviews> findByStudentWithDetails(int studentId) {
        List<Reviews> list = new ArrayList<>();

        String sql = """
            SELECT r.*,
                   s.title       AS service_title,
                   u.name        AS prestataire_name
            FROM reviews r
            JOIN reservations res ON r.reservation_id  = res.id
            JOIN services s       ON res.service_id    = s.id
            JOIN users u          ON s.prestataire_id  = u.id
            WHERE r.student_id = ?
            ORDER BY r.id DESC
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Reviews r = mapResultSetToReview(rs);
                        r.setServiceTitle(rs.getString("service_title"));
                        r.setPrestataireName(rs.getString("prestataire_name"));
                        list.add(r);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===================== READ BY TUTOR =====================

    public List<Reviews> findByTutorWithDetails(int tutorId) {
        List<Reviews> list = new ArrayList<>();

        String sql = """
            SELECT r.*,
                   s.title       AS service_title,
                   u.name        AS student_name
            FROM reviews r
            JOIN reservations res ON r.reservation_id  = res.id
            JOIN services s       ON res.service_id    = s.id
            JOIN users u          ON r.student_id      = u.id
            WHERE r.prestataire_id = ?
            ORDER BY r.id DESC
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, tutorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Reviews r = mapResultSetToReview(rs);
                        r.setServiceTitle(rs.getString("service_title"));
                        r.setStudentName(rs.getString("student_name"));
                        list.add(r);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===================== READ ALL (ADMIN) =====================

    public List<Reviews> findAllWithDetails() {
        List<Reviews> list = new ArrayList<>();

        String sql = """
            SELECT r.*,
                   s.title          AS service_title,
                   student.name     AS student_name,
                   prestataire.name AS prestataire_name
            FROM reviews r
            JOIN reservations res  ON r.reservation_id  = res.id
            JOIN services s        ON res.service_id    = s.id
            JOIN users student     ON r.student_id      = student.id
            JOIN users prestataire ON r.prestataire_id  = prestataire.id
            ORDER BY r.is_reported DESC, r.id DESC
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Reviews r = mapResultSetToReview(rs);
                        r.setServiceTitle(rs.getString("service_title"));
                        r.setStudentName(rs.getString("student_name"));
                        r.setPrestataireName(rs.getString("prestataire_name"));
                        list.add(r);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===================== REPORTED COUNT =====================

    public int getReportedReviewsCount() {
        String sql = "SELECT COUNT(*) FROM reviews WHERE is_reported = TRUE";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ===================== FIND BY ID =====================

    public Reviews findById(int id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSetToReview(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===================== UPDATE =====================

    public void update(Reviews r) {
        String sql = "UPDATE reviews SET rating = ?, comment = ? WHERE id = ?";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, r.getRating());
                ps.setString(2, r.getComment());
                ps.setInt(3, r.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== REPORT =====================

    public void reportReview(int reviewId, String reason) {
        String sql = """
            UPDATE reviews
            SET is_reported = TRUE, report_reason = ?, reported_at = ?
            WHERE id = ?
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reason);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(3, reviewId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== UNREPORT =====================

    public void unreportReview(int reviewId) {
        String sql = """
            UPDATE reviews
            SET is_reported = FALSE, report_reason = NULL, reported_at = NULL
            WHERE id = ?
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reviewId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== DELETE =====================

    public void delete(int id) {
        String sql = "DELETE FROM reviews WHERE id = ?";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== TRUST POINTS =====================
    // Colonne trust_points directement dans users

    public int getTrustPointsByUserId(int userId) {
        String sql = "SELECT trust_points FROM users WHERE id = ?";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("trust_points");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ===================== MAPPER =====================

    private Reviews mapResultSetToReview(ResultSet rs) throws SQLException {
        Reviews r = new Reviews();
        r.setId(rs.getInt("id"));
        r.setStudentId(rs.getInt("student_id"));
        r.setPrestataireId(rs.getInt("prestataire_id"));
        r.setReservationId(rs.getInt("reservation_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setReported(rs.getBoolean("is_reported"));
        r.setReportReason(rs.getString("report_reason"));

        Timestamp reportedAt = rs.getTimestamp("reported_at");
        if (reportedAt != null) r.setReportedAt(reportedAt.toLocalDateTime());

        return r;
    }
    public Integer findPrestataireIdByReservation(int reservationId) {
        String sql = """
        SELECT s.prestataire_id
        FROM reservations r
        JOIN services s ON r.service_id = s.id
        JOIN users u ON s.prestataire_id = u.id
        WHERE r.id = ?
          AND u.user_type = 'PRESTATAIRE'
    """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}