import java.util.*;

/**
 * Markov Chain Implementation for Sentence Generation
 * 
 * 
 * How it works:
 * 1. Start with the given word
 * 2. At each step:
 *    - Look at all possible next words
 *    - Calculate probability of each word appearing after current word
 *    - Randomly select next word based on these probabilities
 *    - Add selected word to sentence
 *    - Repeat until reaching an end word or max length
 * 
 * Probability Calculation:
 * P(next_word | current_word) = count(current_word → next_word) / total_transitions_from(current_word)
 * 
 */
public class MarkovSentenceGenerator extends AbstractSentenceGenerator {
    private final Random random = new Random();
    
    @Override
    public String generateSentence(String startWordText) {
        // Get starting word
        Word currentWord = wordMap.get(startWordText.toLowerCase());
        if (currentWord == null) return "Error: Starting word not found.";
        
        // Build sentence
        List<String> sentence = new ArrayList<>();
        sentence.add(currentWord.getText());
        
        // Markov chain: randomly select next word based on frequencies
        while (sentence.size() < MAX_SENTENCE_LENGTH) {
            Map<Word, Bigram> nextWords = bigramMap.get(currentWord);
            if (nextWords == null || nextWords.isEmpty()) break;
            
            // Calculate total frequency
            int total = 0;
            for (Bigram bigram : nextWords.values()) {
                total += bigram.getCount();
            }
            
            // Random selection based on frequencies
            int rand = random.nextInt(total);
            Word nextWord = null;
            for (Map.Entry<Word, Bigram> entry : nextWords.entrySet()) {
                rand -= entry.getValue().getCount();
                if (rand < 0) {
                    nextWord = entry.getKey();
                    break;
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