package org.example.campusLink.Services;
import org.example.campusLink.entities.Reviews;   // ✅ OBLIGATOIRE
import org.example.campusLink.units.MyDatabase;


import java.sql.*;


public class ReviewsDAO {

    public void save(Reviews r) {
        String sql = """
            INSERT INTO reviews
            (student_id, prestataire_id, reservation_id, rating, comment)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = MyDatabase.getInstance()
                .getConnection().prepareStatement(sql)) {

            ps.setInt(1, r.getStudentId());
            ps.setInt(2, r.getPrestataireId());
            ps.setInt(3, r.getReservationId());
            ps.setInt(4, r.getRating());
            ps.setString(5, r.getComment());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Reviews findById(int id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";

        try (PreparedStatement ps = MyDatabase.getInstance()
                .getConnection().prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

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

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean existsByStudentAndService(int studentId, int reservationId) {
        String sql = """
            SELECT id FROM reviews
            WHERE student_id = ? AND reservation_id = ?
        """;

        try (PreparedStatement ps = MyDatabase.getInstance()
                .getConnection().prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setInt(2, reservationId);
            return ps.executeQuery().next();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void update(Reviews r) {
        String sql = "UPDATE reviews SET rating = ?, comment = ? WHERE id = ?";

        try (PreparedStatement ps = MyDatabase.getInstance()
                .getConnection().prepareStatement(sql)) {

            ps.setInt(1, r.getRating());
            ps.setString(2, r.getComment());
            ps.setInt(3, r.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM reviews WHERE id = ?";

        try (PreparedStatement ps = MyDatabase.getInstance()
                .getConnection().prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
