module org.example.campusLink {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.sql;
    requires java.desktop;
    requires jdk.httpserver;
    requires javafx.graphics;

    opens org.example.campusLink.controllers to javafx.fxml, javafx.web;
    opens org.example.campusLink.entities to javafx.base;
    opens org.example.campusLink.services to javafx.fxml;
    opens org.example.campusLink.mains to javafx.graphics, javafx.fxml;
}