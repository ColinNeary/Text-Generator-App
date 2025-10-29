import java.util.*;

/**
 * Greedy Algorithm Implementation for Sentence Generation
 * 
 * Core Concept:
 * The greedy algorithm always chooses the locally optimal choice at each step.
 * In this context, it means always selecting the most frequently occurring next word.
 * 
 * How it works:
 * 1. Start with the given word
 * 2. At each step:
 *    - Look at all possible next words
 *    - Choose the one that appears most frequently after the current word
 *    - Add it to the sentence
 *    - Repeat until reaching an end word or max length
 * 
 * Advantages:
 * - Simple and fast
 * - Predictable results
 * - Always finds the most common path
 * 
 * Limitations:
 * - May miss better overall sentences by focusing on local choices
 * - Can get stuck in short, common patterns
 * - No variety in output (same input = same output)
 */
public class SentenceGenerator extends AbstractSentenceGenerator {
    private Map<String, Word> wordMap;
    private Map<Word, Map<Word, Bigram>> bigramMap;
    private static final int MAX_SENTENCE_LENGTH = 20;
    
    public SentenceGenerator() {
        this.wordMap = new HashMap<>();
        this.bigramMap = new HashMap<>();
    }
    
    public void addWord(Word word) {
        wordMap.put(word.getText(), word);
    }
    
    public void addBigram(Word prevWord, Word nextWord) {
        bigramMap.computeIfAbsent(prevWord, k -> new HashMap<>());
        Bigram bigram = bigramMap.get(prevWord)
            .computeIfAbsent(nextWord, k -> new Bigram(prevWord, nextWord));
        bigram.incrementCount();
    }
    
    public String generateSentence(String startWordText) {
        // Get starting word
        Word currentWord = wordMap.get(startWordText.toLowerCase());
        if (currentWord == null) return "Error: Starting word not found.";
        
        // Build sentence
        List<String> sentence = new ArrayList<>();
        sentence.add(currentWord.getText());
        
        // Simple greedy: always pick the most frequent next word
        while (sentence.size() < MAX_SENTENCE_LENGTH) {
            Map<Word, Bigram> nextWords = bigramMap.get(currentWord);
            if (nextWords == null || nextWords.isEmpty()) break;
            
            // Find word with highest count
            Word nextWord = null;
            int maxCount = -1;
            for (Map.Entry<Word, Bigram> entry : nextWords.entrySet()) {
                if (entry.getValue().getCount() > maxCount) {
                    maxCount = entry.getValue().getCount();
                    nextWord = entry.getKey();
                }
            }
            
            if (nextWord == null) break;
            sentence.add(nextWord.getText());
            currentWord = nextWord;
            
            // Stop if it's an ending word
            if (currentWord.getEndCount() > 0) break;
        }
        
        return String.join(" ", sentence) + ".";
    }
}