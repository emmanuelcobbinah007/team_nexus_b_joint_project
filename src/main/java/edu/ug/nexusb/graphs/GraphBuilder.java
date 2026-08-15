package edu.ug.nexusb.graphs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public final class GraphBuilder {

    private GraphBuilder() {
        
    }

    public static MyGraph buildFromDatabase(Connection conn) throws SQLException {
        MyGraph graph = new AdjacencyListGraph();
        loadFacilities(conn, graph);
        loadRoads(conn, graph);
        return graph;
    }

    private static void loadFacilities(Connection conn, MyGraph graph) throws SQLException {
        String sql = "SELECT facility_id FROM facility";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                graph.addVertex(String.valueOf(rs.getInt("facility_id")));
            }
        }
    }

    private static void loadRoads(Connection conn, MyGraph graph) throws SQLException {
        String sql = "SELECT from_facility_id, to_facility_id, "
                + "effective_time_min, is_one_way FROM v_weighted_edge";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String fromId = String.valueOf(rs.getInt("from_facility_id"));
                String toId = String.valueOf(rs.getInt("to_facility_id"));
                double weight = rs.getDouble("effective_time_min");
                boolean isOneWay = rs.getInt("is_one_way") == 1;

                graph.addEdge(new Edge(fromId, toId, weight));
                if (!isOneWay) {
                    graph.addEdge(new Edge(toId, fromId, weight));
                }
            }
        }
    }
}