package com.example.javafxproject;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * ImportFileController Class
 *
 * This controller manages the File Import Manager interface functionality.
 * It handles file selection, import operations, database interactions, and
 * table display of imported files. Supports multiple file formats including
 * TXT, DOCX, and PDF with advanced text extraction and analysis.
 *
 * Features include:
 * - File chooser integration with format validation
 * - Progress indicators for import operations
 * - Dynamic table updates with action buttons
 * - Database integration for file metadata storage
 * - Navigation between application screens
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: October 8, 2025
 */
public class ImportFileController {

    // FXML injected components
    @FXML private VBox root;
    @FXML private Button chooseFileButton;
    @FXML private Label selectedFileLabel;
    @FXML private Button importButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private VBox progressContainer;
    @FXML private TableView<ImportedFile> importedFilesTable;
    @FXML private TableColumn<ImportedFile, Integer> serialNumberColumn;
    @FXML private TableColumn<ImportedFile, String> fileNameColumn;
    @FXML private TableColumn<ImportedFile, Integer> wordCountColumn;
    @FXML private TableColumn<ImportedFile, String> importDateColumn;
    @FXML private TableColumn<ImportedFile, String> actionsColumn;
    @FXML private Label fileCountLabel;

    private File selectedFile;
    private final ObservableList<ImportedFile> importedFiles = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Initializes the controller after FXML loading
     * Sets up table columns, loads existing files, and applies styling
     */
    @FXML
    private void initialize() {
        // ✅ Load external CSS for consistent styling
        String css = getClass().getResource("/com/example/javafxproject/styles.css").toExternalForm();
        root.getStylesheets().add(css);

        setupTableColumns();
        loadFilesFromDatabase();

        // Update file count dynamically when list changes
        importedFiles.addListener((javafx.collections.ListChangeListener.Change<? extends ImportedFile> change) -> updateFileCount());
        updateFileCount();

        // Initially disable import button until file is selected
        importButton.setDisable(true);
        
        // Hide progress bar initially
        progressBar.setVisible(false);
        progressLabel.setVisible(false);
        progressContainer.setVisible(false);
    }

    /**
     * Configures table columns with cell value factories and custom rendering
     * Sets up serial numbers, file data display, and action buttons
     */
    private void setupTableColumns() {
        // Table header setup
        serialNumberColumn.setText("#");
        fileNameColumn.setText("File Name");
        wordCountColumn.setText("Word Count");
        importDateColumn.setText("Import Date");
        actionsColumn.setText("Actions");

        // Serial number generator - dynamic index based on table position
        serialNumberColumn.setCellValueFactory(cellData -> {
            int index = importedFilesTable.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });

        // Bind columns to ImportedFile properties
        fileNameColumn.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        wordCountColumn.setCellValueFactory(cellData -> cellData.getValue().wordCountProperty().asObject());
        importDateColumn.setCellValueFactory(cellData -> cellData.getValue().importDateProperty());

        // Actions column with "View" button
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                // Style view button with blue theme
                viewButton.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;" +
                        "-fx-background-color: #3b82f6; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
                viewButton.setOnAction(event -> {
                    ImportedFile file = getTableView().getItems().get(getIndex());
                    showFileDetails(file);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        // Align columns for better visual presentation
        serialNumberColumn.setStyle("-fx-alignment: CENTER;");
        wordCountColumn.setStyle("-fx-alignment: CENTER;");
        importDateColumn.setStyle("-fx-alignment: CENTER;");
        actionsColumn.setStyle("-fx-alignment: CENTER;");

        importedFilesTable.setItems(importedFiles);
    }

    /**
     * Loads imported files from database asynchronously
     * Updates the table view with retrieved file data
     */
    private void loadFilesFromDatabase() {
        new Thread(() -> {
            List<ImportedFile> files = FileImportService.getAllImportedFiles();
            Platform.runLater(() -> {
                importedFiles.setAll(files);
                updateFileCount();
                System.out.println("✅ Loaded " + files.size() + " files from database");
            });
        }).start();
    }

    /**
     * Updates the file count label based on current imported files
     * Handles singular/plural formatting automatically
     */
    private void updateFileCount() {
        int count = importedFiles.size();
        fileCountLabel.setText("(" + count + " " + (count == 1 ? "file" : "files") + " imported)");
    }

    /**
     * Handles file import operation with advanced analysis
     * Shows progress indicator and processes file asynchronously
     */
    @FXML
    protected void importFile() {
        if (selectedFile == null) return;

        importButton.setDisable(true);
        chooseFileButton.setDisable(true);
        progressIndicator.setVisible(true);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressContainer.setVisible(true);
        progressBar.setProgress(0);

        // Ask for notes about the file
        TextInputDialog dialog = new TextInputDialog("Imported via JavaFX Application");
        dialog.setTitle("File Import Notes");
        dialog.setHeaderText("Add notes for this file (optional):");
        dialog.setContentText("Notes:");

        Optional<String> notesResult = dialog.showAndWait();
        String notes = notesResult.orElse("Imported via JavaFX Application");

        new Thread(() -> {
            try {
                // Stage 1: Initializing (0-10%)
                updateProgress(5, "Initializing import...");
                Thread.sleep(300);

                // Stage 2: Reading file (10-30%)
                updateProgress(15, "Reading file content...");
                String fileContent = extractTextFromFile(selectedFile);
                Thread.sleep(400);

                // Stage 3: Analyzing text (30-50%)
                updateProgress(35, "Analyzing text structure...");
                Thread.sleep(500);

                // Stage 4: Processing word relationships (50-70%)
                updateProgress(55, "Processing word relationships...");
                Thread.sleep(600);

                // Stage 5: Database operations (70-90%)
                updateProgress(75, "Saving to database...");

                // Use advanced import with text analysis
                boolean saveSuccess = FileImportService.importFileWithAnalysis(
                        selectedFile.getAbsolutePath(), notes);

                // Stage 6: Finalizing (90-100%)
                updateProgress(95, "Finalizing import...");
                Thread.sleep(300);
                updateProgress(100, "Import complete!");

                // Small delay to show 100%
                Thread.sleep(500);

                Platform.runLater(() -> {
                    if (saveSuccess) {
                        // Reload files from database to show the new file
                        loadFilesFromDatabase();

                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "File imported with advanced analysis successfully!\n" +
                                        "✅ Word relationships analyzed\n" +
                                        "✅ Sentence structure processed\n" +
                                        "✅ Word pairs stored in database\n" +
                                        "✅ Auto-complete database populated!");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error",
                                "Failed to import file with analysis!");
                    }

                    progressIndicator.setVisible(false);
                    progressBar.setVisible(false);
                    progressLabel.setVisible(false);
                    progressContainer.setVisible(false);
                    selectedFileLabel.setText("No file selected");
                    selectedFile = null;
                    importButton.setDisable(true);
                    chooseFileButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    progressBar.setVisible(false);
                    progressLabel.setVisible(false);
                    progressContainer.setVisible(false);
                    importButton.setDisable(false);
                    chooseFileButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to import file: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Opens file chooser dialog for file selection
     * Validates file format and updates UI accordingly
     */
    @FXML
    protected void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Document File");

        // Accept all lowercase and uppercase formats
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Files (*.txt, *.docx, *.pdf)", "*.txt", "*.TXT", "*.docx", "*.DOCX", "*.pdf", "*.PDF")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null && file.exists()) {
            String fileName = file.getName().toLowerCase();

            if (!(fileName.endsWith(".txt") || fileName.endsWith(".docx") || fileName.endsWith(".pdf"))) {
                showAlert(Alert.AlertType.ERROR, "Invalid File", "Please select a valid .txt, .docx, or .pdf file.");
                selectedFile = null;
                selectedFileLabel.setText("No file selected");
                importButton.setDisable(true);
                return;
            }

            selectedFile = file;
            selectedFileLabel.setText(file.getName());
            importButton.setDisable(false);
        } else {
            selectedFileLabel.setText("No file selected");
            importButton.setDisable(true);
        }
    }

    /**
     * Extracts text content from various file formats
     *
     * @param file The file to extract text from
     * @return String containing extracted text
     * @throws IOException If file reading fails
     */
    private String extractTextFromFile(File file) throws IOException {
        String text = "";
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".txt")) {
            text = java.nio.file.Files.readString(file.toPath());
        }
        else if (fileName.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(file);
                 XWPFDocument doc = new XWPFDocument(fis)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    String paragraphText = p.getText();
                    if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                        sb.append(paragraphText).append(" ");
                    }
                }
                text = sb.toString();
            }
        }
        else if (fileName.endsWith(".pdf")) {
            try (PDDocument pdf = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                text = stripper.getText(pdf);
            } catch (Exception e) {
                System.err.println("PDF extraction error: " + e.getMessage());
                throw new IOException("Error reading PDF file: " + e.getMessage());
            }
        }

