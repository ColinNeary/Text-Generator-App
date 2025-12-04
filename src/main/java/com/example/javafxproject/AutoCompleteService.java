package com.example.javafxproject;

import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AutoCompleteService Class
 *
 * This service class handles all database operations for the Auto-Complete feature
 * in the Text Generator AI application. It provides methods for word storage,
 * suggestion retrieval, and database management for the auto-complete functionality.
 *
 * The service manages a dedicated database table for word frequency data and implements
 * efficient querying algorithms for real-time suggestion generation based on word prefixes.
 *
 * Written by Arafat Khan for CS4485.OW1, Senior Project, starting September 13, 2025.
 *     NetID: AXK220194
 *     Last modified: November 5, 2025
 */
public class AutoCompleteService {

    /**
     * Creates the autocomplete_words table if it doesn't exist
     * Table structure includes word text, length, frequency, and creation timestamp
     *
     * @return boolean indicating table creation success
     */
    public static boolean createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS autocomplete_words (" +
                "word_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "word_text VARCHAR(100) NOT NULL UNIQUE, " +
                "word_length INT NOT NULL, " +
                "frequency INT NOT NULL DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return false;
            }

            stmt.executeUpdate(sql);
            System.out.println("✅ Auto-complete table created/verified successfully");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error creating table: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts a single word into the autocomplete database
     * Uses UPSERT pattern to update frequency if word already exists
     *
     * @param word The word to insert into the database
     * @return boolean indicating insertion success
     */
    public static boolean insertWord(String word) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        String sql = "INSERT INTO autocomplete_words (word_text, word_length, frequency) VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return false;
            }

            pstmt.setString(1, word.toLowerCase().trim());
            pstmt.setInt(2, word.length());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error inserting word: " + e.getMessage());
            return false;
        }
    }

    /**
     * Bulk inserts multiple words into the autocomplete database
     * Uses batch processing for efficiency with large word lists
     *
     * @param words List of words to insert into the database
     * @return boolean indicating bulk insertion success
     */
    public static boolean insertWords(List<String> words) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        String sql = "INSERT INTO autocomplete_words (word_text, word_length, frequency) VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return false;
            }

            int batchCount = 0;
            int validWordCount = 0;

            // Process each word and add to batch
            for (String word : words) {
                String cleanWord = word.toLowerCase().trim();
                // Only insert valid words with 2+ letters and alphabetic characters
                if (cleanWord.length() > 1 && cleanWord.matches("[a-zA-Z]+")) {
                    pstmt.setString(1, cleanWord);
                    pstmt.setInt(2, cleanWord.length());
                    pstmt.addBatch();
                    batchCount++;
                    validWordCount++;

                    // Execute batch every 1000 words to avoid memory issues
                    if (batchCount % 1000 == 0) {
                        pstmt.executeBatch();
                        System.out.println("✅ Processed " + batchCount + " words...");
                    }
                }
            }

            // Execute remaining batch entries
            if (batchCount % 1000 != 0) {
                pstmt.executeBatch();
            }

            System.out.println("✅ Successfully inserted " + validWordCount + " valid words into database");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error inserting words: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves word suggestions from database based on prefix matching
     * Orders results by frequency (most common words first) then alphabetically
     *
     * @param prefix The word prefix to search for
     * @param limit Maximum number of suggestions to return
     * @return List of suggested words matching the prefix
     */
    public static List<String> getSuggestions(String prefix, int limit) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        List<String> suggestions = new ArrayList<>();
        String sql = "SELECT word_text FROM autocomplete_words " +
                "WHERE word_text LIKE ? AND LENGTH(word_text) >= 2 " +
                "ORDER BY frequency DESC, word_text ASC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return suggestions;
            }

            String searchPattern = prefix.toLowerCase() + "%";
            System.out.println("🔍 Searching for: " + searchPattern);

            pstmt.setString(1, searchPattern);
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                suggestions.add(rs.getString("word_text"));
            }

            System.out.println("💡 Found " + suggestions.size() + " suggestions for '" + prefix + "'");

        } catch (SQLException e) {
            System.err.println("❌ Error getting suggestions: " + e.getMessage());
            e.printStackTrace();
        }

        return suggestions;
    }

    /**
     * Enhanced suggestion method that includes frequency information
     * Useful for displaying word popularity to users
     *
     * @param prefix The word prefix to search for
     * @param limit Maximum number of suggestions to return
     * @return List of suggested words with frequency counts
     */
    public static List<String> getLiterarySuggestions(String prefix, int limit) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        List<String> suggestions = new ArrayList<>();
        String sql = "SELECT word_text, frequency FROM autocomplete_words " +
                "WHERE word_text LIKE ? AND LENGTH(word_text) >= 2 " +
                "ORDER BY frequency DESC, word_text ASC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return suggestions;
            }

            pstmt.setString(1, prefix.toLowerCase() + "%");
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String word = rs.getString("word_text");
                int frequency = rs.getInt("frequency");
                // Format: "word (frequency)" - shows how common the word is
                suggestions.add(word + " (" + frequency + ")");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting literary suggestions: " + e.getMessage());
            e.printStackTrace();
        }

        return suggestions;
    }

    /**
     * Retrieves the most common words from the database
     * Useful for analytics and understanding text corpus characteristics
     *
     * @param limit Maximum number of common words to return
     * @return List of most common words with frequency counts
     */
    public static List<String> getMostCommonWords(int limit) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        List<String> commonWords = new ArrayList<>();
        String sql = "SELECT word_text, frequency FROM autocomplete_words " +
                "WHERE LENGTH(word_text) >= 3 " +  // Only words with 3+ characters
                "ORDER BY frequency DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                return commonWords;
            }

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String word = rs.getString("word_text");
                int frequency = rs.getInt("frequency");
                commonWords.add(word + " (" + frequency + ")");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting common words: " + e.getMessage());
        }

        return commonWords;
    }

    /**
     * Gets the total count of unique words in the database
     *
     * @return Number of unique words stored in the database
     */
    public static int getTotalWordCount() {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        String sql = "SELECT COUNT(*) as total FROM autocomplete_words";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (conn == null) {
                return 0;
            }

            if (rs.next()) {
                int count = rs.getInt("total");
                System.out.println("📊 Database contains " + count + " unique words");
                return count;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting word count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Gets the total frequency count (sum of all word frequencies)
     * Represents the total number of word occurrences in the source texts
     *
     * @return Total frequency count across all words
     */
    public static int getTotalFrequencyCount() {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        String sql = "SELECT SUM(frequency) as total_frequency FROM autocomplete_words";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (conn == null) {
                return 0;
            }

            if (rs.next()) {
                return rs.getInt("total_frequency");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting frequency count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Checks if the database has been populated with words
     *
     * @return boolean indicating if database contains words
     */
    public static boolean isDatabasePopulated() {
        return getTotalWordCount() > 0;
    }

    /**
     * Generates formatted database statistics string
     * Useful for displaying database status to users
     *
     * @return Formatted string with database statistics
     */
    public static String getDatabaseStatistics() {
        int uniqueWords = getTotalWordCount();
        int totalOccurrences = getTotalFrequencyCount();

        if (uniqueWords == 0) {
            return "Database empty - No words loaded";
        }

        return String.format("%,d unique words | %,d total occurrences", uniqueWords, totalOccurrences);
    }

    /**
     * Gets the frequency of a specific word in the database
     *
     * @param word The word to check frequency for
     * @return Frequency count of the specified word
     */
    public static int getWordFrequency(String word) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        String sql = "SELECT frequency FROM autocomplete_words WHERE word_text = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                return 0;
            }

            pstmt.setString(1, word.toLowerCase());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("frequency");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting word frequency: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Clears all words from the database (for reset purposes)
     *
     * @return boolean indicating clearance success
     */
    public static boolean clearDatabase() {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        String sql = "TRUNCATE TABLE autocomplete_words";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (conn == null) {
                return false;
            }

            stmt.executeUpdate(sql);
            System.out.println("✅ Auto-complete database cleared successfully");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error clearing database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets contextual suggestions prioritizing novel-specific words
     * Filters by minimum frequency to ensure word relevance
     *
     * @param prefix The word prefix to search for
     * @param limit Maximum number of suggestions to return
     * @return List of contextually relevant word suggestions
     */
    public static List<String> getContextualSuggestions(String prefix, int limit) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        List<String> suggestions = new ArrayList<>();
        String sql = "SELECT word_text, frequency FROM autocomplete_words " +
                "WHERE word_text LIKE ? AND LENGTH(word_text) >= 2 " +
                "AND frequency >= 5 " +  // Only words that appear at least 5 times
                "ORDER BY frequency DESC, word_text ASC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("❌ Database connection is null!");
                return suggestions;
            }

            pstmt.setString(1, prefix.toLowerCase() + "%");
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                suggestions.add(rs.getString("word_text"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting contextual suggestions: " + e.getMessage());
            e.printStackTrace();
        }

        return suggestions;
    }

    /**
     * Tests database connection and table functionality
     *
     * @return boolean indicating database connection success
     */
    public static boolean testDatabaseConnection() {
        try {
            createTableIfNotExists();
            int wordCount = getTotalWordCount();
            System.out.println("✅ Database connection test successful");
            System.out.println("📊 Current word count: " + wordCount);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets random sample words from the database for testing
     *
     * @param count Number of sample words to retrieve
     * @return List of random sample words
     */
    public static List<String> getSampleWords(int count) {
        // Ensure table exists first to prevent errors
        createTableIfNotExists();

        List<String> sampleWords = new ArrayList<>();
        String sql = "SELECT word_text FROM autocomplete_words " +
                "WHERE LENGTH(word_text) BETWEEN 3 AND 8 " +
                "ORDER BY RAND() " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                return sampleWords;
            }

            pstmt.setInt(1, count);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sampleWords.add(rs.getString("word_text"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting sample words: " + e.getMessage());
        }

        return sampleWords;
    }
}