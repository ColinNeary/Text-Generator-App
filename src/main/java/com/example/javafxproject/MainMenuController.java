package com.example.javafxproject;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.util.DatabaseConnection; // ✅ Import your DB connection class

/**
 * MainMenuController Class
 *
 * This controller manages the main navigation menu and content area for the Text Generator AI application.
 * It handles page navigation between different application features, database connection initialization,
 * and dynamic content loading with smooth transitions. The controller also manages active button styling
 * to provide visual feedback for the current section.
 *
 * Features include:
 * - Dynamic page loading with fade transitions
 * - Database connection initialization
 * - Active button state management
 * - Centralized navigation control
 * - CSS styling integration
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: September 28, 2025
 */
public class MainMenuController {

    // FXML Injected Components
    @FXML
    private StackPane contentArea;

    @FXML
    private Button importFileButton;

    @FXML
    private Button autoCompleteButton;

    @FXML
    private Button sentenceGenButton;

    // ===================== INITIALIZATION =====================

    /**
     * Initializes the controller after FXML loading
     * Sets up database connection, CSS styling, and loads the default page
     * This method runs automatically when the FXML is loaded
     */
    @FXML
    public void initialize() {
        try {
            // ✅ Connect to Database (popup will appear)
            // Establishes connection to MySQL database and shows connection status
            DatabaseConnection.getConnection();

            // ✅ Load external CSS for consistent application styling
            String css = getClass().getResource("/com/example/javafxproject/styles.css").toExternalForm();
            if (contentArea != null) {
                contentArea.getStylesheets().add(css);
            }

            // ✅ Load default page (Import File) on application startup
            loadPage("/com/example/javafxproject/import-file.fxml", "📂 Import File");
            if (importFileButton != null) {
                setActiveButton(importFileButton);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== PAGE NAVIGATION METHODS =====================

    /**
     * Loads the Import File page when the corresponding button is clicked
     * Sets the Import File button as active and updates the content area
     *
     * @param event The action event triggered by button click
     */
    @FXML
    protected void loadImportFilePage(ActionEvent event) {
        loadPage("/com/example/javafxproject/import-file.fxml", "📂 Import File");
        setActiveButton(importFileButton);
    }

    /**
     * Loads the Auto Complete page when the corresponding button is clicked
     * Sets the Auto Complete button as active and updates the content area
     *
     * @param event The action event triggered by button click
     */
    @FXML
    protected void loadAutoCompletePage(ActionEvent event) {
        loadPage("/com/example/javafxproject/auto-complete.fxml", "⚡ Auto Complete");
        setActiveButton(autoCompleteButton);
    }

    /**
     * Loads the Sentence Generation page when the corresponding button is clicked
     * Sets the Sentence Generation button as active and updates the content area
     *
     * @param event The action event triggered by button click
     */
    @FXML
    protected void loadSentenceGenPage(ActionEvent event) {
        loadPage("/com/example/javafxproject/sentence-generation.fxml", "📝 Sentence Generation");
        setActiveButton(sentenceGenButton);
    }

    // ===================== CORE PAGE LOADING LOGIC =====================

    /**
     * Core method for loading FXML pages into the content area
     * Handles FXML loading, error checking, and smooth transitions
     *
     * @param fxmlPath The path to the FXML file to load
     * @param title The title to set for the application window
     */
    private void loadPage(String fxmlPath, String title) {
        try {
            // Load the FXML file using FXMLLoader
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("⚠️ FXML not found: " + fxmlPath);
                return;
            }

            // Load the FXML content as a Parent node
            Parent newContent = loader.load();
            if (contentArea == null) {
                System.err.println("❌ contentArea is null — ensure fx:id=\"contentArea\" in main-menu.fxml");
                return;
            }

            // Clear existing content and add new content
            contentArea.getChildren().clear();
            contentArea.getChildren().add(newContent);

            // Apply smooth fade-in transition for better user experience
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newContent);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Update application window title to reflect current page (if scene is available)
            if (contentArea.getScene() != null) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                if (stage != null) {
                    stage.setTitle("Text Generator AI Project - " + title);
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error loading FXML: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== ACTIVE BUTTON STYLING MANAGEMENT =====================

    /**
     * Sets the visual style for the active navigation button
     * Updates all buttons to inactive style and sets the specified button as active
     * Provides clear visual feedback for the current application section
     *
     * @param activeButton The button to set as active (currently selected section)
     */
    private void setActiveButton(Button activeButton) {
        if (activeButton == null) return;

        // Array of all navigation buttons for batch processing
        Button[] allButtons = {importFileButton, autoCompleteButton, sentenceGenButton};

        // Set all buttons to inactive style
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.setStyle("""
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #1e3c72;
                    -fx-background-color: linear-gradient(to right, #f8f9fa, #e9ecef);
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    -fx-effect: none;
                    """);
            }
        }

        // Set the active button to highlighted style
        activeButton.setStyle("""
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-text-fill: white;
            -fx-background-color: linear-gradient(to right, #00c6ff, #0072ff);
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow( gaussian , rgba(0,0,0,0.25) , 6,0,0,1 );
            """);
    }
}