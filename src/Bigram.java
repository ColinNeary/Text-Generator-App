public class Bigram {
    private Word prevWord;
    private Word nextWord;
    private int count;
    
    public Bigram(Word prevWord, Word nextWord) {
        this.prevWord = prevWord;
        this.nextWord = nextWord;
        this.count = 0;
    }
    
    public Word getPrevWord() { return prevWord; }
    public Word getNextWord() { return nextWord; }
    
    public int getCount() { return count; }
    public void incrementCount() { this.count++; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bigram bigram = (Bigram) o;
        return prevWord.equals(bigram.prevWord) && nextWord.equals(bigram.nextWord);
    }
    
    @Override
    public int hashCode() {
        return 31 * prevWord.hashCode() + nextWord.hashCode();
    }
}