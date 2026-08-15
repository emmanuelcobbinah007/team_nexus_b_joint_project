package edu.ug.nexusb.optimization;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Programming implementation of the 0/1 Knapsack Problem for 
 * resource optimization in the Ghana Smart Service Operations Optimizer.
 * 
 * @author Johnson Kuzagbr
 */
public class KnapsackDP {

    /**
     * Data class to hold the results of the DP Knapsack algorithm,
     * including the trace table and the reconstructed solution.
     */
    public static class KnapsackResult {
        public final int maxValue;
        public final int[][] dpTable;
        public final List<Integer> selectedIndices;

        public KnapsackResult(int maxValue, int[][] dpTable, List<Integer> selectedIndices) {
            this.maxValue = maxValue;
            this.dpTable = dpTable;
            this.selectedIndices = selectedIndices;
        }
    }

    /**
     * Solves the 0/1 Knapsack problem using tabulation and reconstructs the chosen items.
     * 
     * @param weights Array of weights (e.g., time/capacity required)
     * @param values Array of values (e.g., priority/urgency level)
     * @param capacity The maximum capacity constraint (e.g., ambulance capacity or budget)
     * @return KnapsackResult containing the max value, tabulation table, and chosen items
     */
    public KnapsackResult solve(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];

        // 1. Build the DP table (Tabulation)
        for (int i = 0; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                if (i == 0 || w == 0) {
                    dp[i][w] = 0;
                } else if (weights[i - 1] <= w) {
                    dp[i][w] = Math.max(values[i - 1] + dp[i - 1][w - weights[i - 1]], dp[i - 1][w]);
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        // 2. Solution Reconstruction (Backtracking through the table)
        int res = dp[n][capacity];
        int w = capacity;
        List<Integer> selected = new ArrayList<>();

        for (int i = n; i > 0 && res > 0; i--) {
            // If the value didn't come from the row above, it means this item was included
            if (res != dp[i - 1][w]) {
                selected.add(i - 1); // Store the index of the selected item
                res = res - values[i - 1];
                w = w - weights[i - 1];
            }
        }

        return new KnapsackResult(dp[n][capacity], dp, selected);
    }
}