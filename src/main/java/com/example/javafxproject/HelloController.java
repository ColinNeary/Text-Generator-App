package com.example.javafxproject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * HelloController Class
 *
 * This is a simple demonstration controller for the JavaFX application welcome screen.
 * It handles basic user interactions and serves as an introductory interface example.
 * The controller manages a welcome label that updates when the hello button is clicked.
 *
 * This class demonstrates fundamental FXML controller patterns and JavaFX event handling
 * for beginner-level application development and testing purposes.
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: September 15, 2025
 */
public class HelloController {

    /**
     * FXML injected welcome text label
     * This label displays the welcome message and updates on button click
     */
    @FXML
    private Label welcomeText;

    /**
     * Handles the hello button click event
     * Updates the welcome text label with a greeting message when triggered
     * This method demonstrates basic JavaFX event handling and UI updates
     */
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}