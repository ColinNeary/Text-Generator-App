public class Word {
    private Long id;
    private String text;
    private int totalCount;
    private int startCount;
    private int endCount;
    
    public Word(String text) {
        this.text = text.toLowerCase(); // Always store lowercase
        this.totalCount = 0;
        this.startCount = 0;
        this.endCount = 0;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getText() { return text; }
    
    public int getTotalCount() { return totalCount; }
    public void incrementTotalCount() { this.totalCount++; }
    
    public int getStartCount() { return startCount; }
    public void incrementStartCount() { this.startCount++; }
    
    public int getEndCount() { return endCount; }
    public void incrementEndCount() { this.endCount++; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word word = (Word) o;
        return text.equals(word.text);
    }
    
    @Override
    public int hashCode() {
        return text.hashCode();
    }
}