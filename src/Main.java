import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create our sentence generator
        SentenceGenerator generator = new SentenceGenerator();
        
        // Add some sample data
        addSampleData(generator);
        
        // Create scanner for user input
        Scanner scanner = new Scanner(System.in);
        
        // Get starting word from user
        System.out.print("Enter a starting word: ");
        String startWord = scanner.nextLine();
        
        // Generate and print the sentence
        System.out.println("Generated sentence: " + generator.generateSentence(startWord));
    }
    
    private static void addSampleData(SentenceGenerator generator) {
        // Create some words with their statistics
        Word the = new Word("the");
        Word cat = new Word("cat");
        Word dog = new Word("dog");
        Word sat = new Word("sat");
        Word ran = new Word("ran");
        Word on = new Word("on");
        Word in = new Word("in");
        Word mat = new Word("mat");
        Word park = new Word("park");
        Word i = new Word("i");
        Word love = new Word("love");
        Word to = new Word("to");
        Word read = new Word("read");
        Word books = new Word("books");
        Word today = new Word("today");
        Word is = new Word("is");
        Word a = new Word("a");
        Word beautiful = new Word("beautiful");
        Word day = new Word("day");
        // New words for "have" sentences
        Word have = new Word("have");
        Word you = new Word("you");
        Word ever = new Word("ever");
        Word seen = new Word("seen");
        Word rainbow = new Word("rainbow");
        Word good = new Word("good");
        Word time = new Word("time");
        
        // Add words to generator
        for (Word word : new Word[]{the, cat, dog, sat, ran, on, in, mat, park, 
                                  i, love, to, read, books, today, is, a, beautiful, day,
                                  have, you, ever, seen, rainbow, good, time}) {
            generator.addWord(word);
        }
        
        // Add bigrams with their frequencies
        addSentenceBigrams(generator, new Word[]{the, cat, sat, on, the, mat});
        addSentenceBigrams(generator, new Word[]{the, dog, ran, in, the, park});
        addSentenceBigrams(generator, new Word[]{i, love, to, read, books});
        addSentenceBigrams(generator, new Word[]{today, is, a, beautiful, day});
        addSentenceBigrams(generator, new Word[]{have, you, ever, seen, a, rainbow});
        addSentenceBigrams(generator, new Word[]{have, a, good, time});
        
        // Set start/end counts
        the.incrementStartCount();
        the.incrementStartCount();
        i.incrementStartCount();
        today.incrementStartCount();
        have.incrementStartCount();
        have.incrementStartCount();
        
        mat.incrementEndCount();
        park.incrementEndCount();
        books.incrementEndCount();
        day.incrementEndCount();
        rainbow.incrementEndCount();
        time.incrementEndCount();
        
        // Set total counts (simplified for demonstration)
        for (Word word : new Word[]{the, cat, dog, sat, ran, on, in, mat, park, 
                                  i, love, to, read, books, today, is, a, beautiful, day}) {
            word.incrementTotalCount();
        }
        // Add extra count for common words
        the.incrementTotalCount();
        to.incrementTotalCount();
    }
    
    private static void addSentenceBigrams(SentenceGenerator generator, Word[] words) {
        for (int i = 0; i < words.length - 1; i++) {
            generator.addBigram(words[i], words[i + 1]);
        }
    }
}