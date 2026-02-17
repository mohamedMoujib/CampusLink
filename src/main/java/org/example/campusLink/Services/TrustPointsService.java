package org.example.campusLink.services;

import org.example.campusLink.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TrustPointsService {

    public void applyPoints(int prestataireId, int points) {

        Connection conn = null;

        try {
            conn = MyDatabase.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1️⃣ update users.trust_points
            updateUserTrustPoints(conn, prestataireId, points);

            // 2️⃣ historique
            saveTrustHistory(conn, prestataireId, points);

            conn.commit();

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void updateUserTrustPoints(Connection conn, int prestataireId, int points)
            throws SQLException {

        String sql = """
            UPDATE users
            SET trust_points = trust_points + ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, prestataireId);
            ps.executeUpdate();
        }
    }

    private void saveTrustHistory(Connection conn, int prestataireId, int points)
            throws SQLException {

        String sql = """
            INSERT INTO trust_point_history
            (prestataire_id, points_added, reason)
            VALUES (?, ?, 'REVIEW_RATING')
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prestataireId);
            ps.setInt(2, points);
            ps.executeUpdate();
        }
    }
}
