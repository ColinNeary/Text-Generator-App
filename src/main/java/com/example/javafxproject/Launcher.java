package com.example.javafxproject;

import javafx.application.Application;

/**
 * Launcher Class
 *
 * This class serves as the entry point for launching the Text Generator AI JavaFX application.
 * It provides a separate launch mechanism that can be useful for certain deployment scenarios
 * and IDE configurations that require a distinct main class separate from the Application class.
 *
 * The launcher uses the Application.launch() method provided by JavaFX to initialize
 * and start the HelloApplication class, which contains the main application logic.
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: September 15, 2025
 */
public class Launcher {

    /**
     * Main method - application entry point
     * Launches the JavaFX application by calling the Application.launch() method
     * on the HelloApplication class with the provided command line arguments
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}