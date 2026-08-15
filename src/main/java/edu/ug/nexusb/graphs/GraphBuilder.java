package edu.ug.nexusb.graphs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link MyGraph} from the {@code facility} and {@code road_link}
 * tables (T038). Every facility becomes a vertex; every row read becomes
 * one directed edge, or two when {@code is_one_way = 0}, since a two-way
 * road is reachable from either end.
 *
 * <p>Split into a database-reading half ({@link #buildFromDatabase}) and a
 * pure logic half ({@link #buildFromRows}) so the actual graph-assembly
 * logic can be unit-tested with plain in-memory data, independent of any
 * database driver being available.
 */
public final class GraphBuilder {

    private GraphBuilder() {
        // utility class — not meant to be instantiated
    }

    /** A single road row, already read out of the database. */
    public record RoadRow(int fromFacilityId, int toFacilityId, double effectiveTimeMin, boolean isOneWay) {
    }

    /**
     * Builds a graph from the given database connection.
     *
     * @param conn open JDBC connection to the project's SQLite database
     * @return an adjacency-list-backed graph whose vertex and edge counts
     *     match the database exactly
     * @throws SQLException if either query fails
     */
    public static MyGraph buildFromDatabase(Connection conn) throws SQLException {
        List<Integer> facilityIds = readFacilityIds(conn);
        List<RoadRow> roads = readRoads(conn);
        return buildFromRows(facilityIds, roads);
    }

    /**
     * Pure logic: builds a graph from already-fetched facility IDs and road
     * rows, with no database access at all. This is what tests call
     * directly with hand-written data, so the graph-assembly logic can be
     * verified without a live database or JDBC driver.
     */
    public static MyGraph buildFromRows(List<Integer> facilityIds, List<RoadRow> roads) {
        MyGraph graph = new AdjacencyListGraph();

        for (int facilityId : facilityIds) {
            graph.addVertex(String.valueOf(facilityId));
        }

        for (RoadRow road : roads) {
            String fromId = String.valueOf(road.fromFacilityId());
            String toId = String.valueOf(road.toFacilityId());
            graph.addEdge(new Edge(fromId, toId, road.effectiveTimeMin()));
            if (!road.isOneWay()) {
                graph.addEdge(new Edge(toId, fromId, road.effectiveTimeMin()));
            }
        }

        return graph;
    }

    private static List<Integer> readFacilityIds(Connection conn) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT facility_id FROM facility";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("facility_id"));
            }
        }
        return ids;
    }

    private static List<RoadRow> readRoads(Connection conn) throws SQLException {
        List<RoadRow> roads = new ArrayList<>();
        String sql = "SELECT from_facility_id, to_facility_id, "
                + "effective_time_min, is_one_way FROM v_weighted_edge";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                roads.add(new RoadRow(
                        rs.getInt("from_facility_id"),
                        rs.getInt("to_facility_id"),
                        rs.getDouble("effective_time_min"),
                        rs.getInt("is_one_way") == 1));
            }
        }
        return roads;
    }
}