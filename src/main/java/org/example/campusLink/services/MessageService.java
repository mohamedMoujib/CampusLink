package org.example.campusLink.services;

import org.example.campusLink.entities.Message;
import org.example.campusLink.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // Envoyer message
    public void sendMessage(int senderId, int receiverId, String content) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    // Récupérer conversation entre deux users
    public List<Message> getConversation(int userA, int userB) throws SQLException {
        String sql = """
            SELECT * FROM messages
            WHERE (sender_id=? AND receiver_id=?)
               OR (sender_id=? AND receiver_id=?)
            ORDER BY created_at ASC
        """;

        List<Message> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getInt("id"));
                m.setSenderId(rs.getInt("sender_id"));
                m.setReceiverId(rs.getInt("receiver_id"));
                m.setContent(rs.getString("content"));
                m.setTimestamp(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(m);
            }
        }
        return list;
    }

    // Contacts de l'utilisateur connecté
    public List<Integer> getUserContacts(int userId) throws SQLException {
        String sql = """
            SELECT DISTINCT
            CASE
                WHEN sender_id = ? THEN receiver_id
                ELSE sender_id
            END AS contact_id
            FROM messages
            WHERE sender_id = ? OR receiver_id = ?
        """;

        List<Integer> contacts = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contacts.add(rs.getInt("contact_id"));
            }
        }
        return contacts;
    }
}