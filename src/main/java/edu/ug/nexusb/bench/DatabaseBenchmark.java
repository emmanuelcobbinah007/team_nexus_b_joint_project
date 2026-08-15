package edu.ug.nexusb.bench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * Writes the benchmark result directly to the algorithm_runs table.
     */
    private void writeToDatabase(BenchmarkResult result) {
        // The table schema requires these exact fields based on the project brief
        String sql = "INSERT INTO algorithm_runs (algorithmName, inputSize, timeNs, memoryKb, dateRun) VALUES (?, ?, ?, ?, ?)";

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
            pstmt.setInt(2, result.getInputSize()); //[cite: 3]
            pstmt.setLong(3, result.getTimeNs()); //[cite: 3]
            pstmt.setLong(4, result.getMemoryKb()); //[cite: 3]
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); //[cite: 3]
            
            pstmt.executeUpdate();
            System.out.println("Benchmark saved to DB: " + result.getAlgorithmName() + " | n=" + result.getInputSize());
            
        } catch (SQLException e) {
            System.err.println("Failed to write to algorithm_runs table: " + e.getMessage());
        }
    }
}
