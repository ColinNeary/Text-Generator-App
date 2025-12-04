// Written by Raghav Mukundan, rxm210068

package com.example.javafxproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.example.backend.sentencegen.GreedySentenceGenerator;
import com.example.backend.sentencegen.BeamSearchSentenceGenerator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for sentence generation with Greedy and Beam Search algorithms
 */
public class SentenceGenerationController {

    // FXML Injected Components
    @FXML private VBox root;
    @FXML private TextField startingWordField;
    @FXML private Button greedyButton;
    @FXML private Button beamSearchButton;
    @FXML private Button resetButton;
    @FXML private VBox sentenceContainer;
    @FXML private Label algorithmLabel;
    @FXML private Label wordCountLabel;

    // Backend algorithm instances
    private GreedySentenceGenerator greedyGenerator;
    private BeamSearchSentenceGenerator beamSearchGenerator;

    // Initialize sentence generators and setup UI
    @FXML
    public void initialize() {
        try {
            // Initialize sentence generators with max length and debug mode disabled
            greedyGenerator = new GreedySentenceGenerator(15, false);
            beamSearchGenerator = new BeamSearchSentenceGenerator(2, 15, false);
            
            setupButtonHoverEffects();
            setupWordAutocomplete(startingWordField);
            
            System.out.println("✓ SentenceGenerationController initialized successfully");
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Setup autocomplete dropdown for text field with database word suggestions
    private void setupWordAutocomplete(TextField textField) {
        ContextMenu suggestionsMenu = new ContextMenu();
        
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                suggestionsMenu.hide();
                return;
            }
            
            String prefix = newValue.trim().toLowerCase();
            if (prefix.length() < 1) {
                suggestionsMenu.hide();
                return;
            }
            
            // Query database for matching words in background
            new Thread(() -> {
                try {
                    com.example.backend.DatabaseManager db = com.example.backend.DatabaseManager.getInstance();
                    List<String> matches = db.getWordsByPrefix(prefix, 10);
                    
                    Platform.runLater(() -> {
                        suggestionsMenu.getItems().clear();
                        
                        if (matches.isEmpty()) {
                            suggestionsMenu.hide();
                            return;
                        }
                        
                        for (String word : matches) {
                            MenuItem item = new MenuItem(word);
                            item.setOnAction(e -> {
                                textField.setText(word);
                                textField.positionCaret(word.length());
                                suggestionsMenu.hide();
                            });
                            suggestionsMenu.getItems().add(item);
                        }
                        
                        if (!suggestionsMenu.isShowing()) {
                            suggestionsMenu.show(textField, 
                                javafx.stage.Screen.getPrimary().getVisualBounds().getMinX() + textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
        
        // Hide suggestions when field loses focus
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                suggestionsMenu.hide();
            }
        });
    }

    // Generate sentence using Greedy algorithm in background thread
    @FXML
    private void generateGreedy() {
        String startWord = startingWordField.getText().trim();
        
        if (startWord.isEmpty()) {
            showError("Input Required", "Please enter a starting word");
            return;
        }
        
        setButtonsDisabled(true);
        
        // Run generation in background thread to avoid UI freezing
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                GreedySentenceGenerator.SentenceResult result = greedyGenerator.generateSentence(startWord);
                long endTime = System.currentTimeMillis();
                
                // Update UI on JavaFX application thread
                Platform.runLater(() -> {
                    displayResult(result, endTime - startTime);
                    setButtonsDisabled(false);
                });
                
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    showError("Generation Error", "Failed to generate sentence: " + e.getMessage());
                    setButtonsDisabled(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Generate sentence using Beam Search algorithm in background thread
    @FXML
    private void generateBeamSearch() {
        String startWord = startingWordField.getText().trim();
        
        if (startWord.isEmpty()) {
            showError("Input Required", "Please enter a starting word");
            return;
        }
        
        setButtonsDisabled(true);
        
        // Run generation in background thread to avoid UI freezing
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                BeamSearchSentenceGenerator.SentenceResult result = beamSearchGenerator.generateSentence(startWord);
                long endTime = System.currentTimeMillis();
                
                // Update UI on JavaFX application thread
                Platform.runLater(() -> {
                    displayResult(result, endTime - startTime);
                    setButtonsDisabled(false);
                });
                
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    showError("Generation Error", "Failed to generate sentence: " + e.getMessage());
                    setButtonsDisabled(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Display generated sentence and statistics
    private void displayResult(Object resultObj, long generationTimeMs) {
        sentenceContainer.getChildren().clear();
        
        // Extract data from result object
        String sentence = null;
        String algorithm = "";
        int wordCount = 0;
        double score = 0.0;
        String metadata = "";
        
        if (resultObj instanceof GreedySentenceGenerator.SentenceResult) {
            GreedySentenceGenerator.SentenceResult result = (GreedySentenceGenerator.SentenceResult) resultObj;
            sentence = result.getSentence();
            algorithm = result.getAlgorithm();
            wordCount = result.getWordCount();
            score = result.getScore();
            metadata = result.getMetadata();
        } else if (resultObj instanceof BeamSearchSentenceGenerator.SentenceResult) {
            BeamSearchSentenceGenerator.SentenceResult result = (BeamSearchSentenceGenerator.SentenceResult) resultObj;
            sentence = result.getSentence();
            algorithm = result.getAlgorithm();
            wordCount = result.getWordCount();
            score = result.getScore();
            metadata = result.getMetadata();
        }
        
        if (sentence == null) {
            // Display generation failure message
            Label errorLabel = new Label("❌ " + metadata);
            errorLabel.setWrapText(true);
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");
            sentenceContainer.getChildren().add(errorLabel);
            
            algorithmLabel.setText("");
            wordCountLabel.setText("Words: 0");
            return;
        }
        
        // Create and style sentence label
        Label sentenceLabel = new Label("\"" + sentence + "\"");
        sentenceLabel.setWrapText(true);
        sentenceLabel.setStyle(
            "-fx-text-fill: #f1f5f9; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 10;"
        );
        sentenceContainer.getChildren().add(sentenceLabel);
        
        // Update statistics display
        String displayAlgorithm = algorithm.equals("Greedy") ? "Greedy Selection" : "Beam Search";
        String algorithmColor = algorithm.equals("Greedy") ? "#f59e0b" : "#3b82f6";
        algorithmLabel.setText("(Generated using " + displayAlgorithm + ")");
        algorithmLabel.setStyle("-fx-text-fill: " + algorithmColor + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        wordCountLabel.setText("Words: " + wordCount);
    }

    // Reset all fields and clear display
    @FXML
    private void reset() {
        startingWordField.clear();
        sentenceContainer.getChildren().clear();
        
        // Show initial prompt
        Label promptLabel = new Label("Click a generation button above to create a sentence...");
        promptLabel.setWrapText(true);
        promptLabel.setStyle(
            "-fx-text-fill: #94a3b8; " +
            "-fx-font-size: 14px; " +
            "-fx-font-style: italic;"
        );
        sentenceContainer.getChildren().add(promptLabel);
        
        algorithmLabel.setText("");
        wordCountLabel.setText("Words: 0");
    }

    // Navigate back to main menu
    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javafxproject/main-menu.fxml"));
            Parent mainMenuRoot = loader.load();
            
            Stage stage = (Stage) root.getScene().getWindow();
            Scene scene = new Scene(mainMenuRoot, 1400, 900);
            stage.setScene(scene);
            stage.setTitle("Text Generation System - Main Menu");
            
            System.out.println("✓ Navigated back to Main Menu");
            
        } catch (IOException e) {
            showError("Navigation Error", "Failed to return to main menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Setup hover effects for algorithm buttons with color transitions
    private void setupButtonHoverEffects() {
        greedyButton.setOnMouseEntered(e -> 
            greedyButton.setStyle(greedyButton.getStyle() + "-fx-background-color: #ea580c;")
        );
        greedyButton.setOnMouseExited(e -> 
            greedyButton.setStyle(greedyButton.getStyle().replace("-fx-background-color: #ea580c;", "-fx-background-color: #f59e0b;"))
        );
        
        beamSearchButton.setOnMouseEntered(e -> 
            beamSearchButton.setStyle(beamSearchButton.getStyle() + "-fx-background-color: #2563eb;")
        );
        beamSearchButton.setOnMouseExited(e -> 
            beamSearchButton.setStyle(beamSearchButton.getStyle().replace("-fx-background-color: #2563eb;", "-fx-background-color: #3b82f6;"))
        );
        
        resetButton.setOnMouseEntered(e -> 
            resetButton.setStyle(resetButton.getStyle() + "-fx-background-color: #475569;")
        );
        resetButton.setOnMouseExited(e -> 
            resetButton.setStyle(resetButton.getStyle().replace("-fx-background-color: #475569;", "-fx-background-color: #64748b;"))
        );
    }

    // Enable or disable all generation buttons
    private void setButtonsDisabled(boolean disabled) {
        greedyButton.setDisable(disabled);
        beamSearchButton.setDisable(disabled);
        resetButton.setDisable(disabled);
        startingWordField.setDisable(disabled);
    }

    // Show error alert dialog
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
