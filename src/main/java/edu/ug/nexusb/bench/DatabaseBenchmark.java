package edu.ug.nexusb.bench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link Benchmark} implementation (T042) that persists every measurement
 * to the real {@code algorithm_run} table. Follows the measurement rules
 * {@code docs/interfaces.md} lays out for this task: untimed warm-up
 * iterations before timing starts, {@link System#nanoTime()} rather than
 * wall-clock time, and every repetition written to the database as its own
 * row rather than pre-averaged away, so the report can show variance
 * across runs instead of just a single number.
 */
public class DatabaseBenchmark implements Benchmark {

    /**
     * Untimed iterations run before any repetition is timed, so the JIT
     * has already compiled the hot path by the time measurement starts —
     * without this, the first timed repetition would be measuring
     * interpreter/compilation overhead as much as the algorithm itself.
     */
    private static final int WARMUP_ITERATIONS = 3;

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
     * Sets the algorithm logic to be executed during the measurement. Must
     * be called before {@link #measure}.
     *
     * <p><strong>Must generate its own fresh input on every invocation</strong>
     * if what it's timing is order-sensitive (a sort, most obviously): this
     * {@code Runnable} is invoked once per warm-up iteration and once per
     * timed repetition, so if it operates on an array built outside the
     * lambda and captured by reference, every repetition after the first
     * runs against already-sorted (or otherwise already-mutated) input
     * instead of the fresh input each repetition is supposed to measure.
     * Build whatever input the algorithm needs inside the lambda body
     * itself, the same way each individual JUnit test in {@code
     * DatabaseBenchmarkTest} does, so a fresh copy exists every time
     * {@code run()} is called.
     */
    public void setAlgorithm(Runnable currentAlgorithm) {
        this.currentAlgorithm = currentAlgorithm;
    }

    @Override
    public BenchmarkResult measure(String algorithmName, int inputSize, int trialCount) {
        if (currentAlgorithm == null) {
            throw new IllegalStateException("Algorithm Runnable is not set. Call setAlgorithm() first.");
        }
        if (trialCount <= 0) {
            throw new IllegalArgumentException("trialCount must be positive");
        }

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            currentAlgorithm.run();
        }

        long[] timesNs = new long[trialCount];
        long[] memoriesKb = new long[trialCount];

        try (DatabaseWriter writer = DatabaseWriter.openOrNull(dbUrl)) {
            for (int repetition = 1; repetition <= trialCount; repetition++) {
                System.gc();
                long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long startTime = System.nanoTime();

                currentAlgorithm.run();

                long endTime = System.nanoTime();
                long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

                long elapsedNs = endTime - startTime;
                // Convert bytes to Kilobytes and ensure no negative anomalies
                long memoryKb = Math.max(0, (endMem - startMem) / 1024);

                timesNs[repetition - 1] = elapsedNs;
                memoriesKb[repetition - 1] = memoryKb;

                if (writer != null) {
                    writer.writeRepetition(algorithmName, inputSize, repetition, elapsedNs);
                }
            }
        }

        return new BenchmarkResult(algorithmName, inputSize, average(timesNs), average(memoriesKb));
    }

    private static long average(long[] values) {
        long total = 0;
        for (long value : values) {
            total += value;
        }
        return total / values.length;
    }

    /**
     * Owns one JDBC connection for an entire {@link #measure} call, rather
     * than opening and closing a fresh connection per repetition — cheaper,
     * and keeps every repetition of the same measurement on one connection.
     * {@link #openOrNull} returns {@code null} (not a real instance) when no
     * driver/database is available, so {@code measure} can keep timing the
     * algorithm and simply skip persistence, the same graceful-degradation
     * behavior this class always had for test environments without a real
     * {@code nexus.db}.
     */
    private static final class DatabaseWriter implements AutoCloseable {

        private static final String INSERT_SQL = "INSERT INTO algorithm_run "
                + "(algorithm_name, input_size, repetition, elapsed_ns) VALUES (?, ?, ?, ?)";

        private final Connection connection;

        private DatabaseWriter(Connection connection) {
            this.connection = connection;
        }

        static DatabaseWriter openOrNull(String dbUrl) {
            try {
                return new DatabaseWriter(DriverManager.getConnection(dbUrl));
            } catch (SQLException e) {
                System.out.println("No database available at " + dbUrl + "; skipping DB write. (" + e.getMessage() + ")");
                return null;
            }
        }

        void writeRepetition(String algorithmName, int inputSize, int repetition, long elapsedNs) {
            try (PreparedStatement pstmt = connection.prepareStatement(INSERT_SQL)) {
                pstmt.setString(1, algorithmName);
                pstmt.setInt(2, inputSize);
                pstmt.setInt(3, repetition);
                pstmt.setLong(4, elapsedNs);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Failed to write to algorithm_run table: " + e.getMessage());
            }
        }

        @Override
        public void close() {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to close database connection: " + e.getMessage());
            }
        }
    }
}
