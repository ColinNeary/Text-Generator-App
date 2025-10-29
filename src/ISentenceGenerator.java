public interface ISentenceGenerator {
    /**
     * Generates a sentence starting with the given word
     * @param startWord The word to start the sentence with
     * @return The generated sentence
     */
    String generateSentence(String startWord);
}