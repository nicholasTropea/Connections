package com.nicholasTropea.game.model;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import com.google.gson.annotations.SerializedName;

/** Rappresenta l'istogramma delle partite di un giocatore */
public class MistakeHistogram {
    /** Mappa chiave-valore delle partite */
    @SerializedName("values")
    private final Map<String, Integer> values;

    /** Costruttore */
    public MistakeHistogram(
        int perfect,
        int oneMistake,
        int twoMistakes,
        int threeMistakes,
        int failed,
        int unfinished
    ) {
        if (
            perfect < 0 ||
            oneMistake < 0 ||
            twoMistakes < 0 ||
            threeMistakes < 0 ||
            failed < 0 ||
            unfinished < 0
        ) { throw new IllegalArgumentException("Values must be >= 0"); }

        this.values = new LinkedHashMap<String, Integer>();
        this.values.put("Perfect", perfect);
        this.values.put("1 Mistake", oneMistake);
        this.values.put("2 Mistakes", twoMistakes);
        this.values.put("3 Mistakes", threeMistakes);
        this.values.put("Failed", failed);
        this.values.put("Unfinished", unfinished);
    }

    /** Stampa l'istogramma */
    public void print() {
        int maxVal = Collections.max(this.values.values());
        int columnWidth = computeColumnWidth();

        if (maxVal == 0) {
            System.out.println("No games recorded yet.");
            return;
        }

        // Vertical bars
        for (int i = maxVal; i >= 1; i--) {
            System.out.printf("%2d |  ", i);

            for (Map.Entry<String, Integer> e : this.values.entrySet()) {
                int val = e.getValue();
                System.out.print(centeredCell(val >= i, columnWidth));
            }

            System.out.println();
        }

        // Horizontal axis
        int separatorLength = 6 + (this.values.size() * columnWidth);
        System.out.println("-".repeat(separatorLength));
        System.out.print("      ");

        for (Map.Entry<String, Integer> e : this.values.entrySet()) {
            String key = e.getKey();
            System.out.print(centerText(key, columnWidth));
        }

        System.out.println();
        printCountsLegend();
    }


    private void printCountsLegend() {
        System.out.println();
        System.out.println("Counts:");
        for (Map.Entry<String, Integer> entry : this.values.entrySet()) {
            System.out.printf("  %-12s : %d%n", entry.getKey(), entry.getValue());
        }
    }


    private int computeColumnWidth() {
        int maxLabelLength = 0;
        for (String key : this.values.keySet()) {
            if (key.length() > maxLabelLength) {
                maxLabelLength = key.length();
            }
        }

        return Math.max(12, maxLabelLength + 2);
    }


    private String centeredCell(boolean filled, int width) {
        if (!filled) {
            return " ".repeat(width);
        }

        int leftPadding = (width - 1) / 2;
        int rightPadding = width - leftPadding - 1;
        return " ".repeat(leftPadding) + "#" + " ".repeat(rightPadding);
    }


    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }

        int leftPadding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}