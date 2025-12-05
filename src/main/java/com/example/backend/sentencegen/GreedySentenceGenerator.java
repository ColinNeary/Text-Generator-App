// Written by Kai Chang, cxc210040

package com.example.backend.sentencegen;

import com.example.backend.DatabaseManager;
import java.sql.SQLException;
import java.util.*;

// Generates sentences using greedy algorithm: single path, picks best local candidate
public class GreedySentenceGenerator {
    private final DatabaseManager db;
    private final int maxSentenceLength;
    private final boolean debugMode;
    private final Random random = new Random();

    // Words that signal sentence should end
    private static final Set<String> GOOD_ENDINGS = new HashSet<>(Arrays.asList(
            "NOUN", "VERB_TRANS", "VERB_INTR", "VERB_PAST", "ADJ", "VERB_ING"
    ));

    // Words that should NEVER end a sentence
    private static final Set<String> BAD_ENDING_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "my", "his", "her", "their", "our",
            "in", "on", "at", "by", "for", "with", "about", "to", "from", "of", "into",
            "and", "but", "or", "nor", "so", "because", "if", "when", "than", "as",
            "is", "am", "are", "was", "were", "be", "been", "have", "has", "had",
            "will", "would", "shall", "should", "can", "could", "may", "might", "must",
            "very", "too", "quite", "rather", "really", "not"
    ));

    public GreedySentenceGenerator(int maxSentenceLength, boolean debugMode) throws SQLException {
        this.db = DatabaseManager.getInstance();
        this.maxSentenceLength = maxSentenceLength;
        this.debugMode = debugMode;
    }

    // Single-path generation: picks best candidate at each step
    public SentenceResult generateSentence(String startWord) throws SQLException {
        startWord = startWord.trim().toLowerCase();
        if (!db.wordExists(startWord)) {
            return new SentenceResult(null, 0.0, "Word not found", "Greedy");
        }

        List<String> sentence = new ArrayList<>();
        sentence.add(startWord);

        Set<String> usedWords = new HashSet<>();
        usedWords.add(startWord);

        double cumulativeScore = 0.0;
        int grammarViolations = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < maxSentenceLength; i++) {
            String currentWord = sentence.get(sentence.size() - 1);
            String currentTag = db.getPOSTag(currentWord);

            // Early termination: 30% chance after 6+ words at good ending
            if (sentence.size() >= 6) {
                if (GOOD_ENDINGS.contains(currentTag) && !BAD_ENDING_WORDS.contains(currentWord)) {
                    if (random.nextDouble() < 0.3) {
                        break;
                    }
                }
            }

            // Force stop if we hit max length
            if (sentence.size() >= maxSentenceLength) {
                break;
            }

            // Get and filter candidates
            List<DatabaseManager.WordPair> candidates = db.getNextWords(currentWord, 30);
            if (candidates.isEmpty()) break;

            // Filter candidates
            List<ScoredCandidate> validCandidates = new ArrayList<>();
            for (DatabaseManager.WordPair pair : candidates) {
                String nextWord = pair.getRightWord();

                // Skip if already used
                if (usedWords.contains(nextWord)) continue;

                // Skip single letters (except 'a' and 'i')
                if (nextWord.length() == 1 && !nextWord.equals("a") && !nextWord.equals("i")) continue;

                // Skip if same as current word
                if (nextWord.equals(currentWord)) continue;

                String nextTag = db.getPOSTag(nextWord);

                // Calculate score for this candidate
                double naturalScore = pair.getProbability();
                double grammarScore = getGrammarScore(currentTag, nextTag);
                boolean hasViolation = grammarScore < 0;

                // Combined score: natural flow + grammar bonus
                double totalScore = Math.log(naturalScore + 1e-10) + grammarScore;

                validCandidates.add(new ScoredCandidate(nextWord, nextTag, totalScore, hasViolation));
            }

            if (validCandidates.isEmpty()) break;

            // Greedy choice: pick from top 5 for some randomness
            validCandidates.sort((a, b) -> Double.compare(b.score, a.score));

            int sampleSize = Math.min(validCandidates.size(), 5);
            ScoredCandidate chosen = validCandidates.get(random.nextInt(sampleSize));

            sentence.add(chosen.word);
            usedWords.add(chosen.word);
            cumulativeScore += chosen.score;
            if (chosen.hasViolation) grammarViolations++;
        }

        // Clean trailing bad endings
        while (sentence.size() > 1 && BAD_ENDING_WORDS.contains(sentence.get(sentence.size() - 1))) {
            sentence.remove(sentence.size() - 1);
        }

        String finalSentence = String.join(" ", sentence);

        // Capitalize first letter
        if (!finalSentence.isEmpty()) {
            finalSentence = Character.toUpperCase(finalSentence.charAt(0)) + finalSentence.substring(1);
        }

        long duration = System.currentTimeMillis() - startTime;

        // Score calculation: Match Beam Search scoring for fair comparison
        double baseScore = 50;

        // Average score per word (quality metric)
        double avgWordScore = cumulativeScore / Math.max(1, sentence.size());

        // Length bonus: prefer 7-12 words
        double lengthBonus = 0;
        if (sentence.size() >= 7 && sentence.size() <= 12) {
            lengthBonus = 5;
        } else if (sentence.size() >= 5 && sentence.size() <= 14) {
            lengthBonus = 2;
        }

        // Final score
        double normalizedScore = baseScore + (avgWordScore * 8) + lengthBonus - (grammarViolations * 8);
        normalizedScore = Math.min(100, Math.max(0, normalizedScore));

        return new SentenceResult(
                finalSentence,
                normalizedScore,
                "Greedy (violations: " + grammarViolations + ")",
                "Greedy",
                sentence.size(),
                duration
        );
    }

    // Same grammar scoring as Beam Search
    private double getGrammarScore(String currentTag, String nextTag) {
        // Excellent transitions
        if (currentTag.equals("DET") && nextTag.equals("NOUN")) return 3.0;
        if (currentTag.equals("DET") && nextTag.equals("ADJ")) return 2.5;
        if (currentTag.equals("ADJ") && nextTag.equals("NOUN")) return 3.0;
        if (currentTag.equals("NOUN") && nextTag.startsWith("VERB")) return 2.5;
        if (currentTag.equals("PRON") && nextTag.startsWith("VERB")) return 2.5;
        if (currentTag.startsWith("VERB") && nextTag.equals("PREP")) return 2.0;
        if (currentTag.startsWith("VERB") && nextTag.equals("DET")) return 2.0;
        if (currentTag.startsWith("VERB") && nextTag.equals("ADV")) return 1.5;
        if (currentTag.equals("PREP") && nextTag.equals("DET")) return 2.5;
        if (currentTag.equals("PREP") && nextTag.equals("NOUN")) return 2.0;
        if (currentTag.equals("ADV") && nextTag.startsWith("VERB")) return 2.0;
        if (currentTag.equals("ADV") && nextTag.equals("ADJ")) return 2.0;
        if (currentTag.equals("VERB_MODAL") && nextTag.equals("VERB_BASE")) return 3.0;
        if (currentTag.equals("VERB_AUX") && nextTag.equals("VERB_ING")) return 2.5;
        if (currentTag.equals("CONJ") && (nextTag.equals("NOUN") || nextTag.equals("PRON") || nextTag.equals("DET"))) return 2.0;

        // Bad transitions (grammar violations)
        if (currentTag.equals("DET") && nextTag.equals("DET")) return -3.0;
        if (currentTag.equals("DET") && nextTag.startsWith("VERB")) return -2.5;
        if (currentTag.equals("PREP") && nextTag.equals("PREP")) return -3.0;
        if (currentTag.equals("PREP") && nextTag.startsWith("VERB")) return -2.0;
        if (currentTag.startsWith("VERB") && nextTag.startsWith("VERB") && !nextTag.equals("VERB_ING")) return -2.5;
        if (currentTag.equals("ADJ") && nextTag.equals("ADJ")) return -1.5;

        // Neutral transition
        return 0.5;
    }

    // Temporary holder for candidate word scores
    private static class ScoredCandidate {
        String word;
        String tag;
        double score;
        boolean hasViolation;

        ScoredCandidate(String word, String tag, double score, boolean hasViolation) {
            this.word = word;
            this.tag = tag;
            this.score = score;
            this.hasViolation = hasViolation;
        }
    }
    
    // Result container (extends Beam's for compatibility)
    public static class SentenceResult extends BeamSearchSentenceGenerator.SentenceResult {
        public SentenceResult(String s, double sc, String m, String a, int w, long t) {
            super(s, sc, m, a, w, t);
        }
        public SentenceResult(String s, double sc, String m, String a) {
            super(s, sc, m, a);
        }
    }
}