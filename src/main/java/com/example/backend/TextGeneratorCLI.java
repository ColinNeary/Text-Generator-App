// Written by Kai Chang, cxc210040

package com.example.backend;

import com.example.backend.sentencegen.GreedySentenceGenerator;
import com.example.backend.sentencegen.BeamSearchSentenceGenerator; // UPDATED IMPORT
import java.sql.SQLException;
import java.util.Scanner;

public class TextGeneratorCLI {
    private static final Scanner scanner = new Scanner(System.in);
    private static GreedySentenceGenerator greedyGen;
    private static BeamSearchSentenceGenerator beamGen; // UPDATED VAR NAME
    private static boolean debugMode = false;

    public static void main(String[] args) {
        try {
            System.out.println("Initializing Sentence Generator...");
            initializeComponents();
            boolean running = true;
            while (running) {
                running = showMainMenu();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void initializeComponents() throws SQLException {
        greedyGen = new GreedySentenceGenerator(10, debugMode);
        beamGen = new BeamSearchSentenceGenerator(10, 12, debugMode); // UPDATED
    }

    private static boolean showMainMenu() throws SQLException {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Generate Sentence (Compare Algorithms)");
        System.out.println("2. Show Database Stats");
        System.out.println("7. TRAIN MODEL (Calculate Roles & Grammar)");
        System.out.println("8. Toggle Debug Mode (" + debugMode + ")");
        System.out.println("9. Exit");
        System.out.print("Choice: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1": compareGenerators(); break;
            case "2": showStats(); break;
            case "7": trainModel(); break;
            case "8": debugMode = !debugMode; initializeComponents(); break;
            case "9": return false;
        }
        return true;
    }

    private static void trainModel() throws SQLException {
        System.out.println("\n--- TRAINING MODEL ---");
        System.out.println("This will analyze all words in the DB to assign roles.");
        DatabaseManager.getInstance().assignStatisticalRoles();

        // Re-init generator
        beamGen = new BeamSearchSentenceGenerator(10, 12, debugMode);
        System.out.println("Training Complete.");
    }

    private static void compareGenerators() throws SQLException {
        System.out.print("Enter start word: ");
        String word = scanner.nextLine();
        if(word.trim().isEmpty()) return;

        System.out.println("\n--- GREEDY RESULT ---");
        GreedySentenceGenerator.SentenceResult res1 = greedyGen.generateSentence(word);
        System.out.println(res1.getSentence());
        System.out.println("Score: " + res1.getScore());

        System.out.println("\n--- BEAM SEARCH RESULT ---"); // UPDATED LABEL
        BeamSearchSentenceGenerator.SentenceResult res2 = beamGen.generateSentence(word);
        System.out.println(res2.getSentence());
        System.out.println("Score: " + res2.getScore());
    }

    private static void showStats() throws SQLException {
        System.out.println("Total Words: " + DatabaseManager.getInstance().getTotalWordCount());
    }
}