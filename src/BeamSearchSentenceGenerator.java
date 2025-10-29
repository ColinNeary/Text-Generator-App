import java.util.*;

/**
 * Beam Search Implementation for Sentence Generation
 * 
 * Beam search is a modified breadth-first search that keeps track of the k most promising
 * states at each step, where k is the beam width. It's a compromise between greedy
 * (k=1) and exhaustive search (k=∞).
 * 
 * How it works:
 * 1. Start with initial word
 * 2. At each step:
 *    - For each current path in the beam:
 *      * Generate all possible next words
 *      * Create new paths by adding each possible word
 *    - Score all new paths using multiple factors:
 *      * Transition probabilities (bigram frequencies)
 *      * Sequence length
 *      * Word variety
 *      * Controlled randomness
 *    - Keep only the top k paths (beam width)
 *    - Continue until valid ending or max length
 * 3. Randomly select from top 3 final paths
 * 
 * Scoring Factors:
 * - Base score: log(bigram_frequency + 1)
 * - Length bonus: min(10.0, path_size) * 2.0
 * - Variety bonus: unique_words * 1.5
 * - Random factor: random * 2.0
 * 
 * Parameters:
 * - BEAM_WIDTH: Number of paths to maintain (currently 5)
 * - Selection from top 3 paths for final output
 */
public class BeamSearchSentenceGenerator extends AbstractSentenceGenerator {
    private static final int BEAM_WIDTH = 5; // Number of paths to maintain
    private final Random random = new Random();  // Add some controlled randomness
    
    @Override
    public String generateSentence(String startWordText) {
        // Get starting word
        Word startWord = wordMap.get(startWordText.toLowerCase());
        if (startWord == null) return "Error: Starting word not found.";
        
        // Initialize beam with starting path
        List<List<Word>> beam = new ArrayList<>();
        beam.add(new ArrayList<>(Collections.singletonList(startWord)));
        
        // Build sentences keeping top BEAM_WIDTH candidates
        while (beam.get(0).size() < MAX_SENTENCE_LENGTH) {
            List<List<Word>> newBeam = new ArrayList<>();
            
            // Expand each path in current beam
            for (List<Word> path : beam) {
                Word lastWord = path.get(path.size() - 1);
                Map<Word, Bigram> nextWords = bigramMap.get(lastWord);
                
                if (nextWords == null) continue;
                
                // Add all possible next words
                for (Map.Entry<Word, Bigram> entry : nextWords.entrySet()) {
                    List<Word> newPath = new ArrayList<>(path);
                    newPath.add(entry.getKey());
                    newBeam.add(newPath);
                }
            }
            
            if (newBeam.isEmpty()) break;
            
            // Keep top BEAM_WIDTH paths based on combined score
            beam = newBeam.stream()
                .sorted((p1, p2) -> -Double.compare(getPathScore(p1), getPathScore(p2)))
                .limit(BEAM_WIDTH)
                .toList();
            
            // Only stop if we have a reasonably long sentence that ends properly
            Word lastWord = beam.get(0).get(beam.get(0).size() - 1);
            if (beam.get(0).size() >= 4 && lastWord.getEndCount() > 0) {
                // Add some randomness in path selection for variety
                int pathIndex = random.nextInt(Math.min(beam.size(), 3));
                List<Word> selectedPath = beam.get(pathIndex);
                if (selectedPath.get(selectedPath.size() - 1).getEndCount() > 0) {
                    return String.join(" ", selectedPath.stream()
                        .map(Word::getText)
                        .toList()) + ".";
                }
            }
        }
        
        // Return one of the top 3 paths randomly
        int pathIndex = random.nextInt(Math.min(beam.size(), 3));
        return String.join(" ", beam.get(pathIndex).stream()
            .map(Word::getText)
            .toList()) + ".";
    }
    
    private double getPathScore(List<Word> path) {
        if (path.size() < 2) return 0.0;
        
        double score = 0.0;
        Set<String> uniqueWords = new HashSet<>();
        
        // Calculate transition probabilities
        for (int i = 0; i < path.size() - 1; i++) {
            Word current = path.get(i);
            Word next = path.get(i + 1);
            Bigram bigram = bigramMap.get(current).get(next);
            
            // Use log probabilities to prevent underflow
            score += Math.log(bigram.getCount() + 1);
            uniqueWords.add(current.getText());
        }
        uniqueWords.add(path.get(path.size() - 1).getText());
        
        // Favor longer sequences up to a point
        double lengthScore = Math.min(10.0, path.size()) * 2.0;
        
        // Reward word variety
        double varietyScore = uniqueWords.size() * 1.5;
        
        // Add some controlled randomness
        double randomFactor = random.nextDouble() * 2.0;
        
        // Combine scores with weights
        return score + lengthScore + varietyScore + randomFactor;
    }
}