package com.example.javafxproject;

import com.example.util.DatabaseConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.*;

/**
 * FileImportService Class
 *
 * This service class handles all file import operations and database interactions
 * for the Text Generator AI application. It provides methods for importing files
 * with advanced text analysis, retrieving imported files, and managing file data.
 *
 * The service layer separates business logic from UI controllers and provides
 * reusable methods for file operations across the application.
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: October 12, 2025
 */
public class FileImportService {

    /**
     * Advanced file import with comprehensive text analysis
     * Processes file content and populates multiple database tables
     *
     * @param filePath Path to the file to import
     * @param notes User-provided notes about the file
     * @return boolean indicating import success
     */
    public static boolean importFileWithAnalysis(String filePath, String notes) {
        try {
            DatabaseConnection dbHandler = new DatabaseConnection();
            File file = new File(filePath);
            String fileName = file.getName().toLowerCase();

            // Only continue with line-by-line parsing for .txt files, since only they are likely to be very large
            if (!fileName.endsWith(".txt")) {
                return dbHandler.importFileIntoSchema(filePath, notes);
            }

            int fileWordCount = 0; // Track this for "files" table in db
            Set<String> uniqueWords = new HashSet<>(); // Track unique words for autocomplete
            String prevWord = null; // Used to track word-pairs between lines of text

            // Iterate over text file line-by-line
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse this line of text into sentences, if possible. Need to do this to track words that begin or end a sentence
                List<String> sentences = new ArrayList<>(Arrays.asList(
                        line.split("(?i)(?<!mr|mrs|ms|dr|esq|fr|br|sr|pr|prof|rev|hon|capt|lt|col|gen|sgt)([.!?]+)(?-i)(?=$|[\\s]+[A-Z])")));

                // Iterate over sentences to find words. The words are ultimately what we will import into the db
                for (int s_i = 0; s_i < sentences.size(); s_i++) {
                    sentences.set(s_i, sentences.get(s_i).trim());
                    if (sentences.get(s_i).isEmpty()) continue;

                    // Parse this sentence into words
                    String[] words = sentences.get(s_i).split("[^a-zA-Z0-9']+"); // ERROR: abbreviations will fail here (e.g. U.S.A.)

                    // Iterate over words in this sentence
                    for (int w_i = 0; w_i < words.length; w_i++) {
                        if (words[w_i].isEmpty() || words[w_i].length() < 1) continue;

                        String currentWord = words[w_i].toLowerCase();
                        uniqueWords.add(currentWord);
                        fileWordCount++;

                        boolean isStart = w_i == 0;
                        boolean isEnd = w_i == words.length - 1;

                        // Insert word into words table
                        // Insert pair into pairs table
                        dbHandler.insertWordIntoWords(currentWord, isStart, isEnd);
                        if (prevWord != null) {
                            dbHandler.insertPairIntoWordPairs(prevWord, currentWord);
                        }
                        prevWord = currentWord;
                    }
                }
            }
            reader.close();

            // Insert all unique words into autocomplete_words table
            System.out.println("🔄 Adding " + uniqueWords.size() + " unique words to autocomplete database...");
            for (String word : uniqueWords) {
                dbHandler.insertWordIntoAutoComplete(word);
            }
            System.out.println("✅ Word analysis completed for: " + filePath);
            System.out.println("✅ Added " + uniqueWords.size() + " words to autocomplete database");

            // Insert file into files table
            dbHandler.insertFileIntoFiles(fileName, fileWordCount, notes);
            System.out.println("✅ File imported successfully: " + fileName);

            return true;
        } catch (Exception e) {
            System.err.println("❌ Error in advanced file import: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all imported files from database
     * Returns files sorted by import date (newest first)
     *
     * @return List of ImportedFile objects containing file metadata
     */
    public static List<ImportFileController.ImportedFile> getAllImportedFiles() {
        List<ImportFileController.ImportedFile> files = new ArrayList<>();
        String sql = "SELECT file_name, word_count_total, date_imported FROM files ORDER BY date_imported DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return files;
            }

            while (rs.next()) {
                String fileName = rs.getString("file_name");
                int wordCount = rs.getInt("word_count_total");
                String importDate = rs.getString("date_imported");

                files.add(new ImportFileController.ImportedFile(fileName, wordCount, importDate));
            }

            System.out.println("✅ Loaded " + files.size() + " files from database");

        } catch (SQLException e) {
            System.err.println("❌ Error loading files from database: " + e.getMessage());
            e.printStackTrace();
        }

        return files;
    }

    /**
     * Deletes a file record from the database
     *
     * @param fileName Name of the file to delete
     * @return boolean indicating deletion success
     */
    public static boolean deleteFile(String fileName) {
        String sql = "DELETE FROM files WHERE file_name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return false;
            }

            pstmt.setString(1, fileName);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting file from database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves word statistics from database
     * Returns words matching pattern with frequency data
     *
     * @param word Word or word pattern to search for
     * @return List of WordStat objects with statistical data
     */
    public static List<WordStat> getWordStatistics(String word) {
        List<WordStat> stats = new ArrayList<>();
        String sql = "SELECT word_text, word_count_total, word_count_starting, word_count_ending " +
                "FROM words WHERE word_text LIKE ? ORDER BY word_count_total DESC LIMIT 10";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                return stats;
            }

            pstmt.setString(1, word + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                stats.add(new WordStat(
                        rs.getString("word_text"),
                        rs.getInt("word_count_total"),
                        rs.getInt("word_count_starting"),
                        rs.getInt("word_count_ending")
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting word statistics: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    // Word statistics data model
    /**
     * WordStat Data Model Class
     *
     * Represents word statistical data including frequency counts
     * and positional information (start/end of sentences).
     */
    public static class WordStat {
        private final String word;
        private final int totalCount;
        private final int startingCount;
        private final int endingCount;

        public WordStat(String word, int totalCount, int startingCount, int endingCount) {
            this.word = word;
            this.totalCount = totalCount;
            this.startingCount = startingCount;
            this.endingCount = endingCount;
        }

        public String getWord() { return word; }
        public int getTotalCount() { return totalCount; }
        public int getStartingCount() { return startingCount; }
        public int getEndingCount() { return endingCount; }
    }
}