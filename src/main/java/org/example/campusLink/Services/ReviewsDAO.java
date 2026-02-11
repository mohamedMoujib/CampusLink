package org.example.campusLink.Services;

import org.example.campusLink.entities.Reviews;
import org.example.campusLink.units.MyDatabase;

import java.sql.*;

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

            // 🔥 récupérer l'id généré automatiquement
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== READ =====================

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
}
