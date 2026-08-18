package edu.ug.nexusb.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.bench.Benchmark.BenchmarkResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies {@link DatabaseBenchmark#measure} against the real {@code
 * algorithm_run} table (not the {@code algorithm_runs}/camelCase-columns
 * table it wrote to before this test existed, which doesn't exist in
 * {@code data/schema.sql} and silently failed every call) — and, since T042
 * asks for the full measurement methodology rather than just a working
 * write, that each repetition lands as its own row with a real, distinct
 * {@code elapsed_ns}, and that warm-up iterations genuinely run before any
 * repetition is timed. Applies the project's real schema to a throwaway
 * SQLite file, same approach as {@code GraphBuilderDatabaseIntegrationTest}.
 */
class DatabaseBenchmarkIntegrationTest {

    @TempDir
    Path tempDir;

    private String dbUrl;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        Path dbFile = tempDir.resolve("test.db");
        dbUrl = "jdbc:sqlite:" + dbFile;
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            applySchema(conn);
        }
    }

    @Test
    void measureWritesOneRowPerRepetitionNotOneAveragedRow() throws SQLException {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(dbUrl);
        benchmark.setAlgorithm(() -> {
            int[] temp = new int[100];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = temp.length - i;
            }
        });

        BenchmarkResult result = benchmark.measure("DummySort", 100, 3);

        long sumOfRepetitionTimes = 0;
        int rowCount = 0;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT algorithm_name, input_size, repetition, elapsed_ns "
                     + "FROM algorithm_run ORDER BY repetition")) {
            while (rs.next()) {
                rowCount++;
                assertEquals("DummySort", rs.getString("algorithm_name"));
                assertEquals(100, rs.getInt("input_size"));
                assertEquals(rowCount, rs.getInt("repetition"), "repetitions must be recorded 1, 2, 3 in order");
                long elapsedNs = rs.getLong("elapsed_ns");
                assertTrue(elapsedNs >= 0, "a real timing was recorded, not a placeholder");
                sumOfRepetitionTimes += elapsedNs;
            }
        }

        assertEquals(3, rowCount, "trialCount=3 must produce exactly 3 rows, not 1 pre-averaged row");
        assertEquals(sumOfRepetitionTimes / 3, result.getTimeNs(),
            "the returned BenchmarkResult must be the average of exactly the rows written to the DB");
    }

    @Test
    void warmupIterationsRunBeforeAnyTimedRepetition() {
        AtomicInteger invocationCount = new AtomicInteger(0);
        DatabaseBenchmark benchmark = new DatabaseBenchmark(dbUrl);
        benchmark.setAlgorithm(invocationCount::incrementAndGet);

        benchmark.measure("Noop", 1, 3);

        // 3 untimed warm-up calls + 3 timed repetitions = 6 total invocations,
        // even though trialCount alone only accounts for 3 of them.
        assertEquals(6, invocationCount.get(),
            "measure() must run warm-up iterations in addition to trialCount timed repetitions");
    }

    @Test
    void nonPositiveTrialCountThrows() {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(dbUrl);
        benchmark.setAlgorithm(() -> { });
        assertThrows(IllegalArgumentException.class, () -> benchmark.measure("X", 1, 0));
        assertThrows(IllegalArgumentException.class, () -> benchmark.measure("X", 1, -1));
    }

    @Test
    void missingAlgorithmThrows() {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(dbUrl);
        assertThrows(IllegalStateException.class, () -> benchmark.measure("X", 1, 1));
    }

    @Test
    void measureStillWorksWithNoDatabaseAvailable() {
        DatabaseBenchmark benchmark = new DatabaseBenchmark("jdbc:sqlite:/nonexistent/path/nowhere.db");
        benchmark.setAlgorithm(() -> { });

        // Graceful degradation: an unreachable/invalid DB must not stop the
        // algorithm itself from being measured, only skip persistence.
        BenchmarkResult result = benchmark.measure("Noop", 1, 2);
        assertTrue(result.getTimeNs() >= 0);
    }

    private static void applySchema(Connection conn) throws SQLException, IOException {
        String schema = Files.readString(Path.of("data", "schema.sql"), StandardCharsets.UTF_8);
        String withoutComments = stripCommentLines(schema);
        try (Statement stmt = conn.createStatement()) {
            for (String statement : withoutComments.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static String stripCommentLines(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.strip().startsWith("--")) {
                result.append(line).append('\n');
            }
        }
        return result.toString();
    }
}
