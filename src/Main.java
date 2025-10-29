import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create our sentence generators
        SentenceGenerator greedyGenerator = new SentenceGenerator();
        MarkovSentenceGenerator markovGenerator = new MarkovSentenceGenerator();
        BeamSearchSentenceGenerator beamGenerator = new BeamSearchSentenceGenerator();
        
        // Add sample data to all generators
        addSampleData(greedyGenerator);
        addSampleData(markovGenerator);
        addSampleData(beamGenerator);
        
        // Create scanner for user input
        Scanner scanner = new Scanner(System.in);
        
        // Get starting word from user
        System.out.print("Enter a starting word: ");
        String startWord = scanner.nextLine();
        
        // Generate and print sentences using all algorithms
        System.out.println("\nGreedy Algorithm: " + 
            greedyGenerator.generateSentence(startWord));
        System.out.println("Markov Chain: " + 
            markovGenerator.generateSentence(startWord));
        System.out.println("Beam Search: " + 
            beamGenerator.generateSentence(startWord));
    }
    
    private static void addSampleData(AbstractSentenceGenerator generator) {
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
        Word have = new Word("have");
        Word you = new Word("you");
        Word ever = new Word("ever");
        Word seen = new Word("seen");
        Word rainbow = new Word("rainbow");
        Word good = new Word("good");
        Word time = new Word("time");
        // Additional words for more variety
        Word quickly = new Word("quickly");
        Word slowly = new Word("slowly");
        Word jumped = new Word("jumped");
        Word over = new Word("over");
        Word fence = new Word("fence");
        Word through = new Word("through");
        Word garden = new Word("garden");
        Word chased = new Word("chased");
        Word playfully = new Word("playfully");
        Word and = new Word("and");
        Word want = new Word("want");
        Word play = new Word("play");
        Word sleep = new Word("sleep");
        Word eat = new Word("eat");
        Word dinner = new Word("dinner");
        Word lunch = new Word("lunch");
        Word wonderful = new Word("wonderful");
        Word exciting = new Word("exciting");
        Word adventure = new Word("adventure");
        Word walked = new Word("walked");
        
        // Add all words to generator
        for (Word word : new Word[]{the, cat, dog, sat, ran, on, in, mat, park, 
                                  i, love, to, read, books, today, is, a, beautiful, day,
                                  have, you, ever, seen, rainbow, good, time,
                                  quickly, slowly, jumped, over, fence, through, garden,
                                  chased, playfully, and, want, play, sleep, eat,
                                  dinner, lunch, wonderful, exciting, adventure, walked}) {
            generator.addWord(word);
        }

        // Add varied paths for "the"
        addSentenceBigrams(generator, new Word[]{the, cat, jumped, over, the, fence});
        addSentenceBigrams(generator, new Word[]{the, dog, ran, through, the, garden});
        addSentenceBigrams(generator, new Word[]{the, cat, slowly, walked, in, the, park});
        addSentenceBigrams(generator, new Word[]{the, dog, playfully, chased, the, cat});
        
        // Add varied paths for "i"
        addSentenceBigrams(generator, new Word[]{i, love, to, read, books});
        addSentenceBigrams(generator, new Word[]{i, want, to, play, and, sleep});
        addSentenceBigrams(generator, new Word[]{i, have, to, eat, dinner});
        addSentenceBigrams(generator, new Word[]{i, love, a, good, adventure});
        
        // Add varied paths for "today"
        addSentenceBigrams(generator, new Word[]{today, is, a, beautiful, day});
        addSentenceBigrams(generator, new Word[]{today, is, a, wonderful, adventure});
        addSentenceBigrams(generator, new Word[]{today, is, exciting, and, good});
        
        // Add varied paths for "have"
        addSentenceBigrams(generator, new Word[]{have, you, ever, seen, a, rainbow});
        addSentenceBigrams(generator, new Word[]{have, a, good, time});
        addSentenceBigrams(generator, new Word[]{have, a, wonderful, day});
        addSentenceBigrams(generator, new Word[]{have, to, eat, lunch});        // Set start/end counts with more variety
        the.incrementStartCount();
        the.incrementStartCount();
        the.incrementStartCount();
        i.incrementStartCount();
        i.incrementStartCount();
        today.incrementStartCount();
        today.incrementStartCount();
        have.incrementStartCount();
        have.incrementStartCount();
        
        // Set end counts with more variety
        fence.incrementEndCount();
        garden.incrementEndCount();
        park.incrementEndCount();
        cat.incrementEndCount();
        books.incrementEndCount();
        sleep.incrementEndCount();
        dinner.incrementEndCount();
        adventure.incrementEndCount();
        day.incrementEndCount();
        day.incrementEndCount();
        rainbow.incrementEndCount();
        time.incrementEndCount();
        lunch.incrementEndCount();
        
        // Set total counts (simplified for demonstration)
        for (Word word : new Word[]{the, cat, dog, sat, ran, on, in, mat, park, 
                                  i, love, to, read, books, today, is, a, beautiful, day}) {
            word.incrementTotalCount();
        }
        // Add extra count for common words
        the.incrementTotalCount();
        to.incrementTotalCount();
    }
    
    private static void addSentenceBigrams(AbstractSentenceGenerator generator, Word[] words) {
        for (int i = 0; i < words.length - 1; i++) {
            generator.addBigram(words[i], words[i + 1]);
        }
    }
}