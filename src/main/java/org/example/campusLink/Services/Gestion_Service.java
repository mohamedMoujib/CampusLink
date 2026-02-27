package org.example.campusLink.Services;

import org.example.campusLink.entities.Services;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Services (Offerings by providers)
 * ✅ FIXED: All Integer comparison issues resolved
 */
public class Gestion_Service {

    private final Connection connection;

    public Gestion_Service() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ==================== CREATE ====================

    /**
     * ✅ FIXED: Now properly sets all 6 parameters including category_id
     */
    public void ajouterService(Services s) throws SQLException {

        if (s.getTitle() == null || s.getTitle().isBlank())
            throw new IllegalArgumentException("Titre obligatoire");

        if (s.getPrice() <= 0)
            throw new IllegalArgumentException("Le prix doit être positif");

        String sql = """
        INSERT INTO services
        (title, description, image, price, prestataire_id, category_id)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getTitle());

            // Handle NULL description
            if (s.getDescription() != null && !s.getDescription().isEmpty()) {
                ps.setString(2, s.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            // Handle NULL image
            if (s.getImage() != null && !s.getImage().isEmpty()) {
                ps.setString(3, s.getImage());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setDouble(4, s.getPrice());
            ps.setInt(5, s.getPrestataireId());

            // ✅ FIX: Set parameter 6 (category_id) - Handle Integer properly
            Integer categoryId = s.getCategoryId();
            if (categoryId != null && categoryId > 0) {
                ps.setInt(6, categoryId);
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    s.setId(generatedKeys.getInt(1));
                    System.out.println("✅ Service created with ID: " + s.getId());
                }
            }
        }
    }

    // ==================== READ ====================

    /**
     * ✅ Afficher tous les services avec informations enrichies
     */
    public List<Services> afficherServices() throws SQLException {
        List<Services> list = new ArrayList<>();
        String sql = """
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            ORDER BY s.id DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Services s = mapResultSetToService(rs);
                list.add(s);
            }
        }
        return list;
    }

    /**
     * ✅ Récupérer un service par son ID avec informations enrichies
     */
    public Services getServiceById(int serviceId) throws SQLException {
        String sql = """
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            WHERE s.id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToService(rs);
            }
        }

        throw new SQLException("Service introuvable (ID: " + serviceId + ")");
    }

    /**
     * ✅ Afficher les services d'un prestataire spécifique
     */
    public List<Services> afficherServicesParPrestataire(int prestataireId) throws SQLException {
        List<Services> list = new ArrayList<>();
        String sql = """
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            WHERE s.prestataire_id = ?
            ORDER BY s.id DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, prestataireId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Services s = mapResultSetToService(rs);
                list.add(s);
            }
        }
        return list;
    }

    /**
     * ✅ Afficher les services par catégorie
     */
    public List<Services> afficherServicesParCategorie(int categoryId) throws SQLException {
        List<Services> list = new ArrayList<>();
        String sql = """
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            WHERE s.category_id = ?
            ORDER BY s.id DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Services s = mapResultSetToService(rs);
                list.add(s);
            }
        }
        return list;
    }

    /**
     * ✅ Rechercher des services par titre ou description
     */
    public List<Services> rechercherServices(String keyword) throws SQLException {
        List<Services> list = new ArrayList<>();
        String sql = """
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            WHERE s.title LIKE ? OR s.description LIKE ?
            ORDER BY s.id DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Services s = mapResultSetToService(rs);
                list.add(s);
            }
        }
        return list;
    }

    /**
     * ✅ Recherche avancée avec filtres multiples
     */
    public List<Services> rechercherServicesAvance(String keyword, Integer categoryId,
                                                   Double prixMin, Double prixMax) throws SQLException {
        List<Services> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (s.title LIKE ? OR s.description LIKE ?)");
            String searchPattern = "%" + keyword + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND s.category_id = ?");
            params.add(categoryId);
        }

        if (prixMin != null && prixMin > 0) {
            sql.append(" AND s.price >= ?");
            params.add(prixMin);
        }

        if (prixMax != null && prixMax > 0) {
            sql.append(" AND s.price <= ?");
            params.add(prixMax);
        }

        sql.append(" ORDER BY s.id DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Services s = mapResultSetToService(rs);
                list.add(s);
            }
        }
        return list;
    }

    // ==================== UPDATE ====================

    /**
     * ✅ Modifier un service existant avec validation complète
     */
    public void modifierService(Services s) throws SQLException {

        if (s.getId() <= 0) {
            throw new IllegalArgumentException("ID du service invalide");
        }

        if (!serviceExists(s.getId())) {
            throw new IllegalArgumentException("Service introuvable (ID: " + s.getId() + ")");
        }

        if (s.getTitle() == null || s.getTitle().isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }

        if (s.getTitle().length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }

        if (s.getTitle().length() > 200) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 200 caractères");
        }

        if (s.getDescription() != null && s.getDescription().length() > 1000) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 1000 caractères");
        }

        if (s.getPrice() <= 0) {
            throw new IllegalArgumentException("Le prix doit être supérieur à 0");
        }

        if (s.getPrice() > 10000) {
            throw new IllegalArgumentException("Le prix ne peut pas dépasser 10000€");
        }

        if (s.getPrestataireId() > 0 && !prestataireExists(s.getPrestataireId())) {
            throw new IllegalArgumentException("Prestataire invalide");
        }

        // ✅ FIX: Handle Integer comparison properly
        Integer categoryId = s.getCategoryId();
        if (categoryId != null && categoryId > 0 && !categoryExists(categoryId)) {
            throw new IllegalArgumentException("Catégorie invalide");
        }

        String sql = """
            UPDATE services 
            SET title = ?,
                description = ?,
                image = ?,
                price = ?,
                prestataire_id = ?,
                category_id = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getTitle().trim());

            if (s.getDescription() != null && !s.getDescription().isEmpty()) {
                ps.setString(2, s.getDescription().trim());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            if (s.getImage() != null && !s.getImage().isEmpty()) {
                ps.setString(3, s.getImage());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setDouble(4, s.getPrice());
            ps.setInt(5, s.getPrestataireId());

            // ✅ FIX: Handle Integer properly
            Integer categoryIdUpdate = s.getCategoryId();
            if (categoryIdUpdate != null && categoryIdUpdate > 0) {
                ps.setInt(6, categoryIdUpdate);
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setInt(7, s.getId());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Échec de la modification du service");
            }
        }
    }

    /**
     * ✅ Changer le statut d'un service
     */
    public void changerStatutService(int serviceId, String newStatus) throws SQLException {

        if (!newStatus.equals("ACTIF") && !newStatus.equals("INACTIF") &&
                !newStatus.equals("EN_ATTENTE") && !newStatus.equals("REJETE")) {
            throw new IllegalArgumentException("Statut invalide");
        }

        String sql = """
            UPDATE services 
            SET status = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, serviceId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Service introuvable");
            }
        }
    }

    // ==================== DELETE ====================

    /**
     * ✅ Supprimer un service avec vérification des demandes actives
     */
    public void supprimerService(int serviceId) throws SQLException {

        String checkSql = """
            SELECT COUNT(*) FROM demandes
            WHERE service_id = ?
            AND status IN ('EN_ATTENTE','CONFIRMEE')
        """;

        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setInt(1, serviceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new IllegalStateException(
                        "Impossible de supprimer un service avec des réservations actives"
                );
            }
        }

        String deleteSql = "DELETE FROM services WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setInt(1, serviceId);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Service introuvable (ID: " + serviceId + ")");
            }
        }
    }

    // ==================== STATISTICS ====================

    /**
     * ✅ Compter le nombre total de services
     */
    public int compterServices() throws SQLException {
        String sql = "SELECT COUNT(*) FROM services";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * ✅ Compter les services par statut
     */
    public int compterServicesParStatut(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM services WHERE status = ?";

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
     * ✅ Obtenir les statistiques des services
     */
    public java.util.Map<String, Integer> getStatistiquesServices() throws SQLException {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();

        String sql = "SELECT status, COUNT(*) as count FROM services GROUP BY status";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                stats.put(rs.getString("status"), rs.getInt("count"));
            }
        }

        return stats;
    }

    /**
     * ✅ Obtenir les services les plus demandés
     */
    public List<Services> getServicesPlusDemandesAvecDetails() throws SQLException {
        List<Services> list = new ArrayList<>();
        String sql = """
            SELECT 
                s.id,
                s.title,
                s.description,
                s.image,
                s.price,
                s.prestataire_id,
                s.category_id,
                s.status,
                c.name as category_name,
                u.name as prestataire_name,
                COUNT(d.id) as demande_count
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            LEFT JOIN demandes d ON s.id = d.service_id
            GROUP BY s.id, s.title, s.description, s.image, s.price, 
                     s.prestataire_id, s.category_id, s.status,
                     c.name, u.name
            ORDER BY demande_count DESC
            LIMIT 10
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Services s = mapResultSetToService(rs);
                list.add(s);
            }
        }
        return list;
    }

    // ==================== VALIDATION HELPERS ====================

    private boolean serviceExists(int serviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM services WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean prestataireExists(int prestataireId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, prestataireId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean categoryExists(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // ==================== MAPPER HELPER ====================

    private Services mapResultSetToService(ResultSet rs) throws SQLException {
        Services s = new Services();
        s.setId(rs.getInt("id"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setImage(rs.getString("image"));
        s.setPrice(rs.getDouble("price"));
        s.setPrestataireId(rs.getInt("prestataire_id"));
        s.setCategoryId(rs.getInt("category_id"));
        s.setStatus(rs.getString("status"));

        try {
            s.setCategoryName(rs.getString("category_name"));
        } catch (SQLException e) {
            // Column doesn't exist, skip
        }

        try {
            s.setPrestataireName(rs.getString("prestataire_name"));
        } catch (SQLException e) {
            // Column doesn't exist, skip
        }

        return s;
    }
}