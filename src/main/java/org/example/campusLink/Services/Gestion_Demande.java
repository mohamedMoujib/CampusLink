package org.example.campusLink.Services;

import org.example.campusLink.entities.Demandes;
import org.example.campusLink.utils.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Demandes (Student Service Requests)
 * Handles all database operations for the demandes table
 *
 * FIXED: Database uses 'name' column in users table, not 'username'
 */
public class Gestion_Demande {

    private final Connection connection;

    public Gestion_Demande() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Add a new demande (service request)
     * Validates: service existence, duplicate requests
     * REMOVED: Service status check (was blocking all demandes)
     */
    public void ajouterDemande(Demandes d) throws SQLException {

        // Vérifier que le service existe
        String checkService = "SELECT id FROM services WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkService)) {
            ps.setInt(1, d.getServiceId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IllegalStateException("Service introuvable");
            }
        }

        // Vérifier doublon - student ne peut pas avoir plusieurs demandes actives pour le même service
        String checkDuplicate = """
            SELECT COUNT(*) FROM demandes
            WHERE student_id = ?
            AND service_id = ?
            AND status IN ('EN_ATTENTE','CONFIRMEE')
        """;

        try (PreparedStatement ps = connection.prepareStatement(checkDuplicate)) {
            ps.setInt(1, d.getStudentId());
            ps.setInt(2, d.getServiceId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new IllegalStateException("Vous avez déjà une demande active pour ce service");
            }
        }

