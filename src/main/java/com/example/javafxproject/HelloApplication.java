package com.example.javafxproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// HelloApplication Class
//
// This class serves as the main entry point for the JavaFX Text Generator application.
// It initializes the primary stage, loads the main menu FXML layout, and sets up
// the initial scene with dimensions 800x600 pixels.
//
// The application title "Text Generator Project" is displayed in the window title bar.
// This class extends the JavaFX Application class and overrides the start method
// to define the application's initial interface.
//
// Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
//    NetID: AXK220194
//    Last modified: September 17, 2025

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Text Generator Project");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}