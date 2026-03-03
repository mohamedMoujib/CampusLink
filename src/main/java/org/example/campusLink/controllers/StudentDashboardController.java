package org.example.campusLink.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.campusLink.entities.*;
import org.example.campusLink.services.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentDashboardController {

    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation,String> serviceCol;
    @FXML private TableColumn<Reservation,String> dateCol;
    @FXML private TableColumn<Reservation,String> statusCol;
    @FXML private TableColumn<Reservation,String> providerCol;
    @FXML private TableColumn<Reservation,String> priceCol;
    @FXML private TableColumn<Reservation,Void> actionsCol;

    private final ReservationService reservationService = new ReservationService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();
    private int studentId = 1;

    @FXML
    public void initialize() {

        serviceCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getServiceTitle()));
        dateCol.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getDate()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                )
        );
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().labelFr())
        );
        providerCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getProviderName())
        );
        priceCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPrice()+" DT")
        );

        addButtonsToTable();

        reservationsTable.setItems(data);
        loadReservations();
    }

    private void loadReservations() {
        try {
            List<Reservation> list =
                    reservationService.getStudentReservations(studentId);
            data.setAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addButtonsToTable() {
        actionsCol.setCellFactory(param -> new TableCell<>() {

            private final Button contactBtn = new Button("Contacter");
            private final Button cancelBtn = new Button("Annuler");
            private final Button coordBtn = new Button("Coordination");

            {
                contactBtn.getStyleClass().add("primary-btn");
                cancelBtn.getStyleClass().add("btn-danger-outline");
                coordBtn.getStyleClass().add("btn-success");

                contactBtn.setOnAction(e -> openChat(getCurrentReservation()));
                cancelBtn.setOnAction(e -> cancelReservation(getCurrentReservation()));
                coordBtn.setOnAction(e -> openCoordination(getCurrentReservation()));
            }

            private Reservation getCurrentReservation() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Reservation r = getCurrentReservation();
                HBox box = new HBox(10, contactBtn, cancelBtn);

                if (r.getStatus() == ReservationStatus.CONFIRMED) {
                    box.getChildren().add(coordBtn);
                }

                if (r.getStatus() == ReservationStatus.CANCELLED) {
                    box.getChildren().clear();
                }

                setGraphic(box);
            }
        });
    }

    private void cancelReservation(Reservation r) {
        try {
            reservationService.updateStatus(r.getId(), ReservationStatus.CANCELLED);
            loadReservations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openChat(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Messages.fxml"));
            Scene scene = new Scene(loader.load());

            MessageController controller = loader.getController();
            controller.initData(r, studentId, r.getProviderId());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setTitle("Messagerie");
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCoordination(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/coordination.fxml"));
            Scene scene = new Scene(loader.load());

            CoordinationController controller = loader.getController();
            controller.initData(r);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setTitle("Coordination & Paiement");
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openCreateModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_create.fxml"));
            Scene scene = new Scene(loader.load());

            ReservationCreateController controller = loader.getController();
            controller.initData(studentId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Réserver une session");
            stage.setScene(scene);
            stage.showAndWait();

            loadReservations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}