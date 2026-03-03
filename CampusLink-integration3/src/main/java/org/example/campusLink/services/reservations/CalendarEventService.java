package org.example.campusLink.services.reservations;

import org.example.campusLink.entities.CalendarEvent;
import org.example.campusLink.utils.MyDatabase;


import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CalendarEventService {
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public int add(CalendarEvent e) throws SQLException {
        String sql = "INSERT INTO calendar_events (reservation_id, event_date, note) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getReservationId());
            ps.setTimestamp(2, Timestamp.valueOf(e.getEventDate()));
            ps.setString(3, e.getNote());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public List<CalendarEvent> getAll() throws SQLException {
        String sql = "SELECT * FROM calendar_events ORDER BY event_date DESC";
        List<CalendarEvent> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<CalendarEvent> getByReservation(int reservationId) throws SQLException {
        String sql = "SELECT * FROM calendar_events WHERE reservation_id=? ORDER BY event_date DESC";
        List<CalendarEvent> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean update(CalendarEvent e) throws SQLException {
        String sql = "UPDATE calendar_events SET reservation_id=?, event_date=?, note=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, e.getReservationId());
            ps.setTimestamp(2, Timestamp.valueOf(e.getEventDate()));
            ps.setString(3, e.getNote());
            ps.setInt(4, e.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM calendar_events WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private CalendarEvent map(ResultSet rs) throws SQLException {
        CalendarEvent e = new CalendarEvent();
        e.setId(rs.getInt("id"));
        e.setReservationId(rs.getInt("reservation_id"));
        Timestamp t = rs.getTimestamp("event_date");
        e.setEventDate(t != null ? t.toLocalDateTime() : LocalDateTime.now());
        e.setNote(rs.getString("note"));
        return e;
    }
}
