package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import org.example.campusLink.entities.Message;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.services.MessageService;
import org.example.campusLink.services.UserService;

import java.util.List;

public class MessageController {

    @FXML private Label contactNameLabel;
    @FXML private ListView<Message> messageList;
    @FXML private TextField messageField;

    private final MessageService messageService = new MessageService();
    private final UserService userService = new UserService();

    private int reservationId;
    private int currentUserId;
    private int otherUserId;

    // ===============================
    // INIT DATA (appelé depuis ReservationsController)
    // ===============================
    public void initData(Reservation reservation, int currentUserId, int otherUserId) {
        this.reservationId = reservation.getId();
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;

        try {
            String name = userService.getUserNameById(otherUserId);
            contactNameLabel.setText("Chat avec " + name);
        } catch (Exception e) {
            contactNameLabel.setText("Conversation");
        }

        setupMessageCellFactory();
        loadMessages();
    }

    // ===============================
    // Affichage bulle droite/gauche
    // ===============================
    private void setupMessageCellFactory() {
        messageList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);

                if (empty || msg == null) {
                    setGraphic(null);
                    return;
                }

                Label messageLabel = new Label(msg.getContent());
                messageLabel.setWrapText(true);
                messageLabel.setMaxWidth(300);
                messageLabel.getStyleClass().add("message-bubble");

                HBox container = new HBox();
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                if (msg.getSenderId() == currentUserId) {
                    messageLabel.getStyleClass().add("message-bubble-mine");
                    container.setAlignment(Pos.CENTER_RIGHT);
                    container.getChildren().addAll(spacer, messageLabel);
                } else {
                    messageLabel.getStyleClass().add("message-bubble-other");
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.getChildren().addAll(messageLabel, spacer);
                }

                setGraphic(container);
            }
        });
    }

    // ===============================
    // Charger messages
    // ===============================
    private void loadMessages() {
        try {
            messageList.getItems().clear();

            List<Message> messages = messageService.getConversation(
                    currentUserId,
                    otherUserId
            );

            messageList.getItems().addAll(messages);
            messageList.scrollTo(messages.size() - 1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===============================
    // Envoyer message
    // ===============================
    @FXML
    private void sendMessage() {
        try {
            String content = messageField.getText();
            if (content == null || content.isEmpty()) return;

            messageService.sendMessage(
                    currentUserId,
                    otherUserId,
                    content
            );

            messageField.clear();
            loadMessages();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}