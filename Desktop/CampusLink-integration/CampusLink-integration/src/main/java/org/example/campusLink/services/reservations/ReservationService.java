package org.example.campusLink.services.reservations;

import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.utils.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    private static final String SELECT_WITH_DETAILS = """
        SELECT r.id, r.student_id, r.service_id, r.date, r.status, r.price, r.localisation,
               s.title          AS service_title,
               s.price          AS service_price,
               s.prestataire_id AS provider_id,
               u.name           AS student_name
        FROM reservations r
        JOIN services s ON s.id = r.service_id
        JOIN users    u ON u.id = r.student_id
    """;

    // =========================
    // CREATE
    // =========================
    public int add(Reservation r) throws SQLException {
        String sql = """
            INSERT INTO reservations (student_id, service_id, date, status, price, localisation)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getStudentId());
            ps.setInt(2, r.getServiceId());
            ps.setTimestamp(3, Timestamp.valueOf(r.getDate()));
            ps.setString(4, r.getStatus().name());
            ps.setBigDecimal(5, r.getPrice() != null ? r.getPrice() : BigDecimal.ZERO);
            ps.setString(6, r.getLocalisation() != null ? r.getLocalisation() : "");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // =========================
    // UPDATE
    // =========================
    public boolean update(Reservation r) throws SQLException {
        String sql = """
            UPDATE reservations
            SET student_id=?, service_id=?, date=?, status=?, price=?, localisation=?
            WHERE id=?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getStudentId());
            ps.setInt(2, r.getServiceId());
            ps.setTimestamp(3, Timestamp.valueOf(r.getDate()));
            ps.setString(4, r.getStatus().name());
            ps.setBigDecimal(5, r.getPrice() != null ? r.getPrice() : BigDecimal.ZERO);
            ps.setString(6, r.getLocalisation() != null ? r.getLocalisation() : "");
            ps.setInt(7, r.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // =========================
    // UPDATE STATUS
    // =========================
    public void updateStatus(int reservationId, ReservationStatus status) throws Exception {
        String sql = "UPDATE reservations SET status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }

        if (status == ReservationStatus.CONFIRMED) {
            Reservation r = getById(reservationId);
            if (r != null) {
                String providerPhone = getProviderPhone(r.getServiceId());
                String studentName   = getStudentName(r.getStudentId());
                if (providerPhone != null && !providerPhone.isEmpty()) {
                    org.example.campusLink.services.SmsService.sendSms(providerPhone, studentName + " a réservé ton service.");
                }
            }
        }
    }

    // =========================
    // DELETE
    // =========================
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM reservations WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================
    // READ basic
    // =========================
    public Reservation getById(int id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapBasic(rs) : null;
            }
        }
    }

    // =========================
    // READ with details
    // =========================
    public List<Reservation> getAllWithDetails() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS + " ORDER BY r.date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapWithDetails(rs));
        }
        return list;
    }

    public List<Reservation> getUpcomingWithDetails() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS + " WHERE r.date >= ? ORDER BY r.date ASC")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithDetails(rs));
            }
        }
        return list;
    }

    public List<Reservation> getPastWithDetails() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS + " WHERE r.date < ? ORDER BY r.date DESC")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithDetails(rs));
            }
        }
        return list;
    }

    public List<Reservation> getConfirmedReservationsByProvider(int providerId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS +
                        " WHERE s.prestataire_id = ? AND r.status = 'CONFIRMED'" +
                        " ORDER BY r.date ASC")) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithDetails(rs));
            }
        }
        return list;
    }

    public List<Reservation> getUpcomingReservationsForProvider(int providerId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS +
                        " WHERE s.prestataire_id = ? AND r.status = 'CONFIRMED' AND r.date >= ?" +
                        " ORDER BY r.date ASC")) {
            ps.setInt(1, providerId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithDetails(rs));
            }
        }
        return list;
    }

    public List<Reservation> getProviderReservationsByDate(int providerId, LocalDate date) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS +
                        " WHERE s.prestataire_id = ? AND DATE(r.date) = ?" +
                        " ORDER BY r.date ASC")) {
            ps.setInt(1, providerId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithDetails(rs));
            }
        }
        return list;
    }

    public List<Reservation> getProviderReservationsForMonth(int providerId, YearMonth month) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_WITH_DETAILS +
                        " WHERE s.prestataire_id = ? AND r.status = 'CONFIRMED'" +
                        " AND DATE(r.date) BETWEEN ? AND ?" +
                        " ORDER BY r.date ASC")) {
            ps.setInt(1, providerId);
            ps.setDate(2, java.sql.Date.valueOf(month.atDay(1)));
            ps.setDate(3, java.sql.Date.valueOf(month.atEndOfMonth()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithDetails(rs));
            }
        }
        return list;
    }

    public List<Reservation> getStudentReservations(int studentId) throws SQLException {
        String sql = """
            SELECT r.id, r.student_id, r.service_id, r.date, r.status, r.price, r.localisation,
                   s.title          AS service_title,
                   s.price          AS service_price,
                   s.prestataire_id AS provider_id,
                   u.name           AS student_name,
                   p.name           AS provider_name
            FROM reservations r
            JOIN services s ON s.id = r.service_id
            JOIN users    u ON u.id = r.student_id
            JOIN users    p ON p.id = s.prestataire_id
            WHERE r.student_id = ?
            ORDER BY r.date DESC
        """;
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapWithDetails(rs);
                    r.setProviderName(rs.getString("provider_name"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    // =========================
    // MAPPERS
    // =========================
    private Reservation mapBasic(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setStudentId(rs.getInt("student_id"));
        r.setServiceId(rs.getInt("service_id"));

        Timestamp t = rs.getTimestamp("date");
        r.setDate(t != null ? t.toLocalDateTime() : LocalDateTime.now());

        String statusStr = rs.getString("status");
        r.setStatus(statusStr != null
                ? ReservationStatus.valueOf(statusStr)
                : ReservationStatus.PENDING);

        try {
            BigDecimal price = rs.getBigDecimal("price");
            r.setPrice(price != null ? price : BigDecimal.ZERO);
        } catch (SQLException ignored) {
            r.setPrice(BigDecimal.ZERO);
        }

        try {
            r.setLocalisation(rs.getString("localisation"));
        } catch (SQLException ignored) {
            r.setLocalisation("");
        }

        return r;
    }

    private Reservation mapWithDetails(ResultSet rs) throws SQLException {
        Reservation r = mapBasic(rs);
        r.setServiceTitle(rs.getString("service_title"));
        r.setServicePrice(rs.getBigDecimal("service_price"));
        r.setStudentName(rs.getString("student_name"));
        r.setProviderId(rs.getInt("provider_id"));
        return r;
    }

    // =========================
    // HELPERS
    // =========================
    private String getStudentName(int studentId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT name FROM users WHERE id=?")) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("name") : "Un étudiant";
        }
    }

    private String getProviderPhone(int serviceId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("""
                SELECT u.phone FROM services s
                JOIN users u ON u.id = s.prestataire_id
                WHERE s.id = ?""")) {
            ps.setInt(1, serviceId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("phone") : null;
        }
    }
}