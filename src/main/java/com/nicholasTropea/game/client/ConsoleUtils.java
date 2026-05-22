package com.nicholasTropea.game.client;

/**
 * Utility class for terminal formatting and colors using ANSI escape codes.
 */
public class ConsoleUtils {
    // ANSI Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String BOLD = "\u001B[1m";

    /** Prints a formatted section title. */
    public static void printSectionTitle(String title) {
        System.out.println("\n" + BOLD + CYAN + "╔" + "═".repeat(title.length() + 2) + "╗" + RESET);
        System.out.println(BOLD + CYAN + "║ " + title.toUpperCase() + " ║" + RESET);
        System.out.println(BOLD + CYAN + "╚" + "═".repeat(title.length() + 2) + "╝" + RESET);
    }

    /** Prints a visual separator. */
    public static void printSeparator() {
        System.out.println(CYAN + "╶".repeat(60) + RESET);
    }

    /** Prints a success message in green. */
    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }

    /** Prints an error message in red. */
    public static void printError(String message) {
        System.out.println(RED + "✘ ERROR: " + message + RESET);
    }

    /** Prints an info/notification message in yellow. */
    public static void printInfo(String message) {
        System.out.println(YELLOW + "ℹ " + message + RESET);
    }

    /** Prints a 16-word grid (4x4). */
    public static void printWordGrid(java.util.List<String> words) {
        if (words == null || words.isEmpty()) return;
        
        printSeparator();
        for (int i = 0; i < words.size(); i++) {
            System.out.printf(BOLD + "%-14s" + RESET, words.get(i));
            if ((i + 1) % 4 == 0) System.out.println();
        }
        printSeparator();
    }

    /** Clears the current line (approximated for most terminals). */
    public static void clearLine() {
        System.out.print("\r" + " ".repeat(80) + "\r");
    }
}
