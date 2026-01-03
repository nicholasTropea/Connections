package com.nicholasTropea.game.test;

import com.nicholasTropea.game.model.MistakeHistogram;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class HistogramTest {
    @Test
    void testHistogramCreationAndPrint() {
        MistakeHistogram hist = new MistakeHistogram(10, 2, 9, 3, 7, 12);
        hist.print();

        assertNotNull(hist);
    }
}