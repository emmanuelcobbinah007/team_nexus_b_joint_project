package edu.ug.nexusb.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies {@link DatabaseBenchmark#measure} actually writes a row into the
 * real {@code algorithm_run} table (not the {@code algorithm_runs}/
 * camelCase-columns table it wrote to before this test existed, which
 * doesn't exist in {@code data/schema.sql} and silently failed every call).
 * Applies the project's real schema to a throwaway SQLite file, same
 * approach as {@code GraphBuilderDatabaseIntegrationTest}.
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

    @AfterEach
    void tearDown() {
        // nothing to close: DatabaseBenchmark opens/closes its own connection per write
    }

    @Test
    void measureWritesARowToAlgorithmRun() throws SQLException {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(dbUrl);
        benchmark.setAlgorithm(() -> {
            int[] temp = new int[100];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = temp.length - i;
            }
        });

        BenchmarkResult result = benchmark.measure("DummySort", 100, 3);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT algorithm_name, input_size, repetition, elapsed_ns FROM algorithm_run")) {
            assertTrue(rs.next(), "expected one row in algorithm_run");
            assertEquals("DummySort", rs.getString("algorithm_name"));
            assertEquals(100, rs.getInt("input_size"));
            assertEquals(1, rs.getInt("repetition"));
            assertEquals(result.getTimeNs(), rs.getLong("elapsed_ns"));
            assertTrue(!rs.next(), "expected exactly one row");
        }
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
