package com.example.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.sql.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;

/**
 * DatabaseConnection Class
 *
 * This class manages all database operations for the Text Generator AI application.
 * It provides connection management, file import functionality, text analysis,
 * and data persistence for words, word pairs, and autocomplete suggestions.
 *
 * Features include:
 * - MySQL database connection management
 * - Multi-format file support (TXT, DOCX, PDF)
 * - Advanced text analysis and word relationship tracking
 * - Autocomplete word database population
 * - Sentence structure analysis and word pair statistics
 *
 * Written by Arafat Khan and Colin Neary for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194, CRN190000
 *     Last modified: October 2, 2025
 */
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/"; // Put your database name
    private static final String USER = "root"; // Put your username
    private static final String PASSWORD = ""; // Put your database password
    private static boolean connectionShown = false;
    private Connection connectedSQL;

    private PreparedStatement stmtWord;
    private PreparedStatement stmtPair;


    public DatabaseConnection() throws SQLException {
        this.connectedSQL = getConnection();

        String sql_for_words = """
        INSERT INTO words (
            word_text, word_count_total, word_count_starting, word_count_ending
        ) VALUES (?, 1, ?, ?)
        ON DUPLICATE KEY UPDATE
            word_count_total = word_count_total + 1,
            word_count_starting = word_count_starting + VALUES(word_count_starting),
            word_count_ending = word_count_ending + VALUES(word_count_ending)
        """;

        this.stmtWord = connectedSQL.prepareStatement(
                """
                    INSERT INTO words (word_text, word_count_total, word_count_starting, word_count_ending) VALUES (?, 1, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        word_count_total = word_count_total + 1,
                        word_count_starting = word_count_starting + VALUES(word_count_starting),
                        word_count_ending = word_count_ending + VALUES(word_count_ending)
                    """);

        this.stmtPair = connectedSQL.prepareStatement(
                """
                    INSERT INTO word_pairs (left_word, right_word, pair_count) VALUES (?, ?, 1)
                    ON DUPLICATE KEY UPDATE
                        pair_count = pair_count + 1
                    """);
    }

    /**
     * Establishes connection to MySQL database
     * Shows success/error alerts and manages connection state
     *
     * @return Connection object or null if connection fails
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);

            // Show success popup only once
            if (!connectionShown) {
                System.out.println("✅ Connected to team33db successfully!");
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Database Connection");
                    alert.setHeaderText(null);
                    alert.setContentText("✅ Connected to team33db successfully!");
                    alert.showAndWait();
                });
                connectionShown = true;
            }

            return connection;

        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText("❌ Database Connection Failed!");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
            return null;
        }
    }

    // DatabaseHandler methods

    /**
     * Empties specified table (for testing/reset purposes)
     * Temporarily disables foreign key checks for truncation
     *
     * @param tableName Name of the table to empty
     * @throws SQLException If table operation fails
     */
    public void emptyTable(String tableName) throws SQLException {
        try {
            Statement stmt = connectedSQL.createStatement();
            String sql = "TRUNCATE TABLE " + tableName;

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate(sql);
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Table "+ tableName + " emptied successfully.");
        }
        catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Inserts file metadata into files table
     * Records file name, word count, import date, and notes
     *
     * @param fileName Name of the imported file
     * @param wordCount Total words in the file
     * @param notes User-provided notes about the file
     * @throws SQLException If insert operation fails
     */
    public void insertFileIntoFiles(String fileName, int wordCount, String notes) throws SQLException {
        try {
            PreparedStatement stmt = connectedSQL.prepareStatement(
                    "INSERT INTO files (file_name, word_count_total, date_imported, notes) VALUES (?, ?, ?, ?)");
            stmt.setString(1, fileName);
            stmt.setInt(2, wordCount);
            stmt.setString(3, LocalDate.now().toString());
            stmt.setString(4, notes);
            stmt.executeUpdate();
            System.out.println("✅ File record inserted: " + fileName);
        }
        catch (Exception ex) {
            System.out.println("Error inserting file: " + ex.getMessage());
        }
    }

    /**
     * Inserts or updates word in words table for word analysis
     * Tracks word frequency, sentence start/end positions
     *
     * @param word The word to insert/update
     * @param isStart Whether word appears at sentence start
     * @param isEnd Whether word appears at sentence end
     * @return word_id of the inserted/updated word
     * @throws SQLException If word operation fails
     */
    public boolean insertWordIntoWords(String word, boolean isStart, boolean isEnd) throws SQLException {
        try {
            stmtWord.setString(1, word);
            stmtWord.setInt(2, isStart ? 1 : 0);
            stmtWord.setInt(3, isEnd ? 1 : 0);
            stmtWord.executeUpdate();

            return true;
        } catch (SQLException ex) {
            System.err.println("Error inserting/updating word: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Inserts or updates word in autocomplete_words table
     * Maintains word frequency for auto-completion features
     *
     * @param word The word to add to autocomplete database
     * @throws SQLException If autocomplete operation fails
     */
    public void insertWordIntoAutoComplete(String word) throws SQLException {
        String cleanedWord = word.toLowerCase().trim();
        if (cleanedWord.isEmpty() || cleanedWord.length() < 1) return;

        // Check if word already exists in autocomplete_words
        try {
            PreparedStatement stmt = connectedSQL.prepareStatement(
                    "SELECT word_id, frequency FROM autocomplete_words WHERE word_text = ?");
            stmt.setString(1, cleanedWord);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Word exists, update frequency
                try {
                    PreparedStatement update = connectedSQL.prepareStatement(
                            "UPDATE autocomplete_words SET frequency = frequency + 1 WHERE word_id = ?");
                    update.setInt(1, rs.getInt("word_id"));
                    update.executeUpdate();
                    System.out.println("✅ Updated autocomplete word frequency: " + cleanedWord);
                }
                catch (Exception ex) {
                    System.out.println("Error updating autocomplete word: " + ex.getMessage());
                }
            } else {
                // Word doesn't exist, insert it
                try {
                    PreparedStatement stmt2 = connectedSQL.prepareStatement(
                            "INSERT INTO autocomplete_words (word_text, word_length, frequency) VALUES (?, ?, ?)");
                    stmt2.setString(1, cleanedWord);
                    stmt2.setInt(2, cleanedWord.length());
                    stmt2.setInt(3, 1);
                    stmt2.executeUpdate();
                    System.out.println("✅ Added new autocomplete word: " + cleanedWord);
                }
                catch (Exception ex) {
                    System.out.println("Error inserting autocomplete word: " + ex.getMessage());
                }
            }
        }
        catch (Exception ex) {
            System.out.println("Error checking autocomplete word: " + ex.getMessage());
        }
    }

    /**
     * Inserts or updates word pairs for relationship analysis
     * Tracks how often words appear together in sequence
     *
     * @param leftWord text of the first word in the pair
     * @param rightWord text of the second word in the pair
     * @throws SQLException If pair operation fails
     */
    public boolean insertPairIntoWordPairs(String leftWord, String rightWord) throws SQLException {
        try {
            stmtPair.setString(1, leftWord);
            stmtPair.setString(2, rightWord);
            stmtPair.executeUpdate();

            return true;
        } catch (SQLException ex) {
            System.err.println("Error inserting/updating pair: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Reads file content with improved PDF and DOCX handling
     * Supports multiple file formats with appropriate libraries
     *
     * @param filePath Path to the file to read
     * @return String containing file content
     * @throws IOException If file reading fails
     */
    private String fileToString(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        String fileName = file.getName().toLowerCase();

        try {
            if (fileName.endsWith(".txt")) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(" ");
                }
                reader.close();
            }
            else if (fileName.endsWith(".docx")) {
                // Use Apache POI for DOCX files
                try (FileInputStream fis = new FileInputStream(file);
                     org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis)) {

                    for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                        String text = p.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            sb.append(text).append(" ");
                        }
                    }
                }
            }
            else if (fileName.endsWith(".pdf")) {
                // Use PDFBox for PDF files
                try (org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(file)) {
                    org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                    stripper.setSortByPosition(true);
                    String text = stripper.getText(pdf);
                    sb.append(text).append(" ");
                } catch (Exception e) {
                    System.err.println("PDF extraction error: " + e.getMessage());
                    throw new IOException("Error reading PDF file: " + e.getMessage());
                }
            }
            else {
                throw new IOException("Unsupported file format: " + fileName);
            }
        } catch(IOException ex) {
            System.out.println("Error reading file: " + ex.getMessage());
            throw ex;
        }
        return sb.toString();
    }

    /**
     * Main import function - integrates both approaches with improved file reading
     * Performs comprehensive text analysis and database population
     *
     * @param filePath Path to the file to import
     * @param notes User notes about the file
     * @return boolean indicating import success
     */
    public boolean importFileIntoSchema(String filePath, String notes) {
        try {
            String text = fileToString(filePath);

            // Improved sentence splitting that handles various file formats better
            List<String> sentences = new ArrayList<>(Arrays.asList(
                    text.split("(?i)(?<!mr|mrs|ms|dr|esq|fr|br|sr|pr|prof|rev|hon|capt|lt|col|gen|sgt)([.!?]+)(?-i)(?=$|[\\s]+[A-Z])")));

            int fileWordCount = 0;
            Set<String> uniqueWords = new HashSet<>(); // Track unique words for autocomplete

            // Process each sentence for word analysis
            for (int s = 0; s < sentences.size(); s++) {
                sentences.set(s, sentences.get(s).trim());
                if (sentences.get(s).isEmpty()) continue;

                // Improved word splitting that handles various punctuation
                String[] words = sentences.get(s).split("[^a-zA-Z0-9']+"); // ERROR: abbreviations will fail here (e.g. U.S.A.)
                String prevWord = null;

                for (int w = 0; w < words.length; w++) {
                    if (words[w].isEmpty() || words[w].length() < 1) continue;

                    String currentWord = words[w].toLowerCase();
                    fileWordCount++;

                    // Add to unique words set for autocomplete
                    uniqueWords.add(currentWord);

                    boolean isStart = w == 0;
                    boolean isEnd = w == words.length - 1;

                    insertWordIntoWords(currentWord, isStart, isEnd);
                    if (prevWord != null) {
                        insertPairIntoWordPairs(prevWord, currentWord);
                    }
                    prevWord = currentWord;
                }
            }

            // Insert all unique words into autocomplete_words table
            System.out.println("🔄 Adding " + uniqueWords.size() + " unique words to autocomplete database...");
            for (String word : uniqueWords) {
                insertWordIntoAutoComplete(word);
            }

            System.out.println("✅ Word analysis completed for: " + filePath);
            System.out.println("✅ Added " + uniqueWords.size() + " words to autocomplete database");

            // Insert file record
            File file = new File(filePath);
            String fileName = file.getName();
            insertFileIntoFiles(fileName, fileWordCount, notes);

            System.out.println("✅ File imported successfully: " + fileName);
            return true;

        } catch (Exception ex) {
            System.err.println("❌ Error importing file: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    // Get connection for other uses
    public Connection getConnectedSQL() {
        return connectedSQL;
    }

    /**
     * Simple query for testing database operations
     * Executes SELECT queries with sorting and limiting
     *
     * @param tableName Table to query from
     * @param sortByColumnName Column to sort by
     * @param sortASC Sort ascending if true, descending if false
     * @param limit Number of results to return
     * @throws SQLException If query execution fails
     */
    public void simpleQuery(String tableName, String sortByColumnName, Boolean sortASC, int limit) throws SQLException {
        try {
            Statement stmt = connectedSQL.createStatement();

            String stmtText;
            if (sortASC) {
                stmtText = "SELECT * FROM " + tableName + " ORDER BY " + sortByColumnName + " ASC LIMIT " + limit;
            } else {
                stmtText = "SELECT * FROM " + tableName + " ORDER BY " + sortByColumnName + " DESC LIMIT " + limit;
            }

            System.out.println("Results:");
            ResultSet rs = stmt.executeQuery(stmtText);
            while (rs.next()) {
                for (int i = 1; i < rs.getMetaData().getColumnCount(); i++) {
                    System.out.print(rs.getString(i) + " ");
                }
                System.out.println();
            }
        }
        catch (Exception ex) {
            System.out.println("Error in query: " + ex.getMessage());
        }
    }

    /**
     * Clean up orphaned words from autocomplete database
     * Removes words that no longer exist in file_words table
     *
     * @return Number of orphaned words deleted
     * @throws SQLException If cleanup operation fails
     */
    public int cleanupOrphanedWords() throws SQLException {
        Connection conn = getConnectedSQL();
        if (conn == null) return 0;

        String deleteOrphanedSQL = "DELETE FROM autocomplete_words WHERE word_text NOT IN (SELECT DISTINCT word_text FROM file_words)";
        try (Statement stmt = conn.createStatement()) {
            int orphanedDeleted = stmt.executeUpdate(deleteOrphanedSQL);
            if (orphanedDeleted > 0) {
                System.out.println("🧹 Cleaned up " + orphanedDeleted + " orphaned words");
            } else {
                System.out.println("ℹ️ No orphaned words to delete");
            }
            return orphanedDeleted;
        } catch (SQLException e) {
            System.err.println("❌ Error cleaning up orphaned words: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}