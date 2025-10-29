import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSentenceGenerator implements ISentenceGenerator {
    protected Map<String, Word> wordMap;
    protected Map<Word, Map<Word, Bigram>> bigramMap;
    protected static final int MAX_SENTENCE_LENGTH = 20;
    
    public AbstractSentenceGenerator() {
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
}