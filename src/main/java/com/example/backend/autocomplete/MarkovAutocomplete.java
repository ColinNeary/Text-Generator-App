// Written by Raghav Mukundan, rxm210068

package com.example.backend.autocomplete;

import com.example.backend.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Markov-based autocomplete that uses probability distribution sampling
 */
public class MarkovAutocomplete {
    private final DatabaseManager db;
    private final int maxSuggestions;
    private final Random random;
    
    public MarkovAutocomplete() throws SQLException {
        this(5);
    }
    
    public MarkovAutocomplete(int maxSuggestions) throws SQLException {
        this.db = DatabaseManager.getInstance();
        this.maxSuggestions = maxSuggestions;
        this.random = new Random();
    }
    
    // Get word suggestions using weighted random sampling from probability distribution
    public List<Suggestion> getSuggestions(String currentWord) throws SQLException {
        if (currentWord == null || currentWord.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String normalized = normalizeWord(currentWord);
        
        if (!db.wordExists(normalized)) {
            return Collections.emptyList();
        }
        
        // Get a larger pool of candidates (top 50) for sampling variety
        List<DatabaseManager.WordPair> allPairs = db.getNextWords(normalized, 50);
        
        if (allPairs.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Use weighted random sampling to select diverse suggestions
        Set<String> selectedWords = new HashSet<>();
        List<Suggestion> suggestions = new ArrayList<>();
        
        // Sample multiple times to get maxSuggestions unique words
        int attempts = 0;
        while (suggestions.size() < maxSuggestions && attempts < maxSuggestions * 10) {
            DatabaseManager.WordPair sampled = weightedRandomSample(allPairs);
            
            // Only add if we haven't already suggested this word
            if (sampled != null && selectedWords.add(sampled.getRightWord())) {
                suggestions.add(new Suggestion(
                        sampled.getRightWord(),
                        sampled.getProbability(),
                        sampled.getPairCount(),
                        "Markov - Sampled"
                ));
            }
            attempts++;
        }
        
        return suggestions;
    }
    
    // Perform weighted random sampling from word pairs based on probability
    private DatabaseManager.WordPair weightedRandomSample(List<DatabaseManager.WordPair> pairs) {
        if (pairs.isEmpty()) return null;
        
        double totalProb = pairs.stream().mapToDouble(DatabaseManager.WordPair::getProbability).sum();
        double rand = random.nextDouble() * totalProb;
        double cumulative = 0.0;
        
        for (DatabaseManager.WordPair pair : pairs) {
            cumulative += pair.getProbability();
            if (rand <= cumulative) {
                return pair;
            }
        }
        
        return pairs.get(0); // Fallback to first pair
    }
    
    // Sample a single word randomly from probability distribution
    public Suggestion sampleSuggestion(String currentWord) throws SQLException {
        List<DatabaseManager.WordPair> pairs = db.getNextWords(normalizeWord(currentWord), 100);
        if (pairs.isEmpty()) return null;
        
        DatabaseManager.WordPair sampled = weightedRandomSample(pairs);
        if (sampled == null) return null;
        
        return new Suggestion(sampled.getRightWord(), sampled.getProbability(), 
            sampled.getPairCount(), "Markov - Sampled");
    }
    
    // Build text by repeatedly sampling next words
    public String buildText(String startWord, int numWords) throws SQLException {
        StringBuilder result = new StringBuilder(normalizeWord(startWord));
        String currentWord = normalizeWord(startWord);
        
        for (int i = 0; i < numWords; i++) {
            Suggestion sampled = sampleSuggestion(currentWord);
            if (sampled == null) break;
            
            result.append(" ").append(sampled.getWord());
            currentWord = sampled.getWord();
        }
        
        return result.toString();
    }
    
    // Normalize word to lowercase
    private String normalizeWord(String word) {
        return word.trim().toLowerCase();
    }
    
    // Suggestion data class with metadata
    public static class Suggestion {
        private final String word;
        private final double probability;
        private final int frequency;
        private final String algorithm;
        
        public Suggestion(String word, double probability, int frequency, String algorithm) {
            this.word = word;
            this.probability = probability;
            this.frequency = frequency;
            this.algorithm = algorithm;
        }
        
        public String getWord() { return word; }
        public double getProbability() { return probability; }
        public int getFrequency() { return frequency; }
        public String getAlgorithm() { return algorithm; }
        
        @Override
        public String toString() {
            return String.format("%-15s | Prob: %.4f | Freq: %4d | %s", 
                word, probability, frequency, algorithm);
        }
    }
}
