import java.util.*;

public class SentenceGenerator {
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
        Word startWord = wordMap.get(startWordText.toLowerCase());
        if (startWord == null) {
            return "Error: Starting word not found.";
        }
        
        List<String> sentence = new ArrayList<>();
        Word currentWord = startWord;
        sentence.add(currentWord.getText());
        
        while (sentence.size() < MAX_SENTENCE_LENGTH) {
            Word nextWord = findNextWord(currentWord);
            if (nextWord == null) break;
            
            sentence.add(nextWord.getText());
            currentWord = nextWord;
            
            // Stop if we've reached a word that commonly ends sentences
            if (currentWord.getEndCount() > 0 && 
                currentWord.getEndCount() >= currentWord.getTotalCount() / 2) {
                break;
            }
        }
        
        return String.join(" ", sentence) + ".";
    }
    
    private Word findNextWord(Word currentWord) {
        Map<Word, Bigram> nextWords = bigramMap.get(currentWord);
        if (nextWords == null || nextWords.isEmpty()) {
            return null;
        }
        
        // Find the word with highest bigram count (greedy approach)
        return nextWords.entrySet().stream()
            .max(Comparator.comparingInt(e -> e.getValue().getCount()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}