package org.example.campusLink.Services;

import org.example.campusLink.entities.Reviews;
import org.example.campusLink.units.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewsDAO {

    // ===================== CREATE =====================

    public void save(Reviews r) {

        String sql = """
            INSERT INTO reviews
            (student_id, prestataire_id, reservation_id, rating, comment)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, r.getStudentId());
            ps.setInt(2, r.getPrestataireId());
            ps.setInt(3, r.getReservationId());
            ps.setInt(4, r.getRating());
            ps.setString(5, r.getComment());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== READ BY ID =====================

    public Reviews findById(int id) {

        String sql = "SELECT * FROM reviews WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    Reviews r = new Reviews();
                    r.setId(rs.getInt("id"));
                    r.setStudentId(rs.getInt("student_id"));
                    r.setPrestataireId(rs.getInt("prestataire_id"));
                    r.setReservationId(rs.getInt("reservation_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setComment(rs.getString("comment"));
                    return r;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 🔥 MÉTHODE AVEC JOIN POUR RÉCUPÉRER LES NOMS

    public List<ReviewWithDetails> findByStudentWithDetails(int studentId) {

        List<ReviewWithDetails> list = new ArrayList<>();

        String sql = """
            SELECT 
                r.id,
                r.student_id,
                r.prestataire_id,
                r.reservation_id,
                r.rating,
                r.comment,
                r.created_at,
                u.name as prestataire_name,
                s.name as service_name
            FROM reviews r
            LEFT JOIN user u ON r.prestataire_id = u.id
            LEFT JOIN user_roles ur ON u.id = ur.user_id AND ur.role_id = 2
            LEFT JOIN reservations res ON r.reservation_id = res.id
            LEFT JOIN services s ON res.service_id = s.id
            WHERE r.student_id = ?
            ORDER BY r.created_at DESC
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Reviews r = new Reviews();
                    r.setId(rs.getInt("id"));
                    r.setStudentId(rs.getInt("student_id"));
                    r.setPrestataireId(rs.getInt("prestataire_id"));
                    r.setReservationId(rs.getInt("reservation_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setComment(rs.getString("comment"));

                    String prestataireName = rs.getString("prestataire_name");
                    String serviceName = rs.getString("service_name");
                    Timestamp createdAt = rs.getTimestamp("created_at");

                    ReviewWithDetails rwd = new ReviewWithDetails(
                            r,
                            prestataireName != null ? prestataireName : "Prestataire inconnu",
                            serviceName != null ? serviceName : "Service inconnu",
                            createdAt
                    );

                    list.add(rwd);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // Ancienne méthode conservée pour compatibilité
    public List<Reviews> findByStudent(int studentId) {

        List<Reviews> list = new ArrayList<>();

        String sql = "SELECT * FROM reviews WHERE student_id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Reviews r = new Reviews();
                    r.setId(rs.getInt("id"));
                    r.setStudentId(rs.getInt("student_id"));
                    r.setPrestataireId(rs.getInt("prestataire_id"));
                    r.setReservationId(rs.getInt("reservation_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setComment(rs.getString("comment"));

                    list.add(r);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===================== CHECK EXISTENCE =====================

    public boolean existsByStudentAndService(int studentId, int reservationId) {

        String sql = """
            SELECT id FROM reviews
            WHERE student_id = ? AND reservation_id = ?
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setInt(2, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ===================== UPDATE =====================

    public void update(Reviews r) {

        String sql = """
            UPDATE reviews
            SET rating = ?, comment = ?
            WHERE id = ?
        """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, r.getRating());
            ps.setString(2, r.getComment());
            ps.setInt(3, r.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== DELETE =====================

    public void delete(int id) {

        String sql = "DELETE FROM reviews WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== CLASSE INTERNE POUR LES DÉTAILS =====================

    public static class ReviewWithDetails {
        private Reviews review;
        private String prestataireName;
        private String serviceName;
        private Timestamp createdAt;

        public ReviewWithDetails(Reviews review, String prestataireName,
                                 String serviceName, Timestamp createdAt) {
            this.review = review;
            this.prestataireName = prestataireName;
            this.serviceName = serviceName;
            this.createdAt = createdAt;
        }

        public Reviews getReview() { return review; }
        public String getPrestataireName() { return prestataireName; }
        public String getServiceName() { return serviceName; }
        public Timestamp getCreatedAt() { return createdAt; }
    }
}