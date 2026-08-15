package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises {@link GraphBuilder#buildFromDatabase} against the project's
 * real schema ({@code data/schema.sql}) — not a second, hand-maintained
 * copy of it. Every run applies that one file to a throwaway SQLite
 * database in a JUnit-managed temp directory, so this needs no CI setup
 * step, no checked-in database file, and can never drift from the schema
 * the rest of the project actually uses.
 */
class GraphBuilderDatabaseIntegrationTest {

    @TempDir
    Path tempDir;

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        Path dbFile = tempDir.resolve("test.db");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        applySchema(conn);
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    @Test
    void buildsAGraphThroughTheRealVWeightedEdgeView() throws SQLException {
        insertFacility(1, "F001");
        insertFacility(2, "F002");
        insertFacility(3, "F003");
        // base_time_min=10, traffic_weight=1.5, road_condition='FAIR' (x1.15)
        // -> effective_time_min = round(10 * 1.5 * 1.15, 2) = 17.25
        insertRoadLink(1, 2, 10.0, 1.5, "FAIR", 0); // two-way
        insertRoadLink(2, 3, 5.0, 1.0, "GOOD", 1);  // one-way

        MyGraph graph = GraphBuilder.buildFromDatabase(conn);

        assertEquals(3, graph.vertexCount());
        assertEquals(3, graph.edgeCount()); // 2 (two-way) + 1 (one-way)
        assertTrue(graph.containsEdge("1", "2"));
        assertTrue(graph.containsEdge("2", "1")); // two-way reverse
        assertTrue(graph.containsEdge("2", "3"));
        assertTrue(!graph.containsEdge("3", "2")); // one-way, no reverse
        assertEquals(17.25, graph.weightOf("1", "2"));
    }

    @Test
    void emptyDatabaseProducesAnEmptyGraph() throws SQLException {
        MyGraph graph = GraphBuilder.buildFromDatabase(conn);

        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
    }

    private void insertFacility(int id, String code) throws SQLException {
        String sql = "INSERT INTO facility "
            + "(facility_id, code, name, facility_type, district, latitude, longitude, care_level) "
            + "VALUES (" + id + ", '" + code + "', '" + code + " Facility', 'DISTRICT_HOSPITAL', "
            + "'Test District', 5.6, -0.2, 2)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void insertRoadLink(int fromId, int toId, double baseTimeMin,
            double trafficWeight, String condition, int isOneWay) throws SQLException {
        String sql = "INSERT INTO road_link "
            + "(from_facility_id, to_facility_id, distance_km, base_time_min, "
            + "traffic_weight, road_condition, is_one_way) VALUES ("
            + fromId + ", " + toId + ", 1.0, " + baseTimeMin + ", "
            + trafficWeight + ", '" + condition + "', " + isOneWay + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void applySchema(Connection conn) throws SQLException, IOException {
        // Comments are stripped from the whole file before splitting on
        // ";", not per-fragment after: a comment can itself contain a
        // semicolon (schema.sql has one — "append-only; backs undo"), and
        // splitting first would cut such a line in half, leaving a second
        // fragment that no longer starts with "--" and reads as bad SQL.
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
