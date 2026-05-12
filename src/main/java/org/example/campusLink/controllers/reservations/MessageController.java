package org.example.campusLink.controllers.reservations;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
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

    @FXML private ListView<User>    contactList;
    @FXML private ListView<Message> messageList;
    @FXML private TextField         messageField;

    @FXML private HBox chatHeader;
    @FXML private HBox inputArea;
    @FXML private VBox emptyState;

    @FXML private Label lblContactName;
    @FXML private Label lblContactEmail;
    @FXML private Label lblContactAvatar;
    @FXML private Label lblUnreadBadge;

    // ── Emoji panel (injected from FXML) ──────────────────────────────────────
    @FXML private FlowPane emojiPane;

    private final MessageService messageService = new MessageService();
    private final UserService    userService    = new UserService();

    private User    currentUser;
    private User    selectedContact;
    private Timer   refreshTimer;

    // TTS / STT state
    private boolean isRecording  = false;
    private Thread  sttThread    = null;

    // Emojis list
    private static final String[] EMOJIS = {
            "😀","😂","😍","🥰","😊","😎","🤔","😅","😭","😡",
            "👍","👎","👏","🙏","🤝","💪","🎉","🔥","❤️","💙",
            "✅","❌","⭐","🚀","💡","📚","🎓","🏫","📝","💬"
    };

    @FXML
    public void initialize() {
        setupContactCellFactory();
        setupMessageCellFactory();
        setupEmojiPane();
        messageField.setOnAction(e -> sendMessage());
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadContacts();
        startAutoRefresh();
    }

    // ── EMOJI PANEL ───────────────────────────────────────────────────────────

    private void setupEmojiPane() {
        emojiPane.setVisible(false);
        emojiPane.setManaged(false);
        emojiPane.setHgap(6);
        emojiPane.setVgap(6);
        emojiPane.setPadding(new Insets(8));
        emojiPane.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:#e5e7eb;" +
                        "-fx-border-radius:10;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-width:1;"
        );

        for (String emoji : EMOJIS) {
            Button btn = new Button(emoji);
            btn.setStyle(
                    "-fx-background-color:transparent;" +
                            "-fx-font-size:18px;" +
                            "-fx-cursor:hand;" +
                            "-fx-padding:4;"
            );
            btn.setOnAction(e -> {
                messageField.appendText(emoji);
                messageField.requestFocus();
                // hide panel after selection
                emojiPane.setVisible(false);
                emojiPane.setManaged(false);
            });
            emojiPane.getChildren().add(btn);
        }
    }

    @FXML
    private void toggleEmojiPanel() {
        boolean visible = !emojiPane.isVisible();
        emojiPane.setVisible(visible);
        emojiPane.setManaged(visible);
        if (visible) messageField.requestFocus();
    }

    // ── SPEECH TO TEXT (micro) ────────────────────────────────────────────────
    // Uses javafx.scene.web.WebView + JavaScript SpeechRecognition via a
    // lightweight approach: we open a hidden WebView that runs the browser API
    // and sends the result back via JSObject bridge.
    //
    // Alternative (no browser API): FreeTTS / CMU Sphinx — but those need extra
    // JARs. The WebView approach works out-of-the-box with JavaFX WebEngine.

    @FXML private Button btnMic;   // injected

    @FXML
    private void toggleSpeechToText() {
        if (!isRecording) {
            startListening();
        } else {
            stopListening();
        }
    }

    private javafx.scene.web.WebView hiddenWebView;
    private javafx.scene.web.WebEngine webEngine;

    private void startListening() {
        isRecording = true;
        btnMic.setStyle(
                "-fx-background-color:#ef4444; -fx-text-fill:white;" +
                        "-fx-background-radius:50; -fx-min-width:42; -fx-min-height:42;" +
                        "-fx-cursor:hand; -fx-font-size:16px;"
        );
        btnMic.setText("⏹");

        if (hiddenWebView == null) {
            hiddenWebView = new javafx.scene.web.WebView();
            webEngine = hiddenWebView.getEngine();

            // JavaScript bridge
            webEngine.getLoadWorker().stateProperty().addListener(
                    (obs, oldState, newState) -> {
                        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                            // Expose Java bridge to JS
                            netscape.javascript.JSObject window =
                                    (netscape.javascript.JSObject) webEngine.executeScript("window");
                            window.setMember("javaBridge", new JavaBridge());
                            webEngine.executeScript("startRecognition()");
                        }
                    }
            );
        }

        String html = buildSpeechHtml();
        webEngine.loadContent(html);
    }

    private void stopListening() {
        isRecording = false;
        btnMic.setStyle(
                "-fx-background-color:#6366f1; -fx-text-fill:white;" +
                        "-fx-background-radius:50; -fx-min-width:42; -fx-min-height:42;" +
                        "-fx-cursor:hand; -fx-font-size:16px;"
        );
        btnMic.setText("🎤");
        if (webEngine != null) {
            try { webEngine.executeScript("stopRecognition()"); }
            catch (Exception ignored) {}
        }
    }

    private String buildSpeechHtml() {
        return """
            <!DOCTYPE html><html><body>
            <script>
            var recognition;
            function startRecognition() {
                if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
                    javaBridge.onError('Speech Recognition not supported');
                    return;
                }
                recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
                recognition.lang = 'fr-FR';
                recognition.interimResults = false;
                recognition.maxAlternatives = 1;
                recognition.onresult = function(event) {
                    var transcript = event.results[0][0].transcript;
                    javaBridge.onResult(transcript);
                };
                recognition.onerror = function(event) {
                    javaBridge.onError(event.error);
                };
                recognition.onend = function() {
                    javaBridge.onEnd();
                };
                recognition.start();
            }
            function stopRecognition() {
                if (recognition) recognition.stop();
            }
            </script>
            </body></html>
            """;
    }

    // Bridge Java ↔ JavaScript
    public class JavaBridge {
        public void onResult(String transcript) {
            Platform.runLater(() -> {
                messageField.appendText(transcript);
                stopListening();
            });
        }
        public void onError(String error) {
            Platform.runLater(() -> {
                stopListening();
                showAlert("Micro", "Erreur reconnaissance vocale : " + error);
            });
        }
        public void onEnd() {
            Platform.runLater(() -> stopListening());
        }
    }

    // ── TEXT TO SPEECH (haut-parleur) ─────────────────────────────────────────
    // Uses the same WebEngine to call SpeechSynthesis API

    @FXML
    private void speakLastMessage() {
        if (messageList.getItems().isEmpty()) return;

        // Find last message NOT sent by me
        List<Message> items = messageList.getItems();
        String textToSpeak = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            Message m = items.get(i);
            if (m.getSenderId() != currentUser.getId()) {
                textToSpeak = m.getContent();
                break;
            }
        }
        if (textToSpeak == null) {
            // fallback: last message regardless
            textToSpeak = items.get(items.size() - 1).getContent();
        }

        final String text = textToSpeak
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        if (webEngine == null) {
            hiddenWebView = new javafx.scene.web.WebView();
            webEngine = hiddenWebView.getEngine();
        }

        final String js = String.format("""
            var u = new SpeechSynthesisUtterance('%s');
            u.lang = 'fr-FR';
            u.rate = 1.0;
            window.speechSynthesis.speak(u);
            """, text);

        String ttsHtml = """
            <!DOCTYPE html><html><body>
            <script>
            window.onload = function() {
                %s
            }
            </script></body></html>
            """.formatted(js);

        webEngine.loadContent(ttsHtml);
    }

    // ── CONTACTS ──────────────────────────────────────────────────────────────

    private void loadContacts() {
        try {
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

                StackPane avatarPane = new StackPane();
                Circle circle = new Circle(20);
                circle.setStyle("-fx-fill:#e0e7ff;");
                String initial = contact.getName() != null && !contact.getName().isEmpty()
                        ? String.valueOf(contact.getName().charAt(0)).toUpperCase() : "?";
                Label avatarLbl = new Label(initial);
                avatarLbl.setStyle(
                        "-fx-font-weight:bold; -fx-text-fill:#6366f1; -fx-font-size:14px;"
                );
                avatarPane.getChildren().addAll(circle, avatarLbl);

                VBox info = new VBox(3);
                Label nameLbl = new Label(contact.getName());
                nameLbl.setStyle(
                        "-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#111827;"
                );

                String preview = "";
                try {
                    List<Message> msgs = messageService.getConversation(
                            currentUser.getId(), contact.getId());
                    if (!msgs.isEmpty()) {
                        String last = msgs.get(msgs.size() - 1).getContent();
                        preview = last.length() > 35 ? last.substring(0, 32) + "..." : last;
                    }
                } catch (Exception ignored) {}

                Label previewLbl = new Label(preview);
                previewLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");

                info.getChildren().addAll(nameLbl, previewLbl);
                row.getChildren().addAll(avatarPane, info);

                boolean isSelected = selectedContact != null
                        && selectedContact.getId() == contact.getId();
                if (isSelected) {
                    row.setStyle(
                            "-fx-background-color:#ede9fe;" +
                                    "-fx-background-radius:8; -fx-padding:10 12;"
                    );
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

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        chatHeader.setVisible(true);
        chatHeader.setManaged(true);
        messageList.setVisible(true);
        messageList.setManaged(true);
        inputArea.setVisible(true);
        inputArea.setManaged(true);

        String initial = contact.getName() != null && !contact.getName().isEmpty()
                ? String.valueOf(contact.getName().charAt(0)).toUpperCase() : "?";
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

        // Hide emoji panel on send
        emojiPane.setVisible(false);
        emojiPane.setManaged(false);

        try {
            messageService.sendMessage(
                    currentUser.getId(),
                    selectedContact.getId(),
                    content
            );
            messageField.clear();
            loadMessages();
            loadContacts();
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
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    loadContacts();
                    if (selectedContact != null) loadMessages();
                });
            }
        }, 10_000, 10_000);
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) refreshTimer.cancel();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void openConversationWithUser(int userId) {
        try {
            User contact = userService.getById(userId);
            if (contact == null) return;
            boolean alreadyInList = contactList.getItems().stream()
                    .anyMatch(u -> u.getId() == userId);
            if (!alreadyInList) {
                contactList.getItems().add(0, contact);
            }
            openConversation(contact);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}