package edu.ug.nexusb.optimization;

/**
 * Dynamic Programming implementation of the 0/1 Knapsack Problem for 
 * resource optimization in the Ghana Smart Service Operations Optimizer.
 * 
 * @author Johnson Kuzagbe
 */
public class KnapsackDP {

    /**
     * Data class to hold the results of the DP Knapsack algorithm,
     * including the trace table and the reconstructed solution.
     */
    public static class KnapsackResult {
        public final int maxValue;
        public final int[][] dpTable;
        public final int[] selectedIndices; // Changed from List<Integer> to int[]

        public KnapsackResult(int maxValue, int[][] dpTable, int[] selectedIndices) {
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
     * @throws IllegalArgumentException if {@code weights} or {@code values} is null, they
     *         differ in length, {@code capacity} is negative, or any weight is negative
     *         (a negative weight would push the DP table index below zero)
     */
    public KnapsackResult solve(int[] weights, int[] values, int capacity) {
        if (weights == null || values == null) {
            throw new IllegalArgumentException("weights and values must not be null");
        }
        if (weights.length != values.length) {
            throw new IllegalArgumentException("weights and values must have the same length");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        for (int weight : weights) {
            if (weight < 0) {
                throw new IllegalArgumentException("weights must not be negative");
            }
        }

        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];

        // 1. Build the DP table (Tabulation)
        for (int i = 0; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                if (i == 0) {
                    // w == 0 is not its own base case here: a zero-weight item must
                    // still be eligible for inclusion at w == 0 via the branch below.
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
        
        // We use a temporary array of max possible size 'n' to store selected items
        int[] tempSelected = new int[n];
        int count = 0;

        for (int i = n; i > 0 && res > 0; i--) {
            // If the value didn't come from the row above, it means this item was included
            if (res != dp[i - 1][w]) {
                tempSelected[count++] = i - 1; // Store the index of the selected item
                res = res - values[i - 1];
                w = w - weights[i - 1];
            }
        }
        
        // Trim the array to the exact number of selected items
        int[] finalSelected = new int[count];
        System.arraycopy(tempSelected, 0, finalSelected, 0, count);

        return new KnapsackResult(dp[n][capacity], dp, finalSelected);
    }
}