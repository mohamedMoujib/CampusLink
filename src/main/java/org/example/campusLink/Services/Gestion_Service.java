package org.example.campusLink.Services;

import org.example.campusLink.entities.Services;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Services (Offerings by providers)
 * Enhanced with methods needed for CreateDemande_controller
 */
public class Gestion_Service {

    private final Connection connection;

    public Gestion_Service() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ==================== CREATE ====================

    public void ajouterService(Services s) throws SQLException {

        if (s.getTitle() == null || s.getTitle().isBlank())
            throw new IllegalArgumentException("Titre obligatoire");

        if (s.getPrice() <= 0)
            throw new IllegalArgumentException("Le prix doit être positif");

        String sql = """
            INSERT INTO services
            (title, description, image, price, prestataire_id, category_id, status)
            VALUES (?, ?, ?, ?, ?, ?, 'EN_ATTENTE')
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getDescription());
            ps.setString(3, s.getImage());
            ps.setDouble(4, s.getPrice());
            ps.setInt(5, s.getPrestataireId());
            ps.setInt(6, s.getCategoryId());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    s.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    // ==================== READ ====================

    /**
     * ✅ Afficher tous les services avec informations enrichies
     * ENHANCED: Inclut le nom de la catégorie et du prestataire
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
     * ENHANCED: Inclut le nom de la catégorie et du prestataire
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
     * ✅ Afficher les services d'un prestataire spécifique avec informations enrichies
     * ENHANCED: Inclut le nom de la catégorie
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
     * NEW METHOD: Utile pour filtrer les services dans l'interface
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
     * ENHANCED: Inclut le nom de la catégorie et du prestataire
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
     * NEW METHOD: Pour la page de recherche avec filtres
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

        // Filtre par mot-clé
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (s.title LIKE ? OR s.description LIKE ?)");
            String searchPattern = "%" + keyword + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Filtre par catégorie
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND s.category_id = ?");
            params.add(categoryId);
        }

        // Filtre par prix minimum
        if (prixMin != null && prixMin > 0) {
            sql.append(" AND s.price >= ?");
            params.add(prixMin);
        }

        // Filtre par prix maximum
        if (prixMax != null && prixMax > 0) {
            sql.append(" AND s.price <= ?");
            params.add(prixMax);
        }

        sql.append(" ORDER BY s.id DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            // Assigner les paramètres
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

        // ===== VALIDATION DES DONNÉES =====

        // 1. Vérifier que le service existe
        if (s.getId() <= 0) {
            throw new IllegalArgumentException("ID du service invalide");
        }

        // 2. Vérifier que le service existe dans la base
        if (!serviceExists(s.getId())) {
            throw new IllegalArgumentException("Service introuvable (ID: " + s.getId() + ")");
        }

        // 3. Valider le titre
        if (s.getTitle() == null || s.getTitle().isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }

        if (s.getTitle().length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }

        if (s.getTitle().length() > 200) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 200 caractères");
        }

        // 4. Valider la description
        if (s.getDescription() != null && s.getDescription().length() > 1000) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 1000 caractères");
        }

        // 5. Valider le prix
        if (s.getPrice() <= 0) {
            throw new IllegalArgumentException("Le prix doit être supérieur à 0");
        }

        if (s.getPrice() > 10000) {
            throw new IllegalArgumentException("Le prix ne peut pas dépasser 10000€");
        }

        // 6. Vérifier que le prestataire existe
        if (s.getPrestataireId() > 0 && !prestataireExists(s.getPrestataireId())) {
            throw new IllegalArgumentException("Prestataire invalide");
        }

        // 7. Vérifier que la catégorie existe
        if (s.getCategoryId() > 0 && !categoryExists(s.getCategoryId())) {
            throw new IllegalArgumentException("Catégorie invalide");
        }

        // ===== MISE À JOUR EN BASE DE DONNÉES =====

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
            ps.setString(2, s.getDescription() != null ? s.getDescription().trim() : null);
            ps.setString(3, s.getImage());
            ps.setDouble(4, s.getPrice());
            ps.setInt(5, s.getPrestataireId());
            ps.setInt(6, s.getCategoryId());
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

        // Validation du statut
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
                        "Impossible de supprimer un service avec des demandes actives"
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
     * NEW METHOD: Pour les statistiques du dashboard
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
     * NEW METHOD: Pour le dashboard admin
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
     * NEW METHOD: Pour recommandations et statistiques
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
                // Note: demande_count could be added to Services entity if needed
                list.add(s);
            }
        }
        return list;
    }

    // ==================== VALIDATION HELPERS ====================

    /**
     * ✅ Vérifier si un service existe
     */
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

    /**
     * ✅ Vérifier si un prestataire existe
     */
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

    /**
     * ✅ Vérifier si une catégorie existe
     */
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

    /**
     * ✅ Mapper ResultSet vers Services entity
     * IMPORTANT: Includes categoryName and prestataireName for display
     */
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

        // Set display fields (if columns exist)
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