package org.example.campusLink.services.reviews;

import org.example.campusLink.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TrustPointsService {

    // Met à jour trust_points dans users ET enregistre dans trust_point_history
    public void applyPoints(int prestataireId, int points) {
        if (points == 0) return;

        updateTrustPointsInUsers(prestataireId, points);
        logToHistory(prestataireId, points);
    }

    private void updateTrustPointsInUsers(int prestataireId, int points) {
        String sql = "UPDATE users SET trust_points = trust_points + ? WHERE id = ?";

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, points);
                ps.setInt(2, prestataireId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void logToHistory(int prestataireId, int points) {
        // La table trust_point_history a un ENUM pour reason
        // On utilise REVIEW_RATING pour les avis
        String sql = """
            INSERT INTO trust_point_history (prestataire_id, points_added, reason)
            VALUES (?, ?, 'REVIEW_RATING')
        """;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, prestataireId);
                ps.setInt(2, points);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}