        // Insérer la nouvelle demande
        String sql = """
            INSERT INTO demandes
            (student_id, service_id, prestataire_id, message, requested_date, proposed_price, status)
            VALUES (?, ?, ?, ?, ?, ?, 'EN_ATTENTE')
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getStudentId());
            ps.setInt(2, d.getServiceId());
            ps.setInt(3, d.getPrestataireId());
            ps.setString(4, d.getMessage());

            if (d.getRequestedDate() != null) {
                ps.setTimestamp(5, d.getRequestedDate());
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }

            if (d.getProposedPrice() != null) {
                ps.setBigDecimal(6, d.getProposedPrice());
            } else {
                ps.setNull(6, Types.DECIMAL);
            }

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    d.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Modify demande status with state machine validation
     * Valid transitions:
     * - EN_ATTENTE -> CONFIRMEE, REFUSEE
     * - CONFIRMEE -> TERMINEE
     * - REFUSEE, TERMINEE -> No transitions (final states)
     */
    public void modifierStatut(int demandeId, String nouveauStatut) throws SQLException {

        // Validate status value
        if (!nouveauStatut.matches("EN_ATTENTE|CONFIRMEE|REFUSEE|TERMINEE")) {
            throw new IllegalArgumentException("Statut invalide: " + nouveauStatut);
        }

        // Get current status
        String getCurrent = "SELECT status FROM demandes WHERE id = ?";
        String currentStatus = null;

        try (PreparedStatement ps = connection.prepareStatement(getCurrent)) {
            ps.setInt(1, demandeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentStatus = rs.getString("status");
            }
        }

        if (currentStatus == null) {
            throw new IllegalStateException("Demande introuvable (ID: " + demandeId + ")");
        }

        // Validate state transitions
        boolean validTransition = switch (currentStatus) {
            case "EN_ATTENTE" -> nouveauStatut.equals("CONFIRMEE") || nouveauStatut.equals("REFUSEE");
            case "CONFIRMEE" -> nouveauStatut.equals("TERMINEE");
            case "REFUSEE", "TERMINEE" -> false; // Final states
            default -> false;
        };

        if (!validTransition) {
            throw new IllegalStateException(
                    "Transition impossible: " + currentStatus + " -> " + nouveauStatut
            );
        }

        // Update status
        String update = "UPDATE demandes SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(update)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, demandeId);
            ps.executeUpdate();
        }
    }

    /**
     * Display all demandes with optional status filter
     * Includes JOINs for service, student, prestataire, and category info
     * NOTE: users table does NOT have 'username' column, using fallback
     */
    public List<Demandes> afficherDemandes(String statusFilter) throws SQLException {
        List<Demandes> demandes = new ArrayList<>();

        String sql = """
            SELECT 
                d.id,
                d.student_id,
                d.service_id,
                d.prestataire_id,
                d.message,
                d.requested_date,
                d.proposed_price,
                d.status,
                d.created_at,
                s.title as service_name,
                s.description as service_description,
                s.price as service_price,
                c.name as category_name
            FROM demandes d
            LEFT JOIN services s ON d.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
        """;

        if (statusFilter != null && !statusFilter.equals("TOUS")) {
            sql += " WHERE d.status = ?";
        }

        sql += " ORDER BY d.created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (statusFilter != null && !statusFilter.equals("TOUS")) {
                ps.setString(1, statusFilter);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Demandes d = new Demandes();
                d.setId(rs.getInt("id"));
                d.setStudentId(rs.getInt("student_id"));
                d.setServiceId(rs.getInt("service_id"));
                d.setPrestataireId(rs.getInt("prestataire_id"));
                d.setMessage(rs.getString("message"));
                d.setRequestedDate(rs.getTimestamp("requested_date"));
                d.setProposedPrice(rs.getBigDecimal("proposed_price"));
                d.setStatus(rs.getString("status"));
                d.setCreatedAt(rs.getTimestamp("created_at"));

                // Set display fields
                d.setServiceName(rs.getString("service_name"));
                d.setServiceDescription(rs.getString("service_description"));
                d.setServicePrice(rs.getBigDecimal("service_price"));
                d.setStudentName("Étudiant #" + d.getStudentId()); // Fallback
                d.setPrestataireName("Prestataire #" + d.getPrestataireId()); // Fallback
                d.setCategoryName(rs.getString("category_name"));

                demandes.add(d);
            }
        }

        return demandes;
    }

    /**
     * Display all demandes (no filter)
     */
    public List<Demandes> afficherDemandes() throws SQLException {
        return afficherDemandes("TOUS");
    }

    /**
     * Get demandes for a specific student
     */
    public List<Demandes> afficherDemandesParEtudiant(int studentId) throws SQLException {
        List<Demandes> demandes = new ArrayList<>();

        String sql = """
            SELECT 
                d.id,
                d.student_id,
                d.service_id,
                d.prestataire_id,
                d.message,
                d.requested_date,
                d.proposed_price,
                d.status,
                d.created_at,
                s.title as service_name,
                s.description as service_description,
                s.price as service_price,
                c.name as category_name
            FROM demandes d
            LEFT JOIN services s ON d.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE d.student_id = ?
            ORDER BY d.created_at DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Demandes d = mapResultSetToDemande(rs);
                demandes.add(d);
            }
        }

        return demandes;
    }

    /**
     * Get demandes for a specific prestataire
     */
    public List<Demandes> afficherDemandesParPrestataire(int prestataireId) throws SQLException {
        List<Demandes> demandes = new ArrayList<>();

        String sql = """
            SELECT 
                d.id,
                d.student_id,
                d.service_id,
                d.prestataire_id,
                d.message,
                d.requested_date,
                d.proposed_price,
                d.status,
                d.created_at,
                s.title as service_name,
                s.description as service_description,
                s.price as service_price,
                c.name as category_name
            FROM demandes d
            LEFT JOIN services s ON d.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE d.prestataire_id = ?
            ORDER BY d.created_at DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, prestataireId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Demandes d = mapResultSetToDemande(rs);
                demandes.add(d);
            }
        }

        return demandes;
    }

    /**
     * Get demandes for a specific service
     */
    public List<Demandes> afficherDemandesParService(int serviceId) throws SQLException {
        List<Demandes> demandes = new ArrayList<>();

        String sql = """
            SELECT 
                d.id,
                d.student_id,
                d.service_id,
                d.prestataire_id,
                d.message,
                d.requested_date,
                d.proposed_price,
                d.status,
                d.created_at,
                s.title as service_name,
                s.description as service_description,
                s.price as service_price,
                c.name as category_name
            FROM demandes d
            LEFT JOIN services s ON d.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE d.service_id = ?
            ORDER BY d.created_at DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Demandes d = mapResultSetToDemande(rs);
                demandes.add(d);
            }
        }

        return demandes;
    }

    /**
     * Delete a demande
     */
    public void supprimerDemande(int demandeId) throws SQLException {
        String sql = "DELETE FROM demandes WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new IllegalStateException("Demande introuvable (ID: " + demandeId + ")");
            }
        }
    }

    /**
     * Get a specific demande by ID
     */
    public Demandes getDemandeById(int demandeId) throws SQLException {
        String sql = """
            SELECT 
                d.id,
                d.student_id,
                d.service_id,
                d.prestataire_id,
                d.message,
                d.requested_date,
                d.proposed_price,
                d.status,
                d.created_at,
                s.title as service_name,
                s.description as service_description,
                s.price as service_price,
                c.name as category_name
            FROM demandes d
            LEFT JOIN services s ON d.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE d.id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToDemande(rs);
            }
        }

        return null;
    }

    /**
     * Count demandes by status
     */
    public int compterDemandesParStatut(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM demandes WHERE status = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * Get statistics summary
     */
    public java.util.Map<String, Integer> getStatistiquesDemandes() throws SQLException {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();

        String sql = "SELECT status, COUNT(*) as count FROM demandes GROUP BY status";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                stats.put(rs.getString("status"), rs.getInt("count"));
            }
        }

        return stats;
    }

    /**
     * Update demande proposed price
     */
    public void modifierPrixPropose(int demandeId, BigDecimal nouveauPrix) throws SQLException {
        String sql = "UPDATE demandes SET proposed_price = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, nouveauPrix);
            ps.setInt(2, demandeId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException("Demande introuvable (ID: " + demandeId + ")");
            }
        }
    }

    /**
     * Update demande requested date
     */
    public void modifierDateDemandee(int demandeId, Timestamp nouvelleDate) throws SQLException {
        String sql = "UPDATE demandes SET requested_date = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, nouvelleDate);
            ps.setInt(2, demandeId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException("Demande introuvable (ID: " + demandeId + ")");
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Map ResultSet to Demandes object
     * NOTE: Using fallback for student/prestataire names since users table structure is unknown
     */
    private Demandes mapResultSetToDemande(ResultSet rs) throws SQLException {
        Demandes d = new Demandes();
        d.setId(rs.getInt("id"));
        d.setStudentId(rs.getInt("student_id"));
        d.setServiceId(rs.getInt("service_id"));
        d.setPrestataireId(rs.getInt("prestataire_id"));
        d.setMessage(rs.getString("message"));
        d.setRequestedDate(rs.getTimestamp("requested_date"));
        d.setProposedPrice(rs.getBigDecimal("proposed_price"));
        d.setStatus(rs.getString("status"));
        d.setCreatedAt(rs.getTimestamp("created_at"));

        // Set display fields
        d.setServiceName(rs.getString("service_name"));
        d.setServiceDescription(rs.getString("service_description"));
        d.setServicePrice(rs.getBigDecimal("service_price"));
        d.setStudentName("Étudiant #" + d.getStudentId()); // Fallback - users table structure unknown
        d.setPrestataireName("Prestataire #" + d.getPrestataireId()); // Fallback
        d.setCategoryName(rs.getString("category_name"));

        return d;
    }
}