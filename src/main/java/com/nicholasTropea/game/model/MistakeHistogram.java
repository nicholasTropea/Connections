package com.nicholasTropea.game.model;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/** Rappresenta l'istogramma delle partite di un giocatore */
public class MistakeHistogram {
    /** Mappa chiave-valore delle partite */
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

        // Parte verticale
        for (int i = maxVal; i >= 0; i--) {
            System.out.printf("%2d |  ", i);

            // Una colonna di larghezza prefissata (12 caratteri) per chiave
            for (Map.Entry<String, Integer> e : this.values.entrySet()) {
                int val = e.getValue();

                // x centrata ad occhio
                if (val >= i) System.out.print("  #         ");
                else System.out.print("            ");
            }

            System.out.println();
        }

        // Parte orizzontale
        System.out.println("---------------------------------------------------------------------------");
        System.out.print("     ");

        for (Map.Entry<String, Integer> e : this.values.entrySet()) {
            String key = e.getKey();
            System.out.printf("%-12s", key);
        }

        System.out.println();
    }
}