// Written by Kai Chang, cxc210040

package com.example.backend.sentencegen;

import com.example.backend.DatabaseManager;
import java.sql.SQLException;
import java.util.*;

// Generates sentences using beam search: maintains top-k candidate paths simultaneously
public class BeamSearchSentenceGenerator {
    private final DatabaseManager db;
    private final int beamWidth;
    private final int maxSentenceLength;
    private final boolean debugMode;
    private final Random random = new Random();

    // Words that shouldn't end sentences (articles, prepositions, auxiliaries, etc.)
    private static final Set<String> BAD_ENDING_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "my", "his", "her", "their", "our",
            "in", "on", "at", "by", "for", "with", "about", "to", "from", "of", "into",
            "and", "but", "or", "nor", "so", "because", "if", "when", "than", "as",
            "is", "am", "are", "was", "were", "be", "been", "have", "has", "had",
            "will", "would", "shall", "should", "can", "could", "may", "might", "must",
            "very", "too", "quite", "rather", "really", "not"
    ));

    // POS tags that make good sentence endings
    private static final Set<String> GOOD_ENDING_TAGS = new HashSet<>(Arrays.asList(
            "NOUN", "VERB_TRANS", "VERB_INTR", "VERB_PAST", "ADJ", "VERB_ING"
    ));

    public BeamSearchSentenceGenerator(int beamWidth, int maxSentenceLength, boolean debugMode) throws SQLException {
        this.db = DatabaseManager.getInstance();
        this.beamWidth = beamWidth;
        this.maxSentenceLength = maxSentenceLength;
        this.debugMode = debugMode;
    }

    
    // Main generation loop: builds sentences word-by-word across multiple beams
    public SentenceResult generateSentence(String startWord) throws SQLException {
        startWord = startWord.trim().toLowerCase();
        if (!db.wordExists(startWord)) {
            return new SentenceResult(null, 0, "Word not found", "Beam Search");
        }

        List<Beam> beams = new ArrayList<>();
        beams.add(new Beam(startWord));

        long startTime = System.currentTimeMillis();

        // Iteratively extend each beam with best candidate words
        for (int step = 0; step < maxSentenceLength; step++) {
            List<Beam> allCandidates = new ArrayList<>();

            for (Beam beam : beams) {
                if (beam.words.size() >= maxSentenceLength) continue;

                // Early termination: 25% chance to stop at good ending after 8 words
                if (beam.words.size() >= 8) {
                    String lastWord = beam.getLastWord();
                    String lastTag = db.getPOSTag(lastWord);
                    if (GOOD_ENDING_TAGS.contains(lastTag) && !BAD_ENDING_WORDS.contains(lastWord)) {
                        if (random.nextDouble() < 0.25) {
                            continue; 
                        }
                    }
                }

                // Get next word candidates from corpus
                String currentWord = beam.getLastWord();
                List<DatabaseManager.WordPair> nextWords = db.getNextWords(currentWord, 30);

                if (nextWords.isEmpty()) continue;

                // Score and extend beam for each candidate
                for (DatabaseManager.WordPair pair : nextWords) {
                    String nextWord = pair.getRightWord();

                    // Skip if already used
                    if (beam.words.contains(nextWord)) continue;

                    // Skip single letters except 'a' and 'i'
                    if (nextWord.length() == 1 && !nextWord.equals("a") && !nextWord.equals("i")) continue;

                    // Get POS tags
                    String currentTag = db.getPOSTag(currentWord);
                    String nextTag = db.getPOSTag(nextWord);

                    // Calculate score (same style as Greedy)
                    double naturalScore = pair.getProbability();
                    double grammarBonus = getGrammarScore(currentTag, nextTag);

                    boolean hasViolation = grammarBonus < 0;
                    double totalScore = Math.log(naturalScore + 1e-10) + grammarBonus;

                    // Create new beam with this word added
                    allCandidates.add(beam.extendWith(nextWord, totalScore, hasViolation));
                }
            }

            if (allCandidates.isEmpty()) break;

            // Sort by violations first, then score; keep top-k diverse beams
            allCandidates.sort((a, b) -> {
                // First: prefer fewer violations
                if (a.violations != b.violations) {
                    return Integer.compare(a.violations, b.violations);
                }
                // Second: prefer higher score
                return Double.compare(b.cumulativeScore, a.cumulativeScore);
            });

            // Keep top beamWidth diverse candidates
            beams = selectDiverseBeams(allCandidates, beamWidth);
        }

        // Select best beam by average score per word
        Beam best = beams.stream()
                .max(Comparator.comparingDouble(b -> b.cumulativeScore / b.words.size()))
                .orElse(beams.get(0));

        // Clean up bad endings
        List<String> cleanWords = new ArrayList<>(best.words);
        while (!cleanWords.isEmpty() && BAD_ENDING_WORDS.contains(cleanWords.get(cleanWords.size() - 1))) {
            cleanWords.remove(cleanWords.size() - 1);
        }

        String finalSentence = String.join(" ", cleanWords);

        // Capitalize first letter
        if (!finalSentence.isEmpty()) {
            finalSentence = Character.toUpperCase(finalSentence.charAt(0)) + finalSentence.substring(1);
        }

        // Normalize score: base 50 + avgScore bonus + length bonus - violation penalty
        long duration = System.currentTimeMillis() - startTime;

        double baseScore = 50;
        double avgWordScore = best.cumulativeScore / Math.max(1, cleanWords.size());

        double lengthBonus = 0;
        if (cleanWords.size() >= 7 && cleanWords.size() <= 12) {
            lengthBonus = 5;
        } else if (cleanWords.size() >= 5 && cleanWords.size() <= 14) {
            lengthBonus = 2;
        }

        double normalizedScore = baseScore + (avgWordScore * 8) + lengthBonus - (best.violations * 8);
        normalizedScore = Math.min(100, Math.max(0, normalizedScore));

        return new SentenceResult(
                finalSentence,
                normalizedScore,
                "Beam Search (violations: " + best.violations + ")",
                "Beam Search",
                cleanWords.size(),
                duration
        );
    }

    // Ensures beam diversity by preventing beams with identical endings
    private List<Beam> selectDiverseBeams(List<Beam> candidates, int count) {
        List<Beam> selected = new ArrayList<>();
        Set<String> usedEndings = new HashSet<>();

        for (Beam candidate : candidates) {
            if (selected.size() >= count) break;

            // Get last 2 words as a signature
            String ending = "";
            int size = candidate.words.size();
            if (size >= 2) {
                ending = candidate.words.get(size - 2) + " " + candidate.words.get(size - 1);
            } else if (size >= 1) {
                ending = candidate.words.get(size - 1);
            }

            // Only add if we don't already have a beam with this ending
            if (!usedEndings.contains(ending)) {
                selected.add(candidate);
                usedEndings.add(ending);
            }
        }

        // Fallback: add best remaining beams if not enough unique ones
        if (selected.size() < count && candidates.size() > selected.size()) {
            for (Beam candidate : candidates) {
                if (selected.size() >= count) break;
                if (!selected.contains(candidate)) {
                    selected.add(candidate);
                }
            }
        }

        return selected;
    }


    // POS transition scoring: positive for good grammar, negative for violations
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

        // Bad transitions
        if (currentTag.equals("DET") && nextTag.equals("DET")) return -3.0;
        if (currentTag.equals("DET") && nextTag.startsWith("VERB")) return -2.5;
        if (currentTag.equals("PREP") && nextTag.equals("PREP")) return -3.0;
        if (currentTag.equals("PREP") && nextTag.startsWith("VERB")) return -2.0;
        if (currentTag.startsWith("VERB") && nextTag.startsWith("VERB") && !nextTag.equals("VERB_ING")) return -2.5;
        if (currentTag.equals("ADJ") && nextTag.equals("ADJ")) return -1.5;

        // Neutral
        return 0.5;
    }

    // Tracks a single sentence path: words, cumulative score, and violation count
    private static class Beam {
        List<String> words;
        double cumulativeScore;
        int violations;

        public Beam(String firstWord) {
            this.words = new ArrayList<>();
            this.words.add(firstWord);
            this.cumulativeScore = 0.0;
            this.violations = 0;
        }

        public Beam(List<String> words, double score, int violations) {
            this.words = new ArrayList<>(words);
            this.cumulativeScore = score;
            this.violations = violations;
        }

        public Beam extendWith(String word, double scoreIncrement, boolean hasViolation) {
            List<String> newWords = new ArrayList<>(this.words);
            newWords.add(word);
            int newViolations = this.violations + (hasViolation ? 1 : 0);
            return new Beam(newWords, this.cumulativeScore + scoreIncrement, newViolations);
        }

        public String getLastWord() {
            return words.isEmpty() ? "" : words.get(words.size() - 1);
        }
    }

    
    // Output container with sentence, score, and metadata
    public static class SentenceResult {
        String sentence, metadata, algorithm;
        double score;
        int wordCount;
        long time;

        public SentenceResult(String s, double sc, String m, String a, int w, long t) {
            sentence = s;
            score = sc;
            metadata = m;
            algorithm = a;
            wordCount = w;
            time = t;
        }

        public SentenceResult(String s, double sc, String m, String a) {
            this(s, sc, m, a, s == null ? 0 : s.split(" ").length, 0);
        }

        public String getSentence() { return sentence; }
        public int getWordCount() { return wordCount; }
        public double getScore() { return score; }
        public long getGenerationTimeMs() { return time; }
        public String getAlgorithm() { return algorithm; }
        public String getMetadata() { return metadata; }
    }
}