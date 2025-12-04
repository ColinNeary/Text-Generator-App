package com.example.javafxproject;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * ImportedFile Class
 *
 * This class represents the data model for imported files in the application.
 * It uses JavaFX properties to enable data binding with TableView and other UI components.
 * Each imported file contains file name, word count, and import date information.
 *
 * The class follows the JavaFX property pattern for seamless integration with
 * TableView column bindings and observable collections.
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: October 5, 2025
 */
public class ImportedFile {
    private final SimpleStringProperty fileName = new SimpleStringProperty();
    private final SimpleIntegerProperty wordCount = new SimpleIntegerProperty();
    private final SimpleStringProperty importDate = new SimpleStringProperty();

    /**
     * Constructor for ImportedFile data model
     *
     * @param fileName Name of the imported file
     * @param wordCount Total word count in the file
     * @param importDate Date when the file was imported
     */
    public ImportedFile(String fileName, int wordCount, String importDate) {
        this.fileName.set(fileName);
        this.wordCount.set(wordCount);
        this.importDate.set(importDate);
    }

    // Property getters for JavaFX data binding

    public SimpleStringProperty fileNameProperty() {
        return fileName;
    }

    public SimpleIntegerProperty wordCountProperty() {
        return wordCount;
    }

    public int getWordCount() {
        return wordCount.get();
    }

    public SimpleStringProperty importDateProperty() {
        return importDate;
    }
}