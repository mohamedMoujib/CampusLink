package org.example.campusLink.controllers;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.entities.Service;
import org.example.campusLink.services.NotificationService;
import org.example.campusLink.services.ReservationService;
import org.example.campusLink.services.ServiceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ReservationCreateController {

    @FXML private ComboBox<Service> cbService;
    @FXML private Label lblServiceTitle;
    @FXML private Label lblTutor;
    @FXML private Label lblPrice;
    @FXML private TextField tfLocation;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbTime;
    @FXML private TextArea taNote;
    @FXML private Button btnConfirm;

    private int studentId;
    private Reservation existingReservation; // si mode édition
    private boolean isEditMode = false;

    private final ReservationService reservationService = new ReservationService();
    private final ServiceService serviceService = new ServiceService();

    @FXML
    public void initialize() {
        // Charger les services
        try {
            List<Service> services = serviceService.getAllWithProviderName(); // suppose que cette méthode existe
            ObservableList<Service> serviceList = FXCollections.observableArrayList(services);
            cbService.setItems(serviceList);
            cbService.setConverter(new javafx.util.StringConverter<Service>() {
                @Override
                public String toString(Service service) {
                    return service == null ? "" : service.getTitle();
                }
                @Override
                public Service fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services : " + e.getMessage());
        }

        // Mise à jour des labels quand un service est sélectionné
        cbService.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblServiceTitle.setText(newVal.getTitle());
                lblTutor.setText("avec " + newVal.getProviderName()); // suppose que Service a getProviderName()
                lblPrice.setText(newVal.getPrice() + " DT");
            } else {
                lblServiceTitle.setText("");
                lblTutor.setText("");
                lblPrice.setText("");
            }
        });

        cbTime.setItems(FXCollections.observableArrayList(
                "08:00","09:00","10:00","11:00","14:00","15:00","16:00","17:00"
        ));

        // Validation du bouton
        btnConfirm.disableProperty().bind(
                dpDate.valueProperty().isNull()
                        .or(cbTime.valueProperty().isNull())
                        .or(cbService.valueProperty().isNull())
        );
    }

    // Pour la création
    public void initData(int studentId) {
        this.studentId = studentId;
        this.isEditMode = false;
        cbService.setDisable(false);
    }

    // Pour l'édition
    public void initForEdit(Reservation r) {
        this.studentId = r.getStudentId();
        this.existingReservation = r;
        this.isEditMode = true;

        try {
            List<Service> services = serviceService.getAllWithProviderName();
            ObservableList<Service> serviceList = FXCollections.observableArrayList(services);
            cbService.setItems(serviceList);
            Service selectedService = services.stream()
                    .filter(s -> s.getId() == r.getServiceId())
                    .findFirst().orElse(null);
            cbService.getSelectionModel().select(selectedService);
            cbService.setDisable(true);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services : " + e.getMessage());
        }

        dpDate.setValue(r.getDate().toLocalDate());
        cbTime.setValue(r.getDate().toLocalTime().toString());
        taNote.setText(r.getNote());
        tfLocation.setText(r.getLocalisation());
    }

    @FXML
    public void onClose() {
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void onConfirm() {
        try {
            LocalDate date = dpDate.getValue();
            String timeStr = cbTime.getValue();

            if (date == null || timeStr == null) {
                showAlert("Attention", "Veuillez choisir une date et une heure.");
                return;
            }

            if (date.isBefore(LocalDate.now())) {
                showAlert("Erreur", "La date ne peut pas être dans le passé.");
                return;
            }

            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime dateTime = LocalDateTime.of(date, time);

            Reservation r;
            if (isEditMode) {
                r = existingReservation;
                r.setDate(dateTime);
                r.setNote(taNote.getText());
                boolean updated = reservationService.update(r);
                if (updated) {
                    showAlert("Succès", "Réservation mise à jour.");
                } else {
                    showAlert("Erreur", "Échec de la mise à jour.");
                    return;
                }
            } else {
                r = new Reservation();
                r.setStudentId(studentId);
                Service selectedService = cbService.getValue();
                if (selectedService == null) {
                    showAlert("Erreur", "Veuillez sélectionner un service.");
                    return;
                }
                r.setServiceId(selectedService.getId());
                r.setDate(dateTime);
                r.setStatus(ReservationStatus.PENDING);
                r.setNote(taNote.getText());
                r.setPrice(selectedService.getPrice());
                int id = reservationService.add(r);
                if (id > 0) {
                    showAlert("Succès", "Réservation créée ");
                    // Déclencher la notification
                    NotificationService.getInstance().addNotification("Nouvelle réservation : " + selectedService.getTitle());
                } else {
                    showAlert("Erreur", "Échec de la création.");
                    return;
                }
            }
            onClose();

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}