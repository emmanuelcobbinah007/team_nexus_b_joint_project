package edu.ug.nexusb.bench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseBenchmark implements Benchmark {

    // Update this to match the actual SQLite database file name created by Sub-team A
    private String dbUrl = "jdbc:sqlite:nexus.db";

    public DatabaseBenchmark() {}

    /**
     * Construct with custom JDBC URL (useful for tests or alternative DBs)
     */
    public DatabaseBenchmark(String dbUrl) {
        this.dbUrl = dbUrl;
    }
    
    private Runnable currentAlgorithm;

    /**
     * Sets the algorithm logic to be executed during the measurement.
     * Must be called before measure().
     */
    public void setAlgorithm(Runnable currentAlgorithm) {
        this.currentAlgorithm = currentAlgorithm;
    }

    @Override
    public BenchmarkResult measure(String algorithmName, int inputSize, int trialCount) {
        if (currentAlgorithm == null) {
            throw new IllegalStateException("Algorithm Runnable is not set. Call setAlgorithm() first.");
        }

        long totalTimeNs = 0;
        long totalMemoryKb = 0;

        for (int i = 0; i < trialCount; i++) {
            // Suggest garbage collection before each run to normalize memory baseline
            System.gc();
            long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.nanoTime();

            currentAlgorithm.run();

            long endTime = System.nanoTime();
            long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            totalTimeNs += (endTime - startTime);
            // Convert bytes to Kilobytes and ensure no negative anomalies
            totalMemoryKb += Math.max(0, (endMem - startMem) / 1024); 
        }

        long avgTimeNs = totalTimeNs / trialCount;
        long avgMemoryKb = totalMemoryKb / trialCount;

        BenchmarkResult result = new BenchmarkResult(algorithmName, inputSize, avgTimeNs, avgMemoryKb);
        writeToDatabase(result);
        
        return result;
    }

    /**
     * Writes the benchmark result to the {@code algorithm_run} table
     * (singular — {@code data/schema.sql} is the source of truth for the
     * name and columns, not this class).
     *
     * <p>{@code repetition} is hardcoded to 1: {@link #measure} already
     * averages over {@code trialCount} internal runs into one {@link
     * BenchmarkResult}, so one call here produces one row. The schema's
     * {@code repetition} column is really meant for recording each
     * repetition as its own row (see {@code docs/interfaces.md}'s "three
     * repetitions recorded individually rather than pre-averaged, so the
     * report can show variance") — {@code measure}'s aggregate-then-write
     * shape doesn't support that yet. Real gap, not fixed here.
     */
    private void writeToDatabase(BenchmarkResult result) {
        String sql = "INSERT INTO algorithm_run "
                + "(algorithm_name, input_size, repetition, elapsed_ns) VALUES (?, ?, 1, ?)";

        // If no JDBC driver is available for the URL, skip writing to DB (useful in test environments)
        try {
            DriverManager.getDriver(dbUrl);
        } catch (SQLException e) {
            System.out.println("No JDBC driver registered for " + dbUrl + "; skipping DB write.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, result.getAlgorithmName());
            pstmt.setInt(2, result.getInputSize());
            pstmt.setLong(3, result.getTimeNs());

            pstmt.executeUpdate();
            System.out.println("Benchmark saved to DB: " + result.getAlgorithmName() + " | n=" + result.getInputSize());

        } catch (SQLException e) {
            System.err.println("Failed to write to algorithm_run table: " + e.getMessage());
        }
    }
}
