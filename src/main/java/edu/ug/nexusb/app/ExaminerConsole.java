package edu.ug.nexusb.app;

import edu.ug.nexusb.data.DBLoader;
import edu.ug.nexusb.graphs.AdjacencyListGraph;
import edu.ug.nexusb.graphs.Dijkstra;
import edu.ug.nexusb.graphs.Edge;
import edu.ug.nexusb.graphs.GraphBuilder;
import edu.ug.nexusb.graphs.MyGraph;
import edu.ug.nexusb.graphs.PathResult;
import edu.ug.nexusb.optimization.GreedyDispatch;
import edu.ug.nexusb.optimization.GreedyDispatch.CaseRequest;
import edu.ug.nexusb.scheduling.TriageCase;
import edu.ug.nexusb.scheduling.TriageDispatchEngine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * T056: menu-driven entry point that runs every graded demo (T046, T051,
 * T053, T055) against real code and real data — nothing here prints a
 * pre-computed string. The DB-backed demos (T046, T053, T055) open a fresh
 * connection to {@code nexus.db} and auto-initialize it via {@link DBLoader}
 * on first run if it's empty, so an examiner can run this straight after a
 * clone with no separate setup step. T051's demo is self-contained (an
 * in-memory graph, no DB) so it always works even before the DB exists.
 */
public final class ExaminerConsole {

    private static final String DB_URL = "jdbc:sqlite:nexus.db";
    private static final Scanner scanner = new Scanner(System.in);

