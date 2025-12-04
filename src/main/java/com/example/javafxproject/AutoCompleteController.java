// Written by Raghav Mukundan, rxm210068

package com.example.javafxproject;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.example.backend.autocomplete.GreedyAutocomplete;
import com.example.backend.autocomplete.MarkovAutocomplete;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for interactive word-by-word sentence building with autocomplete algorithms
 */
public class AutoCompleteController {

    // FXML Injected Components
    @FXML private VBox root;
    @FXML private TextField startingWordField;
    @FXML private Button greedyButton;
    @FXML private Button markovButton;
    @FXML private Button finishButton;
    @FXML private Label currentSentenceLabel;
    @FXML private Label wordCountLabel;
    @FXML private Button resetButton;
    @FXML private VBox suggestionsContainer;

    // Backend algorithm instances
    private GreedyAutocomplete greedyAutocomplete;
    private MarkovAutocomplete markovAutocomplete;
    
    // State tracking
    private List<String> currentSentence;
    private String currentAlgorithm;

    // Initialize controller after FXML loading
    @FXML
    private void initialize() {
        // Load CSS for consistent styling
        String css = getClass().getResource("/com/example/javafxproject/styles.css").toExternalForm();
        root.getStylesheets().add(css);

        // Initialize autocomplete algorithms in background thread
        new Thread(() -> {
            try {
                greedyAutocomplete = new GreedyAutocomplete();
                markovAutocomplete = new MarkovAutocomplete();
                System.out.println("✅ Autocomplete algorithms initialized");
            } catch (Exception e) {
                System.err.println("❌ Error initializing algorithms: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Initialization Error", 
                             "Failed to initialize autocomplete algorithms: " + e.getMessage());
                });
            }
        }).start();
        
        currentSentence = new ArrayList<>();
        setupWordAutocomplete(startingWordField);
        
        System.out.println("🔧 AutoCompleteController initialized");
    }
    
    // Setup autocomplete dropdown for text field with database word suggestions
    private void setupWordAutocomplete(TextField textField) {
        ContextMenu suggestionsMenu = new ContextMenu();
        
        // Listen for text changes and query database for matches
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
            
            // Query database for matching words in background thread
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

    // Start sentence building with Greedy algorithm
    @FXML
    private void startGreedy(javafx.event.ActionEvent event) {
        String startWord = startingWordField.getText().trim().toLowerCase();
        
        if (startWord.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Starting Word", 
                     "Please enter a starting word first.");
            return;
        }
        
        if (greedyAutocomplete == null) {
            showAlert(Alert.AlertType.WARNING, "Not Ready", 
                     "Autocomplete algorithms are still initializing. Please wait a moment.");
            return;
        }
        
        currentAlgorithm = "greedy";
        currentSentence.clear();
        currentSentence.add(startWord);
        updateSentenceDisplay();
        generateSuggestions();
        
        System.out.println("🎯 Started Greedy algorithm with word: " + startWord);
    }

    // Start sentence building with Markov algorithm
    @FXML
    private void startMarkov(javafx.event.ActionEvent event) {
        String startWord = startingWordField.getText().trim().toLowerCase();
        
        if (startWord.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Starting Word", 
                     "Please enter a starting word first.");
            return;
        }
        
        if (markovAutocomplete == null) {
            showAlert(Alert.AlertType.WARNING, "Not Ready", 
                     "Autocomplete algorithms are still initializing. Please wait a moment.");
            return;
        }
        
        currentAlgorithm = "markov";
        currentSentence.clear();
        currentSentence.add(startWord);
        updateSentenceDisplay();
        generateSuggestions();
        
        System.out.println("� Started Markov algorithm with word: " + startWord);
    }

    /**
     * Resets the current sentence
     */
    @FXML
    private void resetSentence(javafx.event.ActionEvent event) {
        currentSentence.clear();
        currentAlgorithm = null;
        currentSentenceLabel.setText("Your sentence will appear here...");
        wordCountLabel.setText("Words: 0");
        clearSuggestions();
        suggestionsContainer.getChildren().clear();
        
        Label instructionLabel = new Label("Click an algorithm button above to start building your sentence...");
        instructionLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-padding: 8;");
        suggestionsContainer.getChildren().add(instructionLabel);
        
        System.out.println("🔄 Sentence reset");
    }

    // Finish the current sentence and show completion summary
    @FXML
    private void finishSentence(javafx.event.ActionEvent event) {
        if (currentSentence.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Sentence", 
                     "Please build a sentence first before finishing.");
            return;
        }
        
        String finalSentence = String.join(" ", currentSentence);
        
        // Clear suggestions and show completion message
        clearSuggestions();
        suggestionsContainer.getChildren().clear();
        
        VBox completionBox = new VBox(10);
        completionBox.setStyle("-fx-padding: 15; -fx-alignment: center;");
        
        Label completionLabel = new Label("✅ Sentence Complete!");
        completionLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label sentenceLabel = new Label("\"" + finalSentence + "\"");
        sentenceLabel.setWrapText(true);
        sentenceLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 16px; -fx-font-style: italic;");
        
        Label wordCountInfo = new Label("Total words: " + currentSentence.size());
        wordCountInfo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        
        Label algorithmInfo = new Label("Algorithm used: " + 
                                        (currentAlgorithm != null ? currentAlgorithm.toUpperCase() : "N/A"));
        algorithmInfo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        
        completionBox.getChildren().addAll(completionLabel, sentenceLabel, wordCountInfo, algorithmInfo);
        suggestionsContainer.getChildren().add(completionBox);
        
        System.out.println("✅ Sentence finished: " + finalSentence);
        System.out.println("   Algorithm: " + currentAlgorithm + ", Words: " + currentSentence.size());
    }

    // Update the sentence display and word count
    private void updateSentenceDisplay() {
        String sentence = String.join(" ", currentSentence);
        currentSentenceLabel.setText(sentence);
        wordCountLabel.setText("Words: " + currentSentence.size());
    }

    // Generate word suggestions based on last word in sentence
    private void generateSuggestions() {
        if (currentSentence.isEmpty() || currentAlgorithm == null) {
            return;
        }
        
        String lastWord = currentSentence.get(currentSentence.size() - 1);
        
        new Thread(() -> {
            try {
                List<String> suggestions = new ArrayList<>();
                
                if ("greedy".equals(currentAlgorithm)) {
                    List<GreedyAutocomplete.Suggestion> greedySuggestions = greedyAutocomplete.getSuggestions(lastWord);
                    for (GreedyAutocomplete.Suggestion s : greedySuggestions) {
                        suggestions.add(s.getWord());
                    }
                } else {
                    List<MarkovAutocomplete.Suggestion> markovSuggestions = markovAutocomplete.getSuggestions(lastWord);
                    for (MarkovAutocomplete.Suggestion s : markovSuggestions) {
                        suggestions.add(s.getWord());
                    }
                }
                
                Platform.runLater(() -> displaySuggestions(suggestions));
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("❌ Error generating suggestions: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", 
                             "Failed to generate suggestions: " + e.getMessage());
                });
            }
        }).start();
    }

    // Display word suggestions as interactive clickable items
    private void displaySuggestions(List<String> suggestions) {
        suggestionsContainer.getChildren().clear();

        if (suggestions.isEmpty()) {
            Label noSuggestions = new Label("No more suggestions available. Your sentence is complete!");
            noSuggestions.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-style: italic;");
            suggestionsContainer.getChildren().add(noSuggestions);
            System.out.println("❌ No suggestions to display");
            return;
        }

        // Create clickable suggestion items
        for (String suggestion : suggestions) {
            HBox suggestionItem = createSuggestionItem(suggestion);
            suggestionsContainer.getChildren().add(suggestionItem);
        }

        System.out.println("✅ Displayed " + suggestions.size() + " suggestions");
    }

    // Create interactive suggestion item with hover effects
    private HBox createSuggestionItem(String suggestion) {
        HBox container = new HBox();
        container.setSpacing(8);
        container.setStyle("-fx-background-color: #334155; -fx-background-radius: 6; -fx-padding: 10 14; -fx-cursor: hand;");
        container.setOnMouseClicked(e -> applySuggestion(suggestion));

        Label textLabel = new Label(suggestion);
        textLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");

        container.getChildren().add(textLabel);

        // Add hover effects for algorithm-specific colors
        container.setOnMouseEntered(e -> {
            String hoverColor = "greedy".equals(currentAlgorithm) ? "#f59e0b" : "#3b82f6";
            container.setStyle("-fx-background-color: " + hoverColor + "; -fx-background-radius: 6; -fx-padding: 10 14; -fx-cursor: hand;");
            textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        container.setOnMouseExited(e -> {
            container.setStyle("-fx-background-color: #334155; -fx-background-radius: 6; -fx-padding: 10 14; -fx-cursor: hand;");
            textLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        return container;
    }

    // Apply selected suggestion and add to sentence
    private void applySuggestion(String suggestion) {
        currentSentence.add(suggestion);
        updateSentenceDisplay();
        generateSuggestions();
        
        System.out.println("✅ Applied suggestion: " + suggestion);
        System.out.println("📝 Current sentence: " + String.join(" ", currentSentence));
    }

    // Clear all suggestions from container
    private void clearSuggestions() {
        suggestionsContainer.getChildren().clear();
    }

    // Navigate back to main menu
    @FXML
    private void goBack(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javafxproject/main-menu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 750);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);

            // Smooth fade transition for better user experience
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to main menu.");
        }
    }

    // Show alert dialog with custom styling
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/javafxproject/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}