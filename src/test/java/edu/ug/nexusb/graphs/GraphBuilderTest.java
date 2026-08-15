package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphBuilderTest {

    private static final String DB_URL = "jdbc:sqlite:campus.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found on classpath", e);
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Initialize test database with required tables
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (Statement stmt = conn.createStatement()) {
                // Create facility table if it doesn't exist
                stmt.execute("CREATE TABLE IF NOT EXISTS facility (" +
                        "facility_id TEXT PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "latitude REAL NOT NULL, " +
                        "longitude REAL NOT NULL" +
                        ")");

                // Create road_link table if it doesn't exist
                stmt.execute("CREATE TABLE IF NOT EXISTS road_link (" +
                        "link_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "from_facility TEXT NOT NULL, " +
                        "to_facility TEXT NOT NULL, " +
                        "distance REAL NOT NULL" +
                        ")");

                // Insert test data into facility table
                stmt.execute("DELETE FROM facility");
                for (int i = 1; i <= 60; i++) {
                    stmt.execute(String.format(
                            "INSERT INTO facility (facility_id, name, latitude, longitude) " +
                            "VALUES ('F%03d', 'Hospital %d', %.4f, %.4f)",
                            i, i, 5.5 + (i * 0.001), -0.2 + (i * 0.001)));
                }

                // Insert test data into road_link table
                stmt.execute("DELETE FROM road_link");
                for (int i = 1; i < 60; i++) {
                    stmt.execute(String.format(
                            "INSERT INTO road_link (from_facility, to_facility, distance) " +
                            "VALUES ('F%03d', 'F%03d', %.2f)",
                            i, i + 1, 1.5 * i));
                }
            }
        }
    }

    @Test
    void vertexCountMatchesFacilityTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            MyGraph graph = GraphBuilder.buildFromDatabase(conn);
            int facilityCount = countRows(conn, "facility");

            assertEquals(facilityCount, graph.vertexCount());
            assertTrue(facilityCount >= 50, "expected at least 50 facilities per the brief");
        }
    }

    @Test
    void edgeCountIsAtLeastRoadLinkCount() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            MyGraph graph = GraphBuilder.buildFromDatabase(conn);
            int roadLinkCount = countRows(conn, "road_link");

            assertTrue(graph.edgeCount() >= roadLinkCount);
        }
    }

    private static int countRows(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
