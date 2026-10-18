// Written by Kai Chang, cxc210040

package com.example.backend;

import java.sql.*;
import java.util.*;


// Database manager for word corpus, POS tags, and word pair probabilities
public class DatabaseManager {
    private static final String URL = DbConfig.URL;
    private static final String USER = DbConfig.USER;
    private static final String PASSWORD = DbConfig.PASSWORD;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            initializeSchema();
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) connection = DriverManager.getConnection(URL, USER, PASSWORD);
        return connection;
    }

    // Creates tables and adds POS column if missing
    private void initializeSchema() throws SQLException {
        executeUpdate("CREATE TABLE IF NOT EXISTS grammar_rules (left_tag VARCHAR(20), right_tag VARCHAR(20), occurrence INT, PRIMARY KEY (left_tag, right_tag))");
        try { executeQuery("SELECT pos_tag FROM words LIMIT 1"); }
        catch (SQLException e) { executeUpdate("ALTER TABLE words ADD COLUMN pos_tag VARCHAR(20) DEFAULT 'NOUN'"); }
    }

    // Hardcoded dictionary for 500+ common words across POS categories
    private static final Map<String, String> KNOWN_WORDS = new HashMap<>();

    static {
        // DETERMINERS
        for (String w : Arrays.asList("the", "a", "an", "this", "that", "these", "those", "my", "your", "his", "her", "their", "our", "all", "some", "no", "any", "every", "one", "two", "three", "first", "second", "next", "other", "another", "such", "own", "whose", "each", "either", "neither", "both", "few", "many", "several", "most", "much", "more", "less")) KNOWN_WORDS.put(w, "DET");

        // PREPOSITIONS
        for (String w : Arrays.asList("in", "on", "at", "by", "for", "with", "about", "to", "from", "of", "into", "over", "after", "before", "without", "against", "under", "between", "towards", "through", "upon", "within", "during", "till", "until", "towards", "among", "beyond", "beneath", "across")) KNOWN_WORDS.put(w, "PREP");

        // PRONOUNS
        for (String w : Arrays.asList("i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "who", "whom", "myself", "himself", "herself", "themselves", "yourself", "itself", "ourselves", "what", "which", "whose")) KNOWN_WORDS.put(w, "PRON");

        // CONJUNCTIONS
        for (String w : Arrays.asList("and", "but", "or", "nor", "for", "yet", "so", "because", "although", "if", "when", "while", "since", "unless", "where", "as", "than", "whether", "though", "whenever", "wherever")) KNOWN_WORDS.put(w, "CONJ");

        // MODAL VERBS
        for (String w : Arrays.asList("can", "could", "will", "would", "shall", "should", "may", "might", "must")) KNOWN_WORDS.put(w, "VERB_MODAL");

        // AUXILIARY VERBS
        for (String w : Arrays.asList("is", "am", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing")) KNOWN_WORDS.put(w, "VERB_AUX");

        // VERB_BASE: Present tense
        for (String w : Arrays.asList("go", "come", "say", "see", "think", "make", "take", "know", "give", "find", "tell", "ask", "feel", "leave", "call", "look", "seem", "believe", "appear", "wish", "hope", "love", "play", "want", "need", "try", "use", "work", "show", "hear", "let", "put", "mean", "keep", "begin", "start", "help", "talk", "turn", "bring", "like", "provide", "sit", "stand", "lose", "pay", "meet", "include", "continue", "set", "learn", "lead", "understand", "watch", "follow", "stop", "create", "speak", "read", "allow", "add", "spend", "grow", "open", "walk", "win", "offer", "remember", "consider", "appear", "buy", "wait", "serve", "die", "send", "expect", "build", "stay", "fall", "cut", "reach", "kill", "remain", "suggest", "raise", "pass", "sell", "require", "report", "decide", "pull")) KNOWN_WORDS.put(w, "VERB_BASE");

        // VERB_TRANS: Transitive past tense (needs object)
        for (String w : Arrays.asList("saw", "took", "knew", "found", "told", "asked", "felt", "left", "called", "believed", "wished", "loved", "heard", "met", "paid", "gave", "made", "seen", "done", "known", "played", "wanted", "needed", "used", "showed", "meant", "kept", "helped", "brought", "liked", "provided", "lost", "included", "learned", "understood", "followed", "created", "read", "allowed", "added", "spent", "opened", "offered", "remembered", "considered", "bought", "served", "sent", "expected", "built", "raised", "sold", "required", "reported", "decided", "received", "continued")) KNOWN_WORDS.put(w, "VERB_TRANS");

        // VERB_INTR: Intransitive past tense (no object needed)
        for (String w : Arrays.asList("went", "came", "looked", "seemed", "appeared", "replied", "cried", "returned", "spoke", "sat", "stood", "ran", "walked", "talked", "stayed", "happened", "began", "turned", "said", "arrived", "departed", "remained", "occurred", "existed", "lived", "died", "fell", "rose", "slept", "woke", "laughed", "smiled", "waited", "worked", "grown", "changed")) KNOWN_WORDS.put(w, "VERB_INTR");

        // ADJECTIVES
        for (String w : Arrays.asList("good", "great", "bad", "happy", "sad", "young", "old", "new", "long", "short", "little", "much", "dear", "fine", "true", "poor", "rich", "handsome", "beautiful", "possible", "impossible", "certain", "sure", "ready", "afraid", "glad", "sorry", "able", "accomplished", "large", "small", "high", "low", "early", "late", "full", "empty", "strong", "weak", "quick", "slow", "right", "wrong", "easy", "hard", "different", "same", "few", "many", "whole", "particular", "general", "own", "other", "such", "last", "next", "past", "present", "future", "better", "worse", "best", "worst", "less", "more", "most", "greater", "greatest", "younger", "older", "elder", "latter", "former", "proper", "civil", "pleasant", "delightful", "agreeable", "excellent", "perfect", "comfortable", "happy", "unhappy", "satisfied", "dissatisfied")) KNOWN_WORDS.put(w, "ADJ");

        // ADVERBS
        for (String w : Arrays.asList("very", "too", "quite", "rather", "really", "so", "not", "never", "always", "often", "soon", "now", "then", "here", "there", "well", "highly", "hardly", "scarcely", "just", "even", "only", "perhaps", "yes", "indeed", "however", "away", "again", "ever", "still", "already", "yet", "thus", "therefore", "hence", "accordingly", "consequently", "moreover", "furthermore", "nevertheless", "nonetheless", "otherwise", "meanwhile", "instead", "particularly", "especially", "extremely", "entirely", "completely", "absolutely", "perfectly", "nearly", "almost", "barely", "merely", "simply", "certainly", "surely", "truly", "really", "actually", "probably", "possibly", "naturally", "fortunately", "unfortunately", "happily", "sadly")) KNOWN_WORDS.put(w, "ADV");

        // COMMON NOUNS
        for (String w : Arrays.asList("nothing", "something", "anything", "everything", "man", "woman", "father", "mother", "sister", "brother", "son", "daughter", "time", "day", "year", "week", "month", "morning", "evening", "night", "letter", "room", "house", "door", "window", "table", "chair", "book", "paper", "hand", "eye", "face", "head", "heart", "mind", "life", "world", "way", "manner", "thing", "place", "part", "side", "end", "moment", "hour", "word", "question", "answer", "reason", "person", "people", "family", "friend", "company", "conversation", "subject", "opinion", "thought", "feeling", "pleasure", "pain", "wish", "hope", "fear", "love", "hate", "attention", "interest", "concern", "business", "matter", "affair", "situation", "circumstance", "condition", "state", "case", "event", "occasion", "opportunity", "advantage", "disadvantage", "power", "ability", "fortune", "money", "property", "character", "disposition", "temper", "spirit", "nature")) KNOWN_WORDS.put(w, "NOUN");

        // PROPER NOUNS (Jane Austen specific)
        for (String w : Arrays.asList("mr", "mrs", "miss", "elizabeth", "darcy", "jane", "bennet", "bingley", "wickham", "collins", "longbourn", "netherfield", "pemberley", "london", "sir", "lady", "catherine", "de", "bourgh", "fitzwilliam", "georgiana", "lydia", "kitty", "mary", "charlotte", "lucas", "forster", "william", "gardiner", "phillips", "meryton", "derbyshire", "kent", "hertfordshire", "rosings")) KNOWN_WORDS.put(w, "NOUN");
    }

    public static class StyleProfile {
        public final int avgSentenceLength;
        public final double avgWordComplexity;
        public StyleProfile(int len, double comp) { this.avgSentenceLength = len; this.avgWordComplexity = comp; }
    }

    public StyleProfile getStyleProfile() throws SQLException {
        int avgLen = 10;
        double avgComp = 5.0;
        try (ResultSet rs = executeQuery("SELECT SUM(word_count_total) / NULLIF(SUM(word_count_starting), 0) as avg_len FROM words")) {
            if (rs.next() && rs.getDouble("avg_len") > 0) avgLen = rs.getInt("avg_len");
        }
        try (ResultSet rs = executeQuery("SELECT AVG(LENGTH(word_text)) as avg_comp FROM words WHERE pos_tag NOT IN ('DET', 'PREP', 'PRON', 'CONJ')")) {
            if (rs.next() && rs.getDouble("avg_comp") > 0) avgComp = rs.getDouble("avg_comp");
        }
        return new StyleProfile(avgLen, avgComp);
    }


    // Multi-step POS tagging: dictionary → suffix patterns → statistical context
    public void assignStatisticalRoles() throws SQLException {
        System.out.println("Applying Advanced POS Tagging...");

        // Step 1: Default everything to NOUN
        executeUpdate("UPDATE words SET pos_tag = 'NOUN'");

        // Step 2: Apply known dictionary
        connection.setAutoCommit(false);
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE words SET pos_tag = ? WHERE word_text = ?")) {
            for (Map.Entry<String, String> entry : KNOWN_WORDS.entrySet()) {
                stmt.setString(1, entry.getValue());
                stmt.setString(2, entry.getKey());
                stmt.addBatch();
            }
            stmt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }

        System.out.println("✓ Dictionary applied (500+ words)");

        // Step 3: Suffix-based heuristics for unknown words
        executeUpdate("UPDATE words SET pos_tag = 'VERB_TRANS' WHERE word_text LIKE '%ed' AND pos_tag = 'NOUN' AND word_text NOT LIKE '%eed'");
        executeUpdate("UPDATE words SET pos_tag = 'VERB_ING' WHERE word_text LIKE '%ing' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'ADV' WHERE word_text LIKE '%ly' AND pos_tag = 'NOUN' AND LENGTH(word_text) > 4");
        executeUpdate("UPDATE words SET pos_tag = 'ADJ' WHERE word_text LIKE '%ful' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'ADJ' WHERE word_text LIKE '%less' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'ADJ' WHERE word_text LIKE '%ous' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'ADJ' WHERE word_text LIKE '%ive' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'ADJ' WHERE word_text LIKE '%able' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'ADJ' WHERE word_text LIKE '%ible' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'NOUN' WHERE word_text LIKE '%tion' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'NOUN' WHERE word_text LIKE '%ness' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'NOUN' WHERE word_text LIKE '%ment' AND pos_tag = 'NOUN'");
        executeUpdate("UPDATE words SET pos_tag = 'NOUN' WHERE word_text LIKE '%ity' AND pos_tag = 'NOUN'");

        System.out.println("✓ Suffix patterns applied");

        // Step 4: Statistical context analysis (MySQL-compatible version)
        // Words that frequently follow determiners are likely NOUNS
        executeUpdate(
                "UPDATE words w " +
                        "JOIN (SELECT wp.right_word, COUNT(*) as det_count " +
                        "      FROM word_pairs wp " +
                        "      JOIN words w2 ON wp.left_word = w2.word_text " +
                        "      WHERE w2.pos_tag = 'DET' " +
                        "      GROUP BY wp.right_word " +
                        "      HAVING det_count > 3) det_followers " +
                        "ON w.word_text = det_followers.right_word " +
                        "SET w.pos_tag = 'NOUN' " +
                        "WHERE w.pos_tag = 'NOUN'"
        );

        // Words that frequently follow pronouns are likely VERBS
        executeUpdate(
                "UPDATE words w " +
                        "JOIN (SELECT wp.right_word, COUNT(*) as pron_count " +
                        "      FROM word_pairs wp " +
                        "      JOIN words w2 ON wp.left_word = w2.word_text " +
                        "      WHERE w2.pos_tag = 'PRON' " +
                        "      GROUP BY wp.right_word " +
                        "      HAVING pron_count > 2) pron_followers " +
                        "ON w.word_text = pron_followers.right_word " +
                        "SET w.pos_tag = 'VERB_TRANS' " +
                        "WHERE w.pos_tag = 'NOUN'"
        );

        // Words that frequently precede nouns are likely ADJECTIVES
        executeUpdate(
                "UPDATE words w " +
                        "JOIN (SELECT wp.left_word, COUNT(*) as noun_count " +
                        "      FROM word_pairs wp " +
                        "      JOIN words w2 ON wp.right_word = w2.word_text " +
                        "      WHERE w2.pos_tag = 'NOUN' " +
                        "      GROUP BY wp.left_word " +
                        "      HAVING noun_count > 3) noun_preceders " +
                        "ON w.word_text = noun_preceders.left_word " +
                        "SET w.pos_tag = 'ADJ' " +
                        "WHERE w.pos_tag = 'NOUN' AND LENGTH(w.word_text) > 4"
        );

        System.out.println("✓ Statistical context analysis complete");

        // Step 5: Identify START words (sentence beginners)
        executeUpdate("UPDATE words SET pos_tag = 'START' WHERE (word_count_starting * 1.0 / word_count_total) > 0.15 AND pos_tag IN ('NOUN', 'PRON', 'DET', 'ADV', 'CONJ')");

        // Step 6: Identify END words (sentence enders)
        executeUpdate("UPDATE words SET pos_tag = 'END' WHERE (word_count_ending * 1.0 / word_count_total) > 0.35 AND pos_tag NOT IN ('DET', 'PREP', 'VERB_MODAL', 'VERB_BASE', 'ADJ', 'CONJ')");

        System.out.println("✓ START/END markers assigned");
        System.out.println("=================================");
        System.out.println("Training Complete - POS Tags Assigned!");
    }

    public void learnGrammarRules() throws SQLException {
        executeUpdate("TRUNCATE TABLE grammar_rules");
    }

    // ==================== QUERY METHODS ====================

    public ResultSet executeQuery(String query, Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(query);
        for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
        return stmt.executeQuery();
    }
    public int executeUpdate(String query, Object... params) throws SQLException {
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            return stmt.executeUpdate();
        }
    }

    // Returns POS tag from dictionary or database
    public String getPOSTag(String word) throws SQLException {
        if (KNOWN_WORDS.containsKey(word)) return KNOWN_WORDS.get(word);
        try (ResultSet rs = executeQuery("SELECT pos_tag FROM words WHERE word_text = ?", word)) {
            if (rs.next()) return rs.getString("pos_tag");
        }
        return "NOUN";
    }

    // Gets next words with probabilities from corpus
    public List<WordPair> getNextWords(String currentWord, int limit) throws SQLException {
        List<WordPair> pairs = new ArrayList<>();
        try (ResultSet rs = executeQuery("SELECT wp.left_word, wp.right_word, wp.pair_count, (wp.pair_count * 1.0 / w.word_count_total) as probability FROM word_pairs wp JOIN words w ON w.word_text = wp.left_word WHERE wp.left_word = ? ORDER BY probability DESC LIMIT ?", currentWord, limit)) {
            while (rs.next()) pairs.add(new WordPair(rs.getString("left_word"), rs.getString("right_word"), rs.getInt("pair_count"), rs.getDouble("probability")));
        }
        return pairs;
    }
    public List<String> getWordsByTag(String tag, int limit) throws SQLException {
        List<String> words = new ArrayList<>();
        try (ResultSet rs = executeQuery("SELECT word_text FROM words WHERE pos_tag = ? ORDER BY word_count_total DESC LIMIT ?", tag, limit)) {
            while (rs.next()) words.add(rs.getString("word_text"));
        }
        return words;
    }
    public boolean wordExists(String word) throws SQLException {
        try (ResultSet rs = executeQuery("SELECT 1 FROM words WHERE word_text = ?", word)) { return rs.next(); }
    }
    public List<String> getWordsByPrefix(String prefix, int limit) throws SQLException {
        List<String> words = new ArrayList<>();
        try (ResultSet rs = executeQuery("SELECT word_text FROM words WHERE word_text LIKE ? ORDER BY word_count_total DESC LIMIT ?", prefix + "%", limit)) {
            while (rs.next()) words.add(rs.getString("word_text"));
        }
        return words;
    }
    public int getTotalWordCount() throws SQLException {
        try (ResultSet rs = executeQuery("SELECT COUNT(*) FROM words")) { if (rs.next()) return rs.getInt(1); } return 0;
    }
    public void close() { try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException e) { e.printStackTrace(); } }

    // Data carrier for word pair with probability
    public static class WordPair {
        private final String leftWord, rightWord; private final int pairCount; private final double probability;
        public WordPair(String l, String r, int c, double p) { this.leftWord=l; this.rightWord=r; this.pairCount=c; this.probability=p; }
        public String getLeftWord() { return leftWord; }
        public String getRightWord() { return rightWord; }
        public double getProbability() { return probability; }
        public int getPairCount() { return pairCount; }
    }
}