        return text;
    }

    /**
     * Shows detailed information about a specific file
     *
     * @param file The file to display details for
     */
    private void showFileDetails(ImportedFile file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File Details");
        alert.setHeaderText("📄 File Information");
        alert.setContentText("File Name: " + file.getFileName() + "\n" +
                "Word Count: " + file.getWordCount() + "\n" +
                "Import Date: " + file.getImportDate());

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/javafxproject/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }

    /**
     * Updates progress bar and label during file import
     *
     * @param progress Progress percentage (0-100)
     * @param message Status message to display
     */
    private void updateProgress(int progress, String message) {
        Platform.runLater(() -> {
            double progressValue = progress / 100.0;
            progressBar.setProgress(progressValue);
            progressLabel.setText(message + " (" + progress + "%)");
            progressIndicator.setProgress(progressValue);
        });
    }

    /**
     * Utility method to show alert dialogs with consistent styling
     *
     * @param type Type of alert (INFO, ERROR, WARNING, etc.)
     * @param title Alert window title
     * @param message Alert content message
     */
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

    /**
     * Navigates back to main menu with fade transition
     *
     * @param event The action event that triggered navigation
     */
    @FXML
    protected void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javafxproject/main-menu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 750);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);

            // Smooth fade transition for better UX
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to main menu.");
        }
    }

    // 📦 Inner class: ImportedFile model
    /**
     * ImportedFile Data Model Class
     *
     * Represents imported file data with JavaFX properties for UI binding.
     * Used within the controller for table display and data management.
     */
    public static class ImportedFile {
        private final SimpleStringProperty fileName;
        private final SimpleIntegerProperty wordCount;
        private final SimpleStringProperty importDate;

        public ImportedFile(String fileName, int wordCount, String importDate) {
            this.fileName = new SimpleStringProperty(fileName);
            this.wordCount = new SimpleIntegerProperty(wordCount);
            this.importDate = new SimpleStringProperty(importDate);
        }

        public String getFileName() { return fileName.get(); }
        public int getWordCount() { return wordCount.get(); }
        public String getImportDate() { return importDate.get(); }

        public SimpleStringProperty fileNameProperty() { return fileName; }
        public SimpleIntegerProperty wordCountProperty() { return wordCount; }
        public SimpleStringProperty importDateProperty() { return importDate; }
    }
}