package edu.ug.nexusb.optimization;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class KnapsackDPTest {

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
        
        // Reconstructed solution should contain indices 1 and 2
        List<Integer> selected = result.selectedIndices;
        assertEquals(2, selected.size());
        assertTrue(selected.contains(1));
        assertTrue(selected.contains(2));
    }
}