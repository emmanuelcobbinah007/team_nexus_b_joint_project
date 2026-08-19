package edu.ug.nexusb.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class KnapsackDPTest {

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void testKnapsackOptimization() {
        KnapsackDP knapsack = new KnapsackDP();

        // Example: 3 service requests with specific resource weights and priority values
        int[] weights = {10, 20, 30};
        int[] values = {60, 100, 120};
        int capacity = 50;

        KnapsackDP.KnapsackResult result = knapsack.solve(weights, values, capacity);

        // Expected max value is 220 (combining item 1 (20w, 100v) and item 2 (30w, 120v))
        assertEquals(220, result.maxValue);

        // Reconstructed solution should contain exactly 2 indices
        int[] selected = result.selectedIndices;
        assertEquals(2, selected.length);

        // Check if indices 1 and 2 are present
        boolean hasOne = false;
        boolean hasTwo = false;
        for (int index : selected) {
            if (index == 1) hasOne = true;
            if (index == 2) hasTwo = true;
        }

        assertTrue(hasOne, "Solution should contain index 1");
        assertTrue(hasTwo, "Solution should contain index 2");
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void emptyItemArraysProduceZeroValueAndNoSelection() {
        KnapsackDP knapsack = new KnapsackDP();

        KnapsackDP.KnapsackResult result = knapsack.solve(new int[0], new int[0], 50);

        assertEquals(0, result.maxValue);
        assertEquals(0, result.selectedIndices.length);
    }

    @Test
    void zeroCapacitySelectsNothingRegardlessOfItems() {
        KnapsackDP knapsack = new KnapsackDP();
        int[] weights = {10, 20, 30};
        int[] values = {60, 100, 120};

        KnapsackDP.KnapsackResult result = knapsack.solve(weights, values, 0);

        assertEquals(0, result.maxValue);
        assertEquals(0, result.selectedIndices.length);
    }

    @Test
    void singleItemThatFitsExactlyIsSelected() {
        KnapsackDP knapsack = new KnapsackDP();

        KnapsackDP.KnapsackResult result = knapsack.solve(new int[] {5}, new int[] {10}, 5);

        assertEquals(10, result.maxValue);
        assertEquals(1, result.selectedIndices.length);
        assertEquals(0, result.selectedIndices[0]);
    }

    @Test
    void singleItemHeavierThanCapacityIsNeverSelected() {
        KnapsackDP knapsack = new KnapsackDP();

        KnapsackDP.KnapsackResult result = knapsack.solve(new int[] {10}, new int[] {5}, 5);

        assertEquals(0, result.maxValue);
        assertEquals(0, result.selectedIndices.length);
    }

    @Test
    void zeroWeightItemIsAlwaysWorthIncluding() {
        KnapsackDP knapsack = new KnapsackDP();

        KnapsackDP.KnapsackResult result = knapsack.solve(new int[] {0, 10}, new int[] {5, 5}, 0);

        // capacity 0 still fits the zero-weight item
        assertEquals(5, result.maxValue);
        assertEquals(1, result.selectedIndices.length);
        assertEquals(0, result.selectedIndices[0]);
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void nullWeightsThrows() {
        KnapsackDP knapsack = new KnapsackDP();
        assertThrows(IllegalArgumentException.class, () -> knapsack.solve(null, new int[] {1}, 10));
    }

    @Test
    void nullValuesThrows() {
        KnapsackDP knapsack = new KnapsackDP();
        assertThrows(IllegalArgumentException.class, () -> knapsack.solve(new int[] {1}, null, 10));
    }

    @Test
    void mismatchedArrayLengthsThrows() {
        KnapsackDP knapsack = new KnapsackDP();
        int[] weights = {1, 2, 3};
        int[] values = {1, 2};
        assertThrows(IllegalArgumentException.class, () -> knapsack.solve(weights, values, 10));
    }

    @Test
    void negativeCapacityThrows() {
        KnapsackDP knapsack = new KnapsackDP();
        int[] weights = {1, 2};
        int[] values = {1, 2};
        assertThrows(IllegalArgumentException.class, () -> knapsack.solve(weights, values, -1));
    }

    @Test
    void negativeWeightThrows() {
        KnapsackDP knapsack = new KnapsackDP();
        int[] weights = {5, -1};
        int[] values = {10, 20};
        assertThrows(IllegalArgumentException.class, () -> knapsack.solve(weights, values, 10));
    }
}