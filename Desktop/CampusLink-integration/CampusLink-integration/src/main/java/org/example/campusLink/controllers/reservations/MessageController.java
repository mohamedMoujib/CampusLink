package org.example.campusLink.controllers.reservations;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.example.campusLink.entities.Message;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.reservations.MessageService;
import org.example.campusLink.services.users.UserService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MessageController {

    @FXML private ListView<User> contactList;
    @FXML private ListView<Message> messageList;
    @FXML private TextField messageField;

    @FXML private HBox chatHeader;
    @FXML private HBox inputArea;
    @FXML private VBox emptyState;

    @FXML private Label lblContactName;
    @FXML private Label lblContactEmail;
    @FXML private Label lblContactAvatar;
    @FXML private Label lblUnreadBadge;

    private final MessageService messageService = new MessageService();
    private final UserService userService = new UserService();

    private User currentUser;
    private User selectedContact;
    private Timer refreshTimer;

    @FXML
    public void initialize() {
        setupContactCellFactory();
        setupMessageCellFactory();
        messageField.setOnAction(e -> sendMessage());
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadContacts();
        startAutoRefresh();
    }

    // ── CONTACTS ──────────────────────────────────────────────────────────────

    private void loadContacts() {
        try {
            // getUserContacts() returns List<Integer> (contact IDs)
            List<Integer> contactIds =
                    messageService.getUserContacts(currentUser.getId());

            List<User> contacts = new ArrayList<>();
            for (int id : contactIds) {
                try {
                    User u = userService.getById(id);
                    if (u != null) contacts.add(u);
                } catch (Exception ignored) {}
            }

            contactList.setItems(FXCollections.observableArrayList(contacts));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupContactCellFactory() {
        contactList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) { setGraphic(null); return; }

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 12, 10, 12));

                // Avatar circle with initial
                StackPane avatarPane = new StackPane();
                Circle circle = new Circle(20);
                circle.setStyle("-fx-fill:#e0e7ff;");
                String initial = contact.getName() != null && !contact.getName().isEmpty()
                        ? String.valueOf(contact.getName().charAt(0)).toUpperCase()
                        : "?";
                Label avatarLbl = new Label(initial);
                avatarLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#6366f1;" +
                        "-fx-font-size:14px;");
                avatarPane.getChildren().addAll(circle, avatarLbl);

                // Name + last message preview
                VBox info = new VBox(3);
                Label nameLbl = new Label(contact.getName());
                nameLbl.setStyle("-fx-font-weight:bold; -fx-font-size:13px;" +
                        "-fx-text-fill:#111827;");

                String preview = "";
                try {
                    List<Message> msgs = messageService.getConversation(
                            currentUser.getId(), contact.getId());
                    if (!msgs.isEmpty()) {
                        String last = msgs.get(msgs.size() - 1).getContent();
                        preview = last.length() > 35
                                ? last.substring(0, 32) + "..."
                                : last;
                    }
                } catch (Exception ignored) {}

                Label previewLbl = new Label(preview);
                previewLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");

                info.getChildren().addAll(nameLbl, previewLbl);
                row.getChildren().addAll(avatarPane, info);

                // Highlight if selected
                boolean isSelected = selectedContact != null
                        && selectedContact.getId() == contact.getId();
                if (isSelected) {
                    row.setStyle("-fx-background-color:#ede9fe;" +
                            "-fx-background-radius:8; -fx-padding:10 12;");
                }

                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
                setOnMouseClicked(e -> openConversation(contact));
            }
        });
    }

    private void openConversation(User contact) {
        this.selectedContact = contact;
        contactList.refresh();

        // Show chat UI, hide empty state
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        chatHeader.setVisible(true);
        chatHeader.setManaged(true);
        messageList.setVisible(true);
        messageList.setManaged(true);
        inputArea.setVisible(true);
        inputArea.setManaged(true);

        // Update header info
        String initial = contact.getName() != null && !contact.getName().isEmpty()
                ? String.valueOf(contact.getName().charAt(0)).toUpperCase()
                : "?";
        lblContactAvatar.setText(initial);
        lblContactName.setText(contact.getName());
        lblContactEmail.setText(contact.getEmail() != null ? contact.getEmail() : "");

        loadMessages();
        messageField.requestFocus();
    }

    // ── MESSAGES ──────────────────────────────────────────────────────────────

    private void loadMessages() {
        if (selectedContact == null) return;
        try {
            List<Message> messages = messageService.getConversation(
                    currentUser.getId(), selectedContact.getId());
            messageList.setItems(FXCollections.observableArrayList(messages));
            scrollToBottom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupMessageCellFactory() {
        messageList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) { setGraphic(null); return; }

                boolean isMine = msg.getSenderId() == currentUser.getId();

                // Message bubble
                Label bubble = new Label(msg.getContent());
                bubble.setWrapText(true);
                bubble.setMaxWidth(380);
                bubble.setPadding(new Insets(10, 14, 10, 14));
                bubble.setStyle(
                        "-fx-background-radius:16; -fx-font-size:13px; " +
                                (isMine
                                        ? "-fx-background-color:#6366f1; -fx-text-fill:white;"
                                        : "-fx-background-color:white; -fx-text-fill:#111827;" +
                                        "-fx-border-color:#e5e7eb; -fx-border-radius:16;" +
                                        "-fx-border-width:1;")
                );

                // Timestamp
                Label time = new Label("");
                if (msg.getTimestamp() != null) {
                    time.setText(msg.getTimestamp()
                            .format(DateTimeFormatter.ofPattern("HH:mm")));
                }
                time.setStyle("-fx-font-size:10px; -fx-text-fill:#9ca3af;");

                VBox msgBox = new VBox(3, bubble, time);
                msgBox.setMaxWidth(400);
                msgBox.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                HBox row = new HBox(msgBox);
                row.setPadding(new Insets(4, 16, 4, 16));
                row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    @FXML
    private void sendMessage() {
        if (selectedContact == null || currentUser == null) return;
        String content = messageField.getText();
        if (content == null || content.isBlank()) return;

        try {
            messageService.sendMessage(
                    currentUser.getId(),
                    selectedContact.getId(),
                    content
            );
            messageField.clear();
            loadMessages();
            loadContacts(); // refresh preview
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void scrollToBottom() {
        if (!messageList.getItems().isEmpty()) {
            Platform.runLater(() ->
                    messageList.scrollTo(messageList.getItems().size() - 1));
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true); // daemon thread
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    loadContacts();
                    if (selectedContact != null) loadMessages();
                });
            }
        }, 10_000, 10_000); // refresh every 10s
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) refreshTimer.cancel();
    }
    // Add this public method to MessagerieController:
    public void openConversationWithUser(int userId) {
        try {
            User contact = userService.getById(userId);
            if (contact != null) openConversation(contact);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}