    private ExaminerConsole() {
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   NEXUS-B HEALTHCARE & DISPATCH EXAMINER CONSOLE ");
        System.out.println("=================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option (1-6): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> runGreedyDispatchDemo();
                case "2" -> runDijkstraShortestPathDemo();
                case "3" -> runTriageQueueDemo();
                case "4" -> runIndexingEngineDemo();
                case "5" -> runAllDemosSequential();
                case "6" -> {
                    System.out.println("\nExiting Examiner Console. Goodbye!");
                    running = false;
                }
                default -> System.out.println("\n[ERROR] Invalid option. Please enter a number between 1 and 6.\n");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n---------------- MAIN MENU ----------------");
        System.out.println("1. Run T051 Greedy Dispatch vs Optimal Demo");
        System.out.println("2. Run T046 Dijkstra Facility Routing Demo");
        System.out.println("3. Run T053 Triage / Dispatch Engine Demo");
        System.out.println("4. Run T055 Live DB Indexing Engine Demo");
        System.out.println("5. Run ALL Demos Sequentially");
        System.out.println("6. Exit");
        System.out.println("-------------------------------------------");
    }

    // ------------------------------------------------------------------
    // T051 - self-contained, no DB needed. Same engineered scenario as
    // docs/counterexamples/counterexample_greedy_dispatch.md and
    // GreedyDispatchTest, computed live here rather than printed as text.
    // ------------------------------------------------------------------

    private static void runGreedyDispatchDemo() {
        System.out.println("\n=== [DEMO T051] GREEDY DISPATCH & COUNTEREXAMPLE ===");

        MyGraph roadNetwork = new AdjacencyListGraph();
        roadNetwork.addEdge(new Edge("STATION", "F024", 3.0));
        roadNetwork.addEdge(new Edge("STATION", "F053", 5.0));
        CaseRequest[] requests = {
            new CaseRequest("REQ_ROUTINE", "F024", 4, 30),
            new CaseRequest("REQ_URGENT", "F053", 1, 15),
        };

        System.out.println("\nIncoming Requests (from resource station STATION):");
        System.out.println("  - REQ_ROUTINE: Facility F024 (Dist: 3.0km, Triage Level: 4)");
        System.out.println("  - REQ_URGENT:  Facility F053 (Dist: 5.0km, Triage Level: 1 - Emergency)");

        String[] greedyOrder = GreedyDispatch.runGreedyDispatch("STATION", requests, roadNetwork);
        String[] optimalOrder = GreedyDispatch.runOptimalDispatch("STATION", requests, roadNetwork);
        double greedyPenalty = GreedyDispatch.totalWeightedPenalty("STATION", greedyOrder, requests, roadNetwork);
        double optimalPenalty = GreedyDispatch.totalWeightedPenalty("STATION", optimalOrder, requests, roadNetwork);

        System.out.println("\n[Greedy Decision] (nearest facility first): " + String.join(" -> ", greedyOrder));
        System.out.println("  Total Weighted Penalty: " + greedyPenalty);

        System.out.println("\n[Optimal Decision] (distance x triage level): " + String.join(" -> ", optimalOrder));
        System.out.println("  Total Weighted Penalty: " + optimalPenalty);

        double reductionPct = (greedyPenalty - optimalPenalty) / greedyPenalty * 100.0;
        System.out.printf("  Penalty Reduction: %.1f%%%n", reductionPct);
        System.out.println("\nSee docs/counterexamples/counterexample_greedy_dispatch.md for why.");
        System.out.println("=== END DEMO T051 ===\n");
    }

    // ------------------------------------------------------------------
    // T046 - real DB-backed graph, real Dijkstra run on whatever facility
    // codes the examiner types (or the defaults).
    // ------------------------------------------------------------------

    private static void runDijkstraShortestPathDemo() {
        System.out.println("\n=== [DEMO T046] DIJKSTRA SHORTEST PATH ROUTING ===");
        ensureDatabaseReady();

        System.out.print("Enter Origin Facility Code [Default F001]: ");
        String originCode = scanner.nextLine().trim();
        if (originCode.isEmpty()) originCode = "F001";

        System.out.print("Enter Destination Facility Code [Default F024]: ");
        String destCode = scanner.nextLine().trim();
        if (destCode.isEmpty()) destCode = "F024";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String originId = resolveFacilityId(conn, originCode);
            String destId = resolveFacilityId(conn, destCode);
            if (originId == null || destId == null) {
                System.out.println("\n[ERROR] Unknown facility code(s). Check the code and try again.");
                System.out.println("=== END DEMO T046 ===\n");
                return;
            }

            MyGraph roadNetwork = GraphBuilder.buildFromDatabase(conn);
            PathResult result = Dijkstra.shortestPaths(roadNetwork, originId);

            System.out.println("\nCalculating route from " + originCode + " to " + destCode + "...");
            if (!result.isReachable(destId)) {
                System.out.println("  " + destCode + " is not reachable from " + originCode + " on the road network.");
            } else {
                System.out.println("  Shortest Distance (effective time): " + result.distanceTo(destId) + " min");
                System.out.print("  Route Path: ");
                String[] path = result.pathTo(destId);
                for (int i = 0; i < path.length; i++) {
                    if (i > 0) System.out.print(" -> ");
                    System.out.print(codeForFacilityId(conn, path[i]));
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error: " + e.getMessage());
        }
        System.out.println("=== END DEMO T046 ===\n");
    }

    // ------------------------------------------------------------------
    // T053 - real triage heap loaded from real PENDING rows in the DB.
    // ------------------------------------------------------------------

    private static void runTriageQueueDemo() {
        System.out.println("\n=== [DEMO T053] TRIAGE QUEUE & SCHEDULER ===");
        ensureDatabaseReady();

        try (TriageDispatchEngine engine = TriageDispatchEngine.connectToDefaultDatabase()) {
            engine.loadPendingCases();

            if (engine.isEmpty()) {
                System.out.println("No PENDING cases in case_request right now"
                        + " (already dispatched by an earlier run, or none loaded).");
                System.out.println("=== END DEMO T053 ===\n");
                return;
            }

            System.out.println("Loaded " + engine.size() + " PENDING case(s) into the triage heap.");
            System.out.println("\nDispatching in priority order (most urgent first):");
            int rank = 1;
            TriageCase next;
            while ((next = engine.dispatchNext()) != null) {
                System.out.println("  " + rank + ". case_id=" + next.getCaseId()
                        + " (Triage Level " + next.getTriageLevel()
                        + ", requested_at=" + next.getRequestedAt() + ") -> marked TRIAGED");
                rank++;
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error: " + e.getMessage());
        }
        System.out.println("=== END DEMO T053 ===\n");
    }

    // ------------------------------------------------------------------
    // T055 - real hash + tree index built from real case_request rows.
    // ------------------------------------------------------------------

    private static void runIndexingEngineDemo() {
        System.out.println("\n=== [DEMO T055] LIVE DB INDEXING ENGINE ===");
        ensureDatabaseReady();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            IndexingEngine engine = IndexingEngine.buildFromDatabase(conn);
            System.out.println("Indexed " + engine.caseCount() + " case(s) (hash index by reference, "
                    + "red-black tree index by requested_at).");

            String sampleRef = oneCaseRef(conn);
            if (sampleRef == null) {
                System.out.println("No cases in case_request to query.");
                System.out.println("=== END DEMO T055 ===\n");
                return;
            }

            System.out.println("\nQuerying case '" + sampleRef + "' via hash index...");
            IndexingEngine.CaseRow row = engine.findByReference(sampleRef);
            System.out.println("  Found: triageLevel=" + row.triageLevel()
                    + ", requestedAt=" + row.requestedAt() + ", status=" + row.status());

            String[] range = requestedAtRange(conn);
            if (range != null) {
                var inRange = engine.findInTimeRange(range[0], range[1]);
                System.out.println("\nQuerying full requested_at range [" + range[0] + ", " + range[1]
                        + "] via tree index...");
                System.out.println("  " + inRange.size() + " case(s) found.");
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error: " + e.getMessage());
        }
        System.out.println("=== END DEMO T055 ===\n");
    }

    private static void runAllDemosSequential() {
        System.out.println("\n>>> RUNNING ALL PROJECT DEMOS IN SEQUENCE <<<\n");
        runGreedyDispatchDemo();
        runDijkstraShortestPathDemo();
        runTriageQueueDemo();
        runIndexingEngineDemo();
        System.out.println(">>> ALL DEMOS COMPLETED SUCCESSFULLY <<<\n");
    }

    // ------------------------------------------------------------------
    // DB helpers
    // ------------------------------------------------------------------

    private static void ensureDatabaseReady() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM facility LIMIT 1");
        } catch (SQLException notReadyYet) {
            System.out.println("nexus.db has no data yet - initializing from data/*.csv (one-time)...");
            DBLoader.run();
        }
    }

    private static String resolveFacilityId(Connection conn, String code) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT facility_id FROM facility WHERE code = ?")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? String.valueOf(rs.getInt("facility_id")) : null;
            }
        }
    }

    private static String codeForFacilityId(Connection conn, String facilityId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT code FROM facility WHERE facility_id = ?")) {
            stmt.setInt(1, Integer.parseInt(facilityId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("code") : facilityId;
            }
        }
    }

    private static String oneCaseRef(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT case_ref FROM case_request LIMIT 1")) {
            return rs.next() ? rs.getString("case_ref") : null;
        }
    }

    /** @return {from, to} spanning every requested_at in the table, or null if the table is empty. */
    private static String[] requestedAtRange(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MIN(requested_at) AS lo, MAX(requested_at) AS hi FROM case_request")) {
            if (rs.next() && rs.getString("lo") != null) {
                return new String[] {rs.getString("lo"), rs.getString("hi")};
            }
            return null;
        }
    }
}
