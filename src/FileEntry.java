import java.time.LocalDateTime;

public class FileEntry {
    private Long id;
    private String path;
    private String title;
    private int wordCount;
    private LocalDateTime importedAt;
    
    public FileEntry(String path, String title) {
        this.path = path;
        this.title = title;
        this.wordCount = 0;
        this.importedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void incrementWordCount() { this.wordCount++; }
    
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
}