package edu.ug.nexusb.graphs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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