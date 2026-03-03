package org.example.campusLink.controllers.reservations;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.entities.Services;
import org.example.campusLink.services.Gestion_Service;
import org.example.campusLink.services.reservations.NotificationService;
import org.example.campusLink.services.reservations.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ReservationCreateController {

    @FXML private ComboBox<Services> cbService;
    @FXML private Label lblServiceTitle;
    @FXML private Label lblTutor;
    @FXML private Label lblPrice;
    @FXML private TextField tfLocation;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbTime;
    @FXML private TextArea taNote;
    @FXML private Button btnConfirm;

    private int studentId;
    private Reservation existingReservation;
    private boolean isEditMode = false;

    private final ReservationService reservationService = new ReservationService();
    private final Gestion_Service serviceService = new Gestion_Service();

    @FXML
    public void initialize() {
        loadServices();

        // Update labels when service is selected
        cbService.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblServiceTitle.setText(newVal.getTitle());
                lblTutor.setText("avec " + newVal.getPrestataireDisplayName());
                lblPrice.setText(newVal.getFormattedPrice());
            } else {
                lblServiceTitle.setText("");
                lblTutor.setText("");
                lblPrice.setText("");
            }
        });

        cbTime.setItems(FXCollections.observableArrayList(
                "08:00", "09:00", "10:00", "11:00",
                "14:00", "15:00", "16:00", "17:00"
        ));

        // Disable confirm until all required fields are filled
        btnConfirm.disableProperty().bind(
                dpDate.valueProperty().isNull()
                        .or(cbTime.valueProperty().isNull())
                        .or(cbService.valueProperty().isNull())
        );
    }

    private void loadServices() {
        try {
            List<Services> services = serviceService.afficherServices();
            ObservableList<Services> serviceList = FXCollections.observableArrayList(services);
            cbService.setItems(serviceList);
            cbService.setConverter(new StringConverter<Services>() {
                @Override
                public String toString(Services service) {
                    if (service == null) return "";
                    String provider = service.getPrestataireName() != null
                            ? " — " + service.getPrestataireName()
                            : "";
                    return service.getTitle() + provider;
                }
                @Override
                public Services fromString(String string) {
                    return null; // not needed for ComboBox selection
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services : " + e.getMessage());
        }
    }

    // ── MODE: CREATE ──────────────────────────────────────────────────────────

    public void initData(int studentId) {
        this.studentId = studentId;
        this.isEditMode = false;
        cbService.setDisable(false);
    }

    // ── MODE: EDIT ────────────────────────────────────────────────────────────

    public void initForEdit(Reservation r) {
        this.studentId = r.getStudentId();
        this.existingReservation = r;
        this.isEditMode = true;

        try {
            List<Services> services = serviceService.afficherServices();
            cbService.setItems(FXCollections.observableArrayList(services));

            Services selected = services.stream()
                    .filter(s -> s.getId() == r.getServiceId())
                    .findFirst().orElse(null);
            cbService.getSelectionModel().select(selected);
            cbService.setDisable(true); // can't change service in edit mode

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services : " + e.getMessage());
        }

        dpDate.setValue(r.getDate().toLocalDate());
        cbTime.setValue(r.getDate().toLocalTime()
                .withSecond(0).withNano(0).toString());
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    @FXML
    public void onClose() {
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void onConfirm() {
        try {
            LocalDate date    = dpDate.getValue();
            String timeStr    = cbTime.getValue();

            if (date == null || timeStr == null) {
                showAlert("Attention", "Veuillez choisir une date et une heure.");
                return;
            }

            if (date.isBefore(LocalDate.now())) {
                showAlert("Erreur", "La date ne peut pas être dans le passé.");
                return;
            }

            LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.parse(timeStr));

            if (isEditMode) {
                existingReservation.setDate(dateTime);

                boolean updated = reservationService.update(existingReservation);
                if (updated) {
                    showAlert("Succès", "Réservation mise à jour.");
                    onClose();
                } else {
                    showAlert("Erreur", "Échec de la mise à jour.");
                }

            } else {
                Services selected = cbService.getValue();
                if (selected == null) {
                    showAlert("Erreur", "Veuillez sélectionner un service.");
                    return;
                }

                Reservation r = new Reservation();
                r.setStudentId(studentId);
                r.setServiceId(selected.getId());
                r.setDate(dateTime);
                r.setStatus(ReservationStatus.PENDING);
                // ✅ Use BigDecimal since ReservationService.add() expects it
                r.setPrice(BigDecimal.valueOf(selected.getPrice()));

                int id = reservationService.add(r);
                if (id > 0) {
                    NotificationService.getInstance()
                            .addNotification("Nouvelle réservation : " + selected.getTitle());
                    showAlert("Succès", "Réservation créée avec succès !");
                    onClose();
                } else {
                    showAlert("Erreur", "Échec de la création.");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", ex.getMessage());
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        Alert.AlertType type = "Erreur".equals(title)
                ? Alert.AlertType.ERROR
                : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}