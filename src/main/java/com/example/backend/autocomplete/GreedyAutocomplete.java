// Written by Raghav Mukundan, rxm210068

package com.example.backend.autocomplete;

import com.example.backend.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Greedy-based autocomplete that always selects words with highest probability
 */
public class GreedyAutocomplete {
    private final DatabaseManager db;
    private final int maxSuggestions;
    
    public GreedyAutocomplete() throws SQLException {
        this(5);
    }
    
    public GreedyAutocomplete(int maxSuggestions) throws SQLException {
        this.db = DatabaseManager.getInstance();
        this.maxSuggestions = maxSuggestions;
    }
    
    // Get top word suggestions sorted by probability (highest first)
    public List<Suggestion> getSuggestions(String currentWord) throws SQLException {
        if (currentWord == null || currentWord.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String normalized = normalizeWord(currentWord);
        
        if (!db.wordExists(normalized)) {
            return Collections.emptyList();
        }
        
        // Query database for top word pairs by probability
        List<DatabaseManager.WordPair> pairs = db.getNextWords(normalized, maxSuggestions);
        
        return pairs.stream()
                .map(pair -> new Suggestion(
                        pair.getRightWord(),
                        pair.getProbability(),
                        pair.getPairCount(),
                        "Greedy - Highest Probability"
                ))
                .collect(Collectors.toList());
    }
    
    // Get the single best suggestion (highest probability word)
    public Suggestion getBestSuggestion(String currentWord) throws SQLException {
        List<Suggestion> suggestions = getSuggestions(currentWord);
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }
    
    // Build text by repeatedly choosing highest probability word
    public String buildText(String startWord, int numWords) throws SQLException {
        StringBuilder result = new StringBuilder(normalizeWord(startWord));
        String currentWord = normalizeWord(startWord);
        
        for (int i = 0; i < numWords; i++) {
            Suggestion best = getBestSuggestion(currentWord);
            if (best == null) break;
            
            result.append(" ").append(best.getWord());
            currentWord = best.getWord();
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
