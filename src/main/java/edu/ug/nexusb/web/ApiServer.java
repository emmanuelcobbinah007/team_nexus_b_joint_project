package edu.ug.nexusb.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import edu.ug.nexusb.algorithms.InsertionSort;
import edu.ug.nexusb.algorithms.LinearBinarySearch;
import edu.ug.nexusb.algorithms.MergeSort;
import edu.ug.nexusb.algorithms.QuickSort;
import edu.ug.nexusb.algorithms.SelectionSort;
import edu.ug.nexusb.algorithms.Sorter;
import edu.ug.nexusb.app.IndexingEngine;
import edu.ug.nexusb.bench.TriageComparison;
import edu.ug.nexusb.bench.TriageComparison.DetailedResult;
import edu.ug.nexusb.bench.TriageComparison.ServedCase;
import edu.ug.nexusb.bench.TriageComparison.TriageCase;
import edu.ug.nexusb.core.KeyNotFoundException;
import edu.ug.nexusb.data.DBLoader;
import edu.ug.nexusb.graphs.Dfs;
import edu.ug.nexusb.graphs.Dijkstra;
import edu.ug.nexusb.graphs.Edge;
import edu.ug.nexusb.graphs.GraphBuilder;
import edu.ug.nexusb.graphs.Kruskal;
import edu.ug.nexusb.graphs.MstResult;
import edu.ug.nexusb.graphs.MyGraph;
import edu.ug.nexusb.graphs.PathResult;
import edu.ug.nexusb.graphs.Prim;
import edu.ug.nexusb.graphs.Reachability;
import edu.ug.nexusb.linear.ArrayQueue;
import edu.ug.nexusb.optimization.GreedyDispatch;
import edu.ug.nexusb.optimization.GreedyDispatch.CaseRequest;
import edu.ug.nexusb.optimization.KnapsackDP;
import edu.ug.nexusb.trees.HashSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Live web frontend for the whole project: a small HTTP server exposing the
 * real algorithm implementations (Dijkstra, BFS, DFS, Kruskal, Prim,
 * GreedyDispatch, TriageComparison, IndexingEngine, KnapsackDP, sorting and
 * searching) as a JSON API, plus the single-page frontend
 * ({@code src/main/resources/web/index.html}) that calls it. Nothing here
 * re-implements an algorithm -- every response comes from calling the same
 * classes the test suite and console demo already exercise.
 *
 * <p>Run with {@code mvn exec:java -Dexec.mainClass=edu.ug.nexusb.web.ApiServer}
 * (or after {@code mvn compile}, {@code java -cp target/classes:<deps>
 * edu.ug.nexusb.web.ApiServer}), then open http://localhost:8080/.
 */
public final class ApiServer {

    private static final String DB_URL = "jdbc:sqlite:nexus.db";
    private static final int PORT = 8080;

    private final Connection connection;
    private final MyGraph graph;

    private ApiServer(Connection connection, MyGraph graph) {
        this.connection = connection;
        this.graph = graph;
    }

    public static void main(String[] args) throws Exception {
        ensureDatabaseReady();
        Connection connection = DriverManager.getConnection(DB_URL);
        MyGraph graph = GraphBuilder.buildFromDatabase(connection);
        System.out.println("Graph loaded: " + graph.vertexCount() + " vertices, " + graph.edgeCount() + " edges.");

        ApiServer server = new ApiServer(connection, graph);
        HttpServer http = HttpServer.create(new InetSocketAddress(PORT), 0);
        http.createContext("/", server::serveFrontend);
        http.createContext("/api/facilities", server::handleFacilities);
        http.createContext("/api/graph", server::handleGraph);
        http.createContext("/api/dijkstra", server::handleDijkstra);
        http.createContext("/api/bfs", server::handleBfs);
        http.createContext("/api/dfs", server::handleDfs);
        http.createContext("/api/kruskal", server::handleKruskal);
        http.createContext("/api/prim", server::handlePrim);
        http.createContext("/api/dispatch", server::handleDispatch);
        http.createContext("/api/triage", server::handleTriage);
        http.createContext("/api/index/lookup", server::handleIndexLookup);
        http.createContext("/api/index/range", server::handleIndexRange);
        http.createContext("/api/knapsack", server::handleKnapsack);
        http.createContext("/api/sort", server::handleSort);
        http.createContext("/api/search", server::handleSearch);
        http.setExecutor(null);
        http.start();
        System.out.println("NexusB web demo running at http://localhost:" + PORT + "/");
    }

    private static void ensureDatabaseReady() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM facility LIMIT 1");
        } catch (SQLException notReadyYet) {
            System.out.println("nexus.db has no data yet - initializing from data/*.csv (one-time)...");
            DBLoader.run();
        }
    }

    // ------------------------------------------------------------------
    // Static frontend
    // ------------------------------------------------------------------

    private void serveFrontend(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String resourcePath = path.equals("/") ? "/web/index.html" : "/web" + path;
        try (InputStream in = ApiServer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                sendText(exchange, 404, "text/plain", "Not found: " + path);
                return;
            }
            String contentType = resourcePath.endsWith(".html") ? "text/html; charset=utf-8"
                    : resourcePath.endsWith(".css") ? "text/css"
                    : resourcePath.endsWith(".js") ? "application/javascript"
                    : "application/octet-stream";
            byte[] body = in.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }

    // ------------------------------------------------------------------
    // Graph data (T038 GraphBuilder, for rendering)
    // ------------------------------------------------------------------

    private void handleFacilities(HttpExchange exchange) {
        respondJson(exchange, () -> {
            Json array = Json.array();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT facility_id, code, name, latitude, longitude, facility_type FROM facility")) {
                while (rs.next()) {
                    array.element(Json.object()
                            .field("id", String.valueOf(rs.getInt("facility_id")))
                            .field("code", rs.getString("code"))
                            .field("name", rs.getString("name"))
                            .field("lat", rs.getDouble("latitude"))
                            .field("lon", rs.getDouble("longitude"))
                            .field("type", rs.getString("facility_type")));
                }
            }
            return array.close();
        });
    }

    private void handleGraph(HttpExchange exchange) {
        respondJson(exchange, () -> {
            Json vertices = Json.array();
            for (var it = graph.vertices().iterator(); it.hasNext(); ) {
                vertices.element(it.next());
            }

            Json edges = Json.array();
            for (var vertexIt = graph.vertices().iterator(); vertexIt.hasNext(); ) {
                String from = vertexIt.next();
                for (var edgeIt = graph.edgesFrom(from).iterator(); edgeIt.hasNext(); ) {
                    Edge edge = edgeIt.next();
                    edges.element(Json.object()
                            .field("from", edge.fromId())
                            .field("to", edge.toId())
                            .field("weight", edge.weight()));
                }
            }

            return Json.object()
                    .field("vertices", vertices)
                    .field("edges", edges)
                    .field("representation", graph.representationName())
                    .close();
        });
    }

    // ------------------------------------------------------------------
    // T046 Dijkstra
    // ------------------------------------------------------------------

    private void handleDijkstra(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String source = requireParam(exchange, "source");
            String dest = param(exchange, "dest");

            PathResult result = Dijkstra.shortestPaths(graph, source);

            Json visitOrder = Json.array();
            for (var it = result.visitOrder().iterator(); it.hasNext(); ) {
                visitOrder.element(it.next());
            }

            Json response = Json.object()
                    .field("sourceId", result.sourceId())
                    .field("visitOrder", visitOrder);

            if (dest != null) {
                boolean reachable = result.isReachable(dest);
                response.field("destId", dest)
                        .field("reachable", reachable)
                        .field("distance", reachable ? result.distanceTo(dest) : -1);
                if (reachable) {
                    Json path = Json.array();
                    for (String id : result.pathTo(dest)) {
                        path.element(id);
                    }
                    response.field("path", path);
                }
            }
            return response.close();
        });
    }

    // ------------------------------------------------------------------
    // T047 BFS reachability
    // ------------------------------------------------------------------

    private void handleBfs(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String source = requireParam(exchange, "source");
            String closedParam = param(exchange, "closed"); // "A-B,C-D"

            HashSet<String> closedEdgeKeys = new HashSet<>();
            if (closedParam != null && !closedParam.isBlank()) {
                for (String pair : closedParam.split(",")) {
                    String[] ends = pair.split("-");
                    if (ends.length == 2) {
                        closedEdgeKeys.add(ends[0] + "->" + ends[1]);
                        closedEdgeKeys.add(ends[1] + "->" + ends[0]);
                    }
                }
            }

            var reachable = Reachability.bfsReachable(
                    graph, source, closedEdgeKeys, new ArrayQueue<>(), new HashSet<>());

            Json array = Json.array();
            for (var it = reachable.iterator(); it.hasNext(); ) {
                array.element(it.next());
            }
            return Json.object().field("sourceId", source).field("reachable", array).close();
        });
    }

    // ------------------------------------------------------------------
    // T048 DFS + cycle detection
    // ------------------------------------------------------------------

    private void handleDfs(HttpExchange exchange) {
        respondJson(exchange, () -> {
            Dfs.Result result = Dfs.traverse(graph);

            Json visitOrder = Json.array();
            for (var it = result.visitOrder().iterator(); it.hasNext(); ) {
                visitOrder.element(it.next());
            }

            return Json.object()
                    .field("visitOrder", visitOrder)
                    .field("hasCycle", result.hasCycle())
                    .field("cycleFromId", result.hasCycle() ? result.cycleFromId() : "")
                    .field("cycleToId", result.hasCycle() ? result.cycleToId() : "")
                    .close();
        });
    }

    // ------------------------------------------------------------------
    // T049 Kruskal / T050 Prim
    // ------------------------------------------------------------------

    private void handleKruskal(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String[] vertexIds = collectVertexIds();
            Kruskal.Edge[] edges = collectKruskalEdges(vertexIds);

            Kruskal.Result result = Kruskal.run(vertexIds.length, edges);

            Json edgeArray = Json.array();
            for (Kruskal.Edge edge : result.mstEdges) {
                edgeArray.element(Json.object()
                        .field("from", vertexIds[edge.src])
                        .field("to", vertexIds[edge.dest])
                        .field("weight", edge.weight));
            }
            return Json.object()
                    .field("edges", edgeArray)
                    .field("totalWeight", (double) result.totalWeight)
                    .field("isSpanning", result.isSpanning)
                    .close();
        });
    }

    private void handlePrim(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String start = requireParam(exchange, "start");
            MstResult result = Prim.minimumSpanningTree(graph, start);

            Json edgeArray = Json.array();
            for (Edge edge : result.edges()) {
                edgeArray.element(Json.object()
                        .field("from", edge.fromId())
                        .field("to", edge.toId())
                        .field("weight", edge.weight()));
            }
            return Json.object()
                    .field("startId", start)
                    .field("edges", edgeArray)
                    .field("totalWeight", result.totalWeight())
                    .close();
        });
    }

    private String[] collectVertexIds() {
        String[] ids = new String[graph.vertexCount()];
        int i = 0;
        for (var it = graph.vertices().iterator(); it.hasNext(); ) {
            ids[i++] = it.next();
        }
        return ids;
    }

    // Kruskal.run() takes an int-indexed edge array, not MyGraph -- rebuild
    // one undirected edge list from the same live graph (dedup each
    // undirected pair once, matching how GraphExperiments builds its own).
    private Kruskal.Edge[] collectKruskalEdges(String[] vertexIds) {
        HashSet<String> seenPairs = new HashSet<>();
        Kruskal.Edge[] buffer = new Kruskal.Edge[graph.edgeCount()];
        int count = 0;
        for (int i = 0; i < vertexIds.length; i++) {
            for (var it = graph.edgesFrom(vertexIds[i]).iterator(); it.hasNext(); ) {
                Edge edge = it.next();
                String pairKey = pairKey(edge.fromId(), edge.toId());
                if (seenPairs.contains(pairKey)) {
                    continue;
                }
                seenPairs.add(pairKey);
                int to = indexOf(vertexIds, edge.toId());
                buffer[count++] = new Kruskal.Edge(i, to, (int) Math.round(edge.weight()));
            }
        }
        Kruskal.Edge[] trimmed = new Kruskal.Edge[count];
        System.arraycopy(buffer, 0, trimmed, 0, count);
        return trimmed;
    }

    private static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static int indexOf(String[] ids, String target) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(target)) {
                return i;
            }
        }
        throw new KeyNotFoundException("no such vertex: " + target);
    }

    // ------------------------------------------------------------------
    // T051 Greedy vs. optimal dispatch
    // ------------------------------------------------------------------

    private void handleDispatch(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String station = requireParam(exchange, "station");
            String casesParam = requireParam(exchange, "cases"); // "ref:facilityId:triage,..."

            String[] entries = casesParam.split(",");
            CaseRequest[] requests = new CaseRequest[entries.length];
            for (int i = 0; i < entries.length; i++) {
                String[] fields = entries[i].split(":");
                requests[i] = new CaseRequest(fields[0], fields[1], Integer.parseInt(fields[2]), 30);
            }

            String[] greedyOrder = GreedyDispatch.runGreedyDispatch(station, requests, graph);
            String[] optimalOrder = GreedyDispatch.runOptimalDispatch(station, requests, graph);
            double greedyPenalty = GreedyDispatch.totalWeightedPenalty(station, greedyOrder, requests, graph);
            double optimalPenalty = GreedyDispatch.totalWeightedPenalty(station, optimalOrder, requests, graph);

            PathResult pathResult = Dijkstra.shortestPaths(graph, station);

            return Json.object()
                    .field("stationId", station)
                    .field("greedyOrder", stringArray(greedyOrder))
                    .field("optimalOrder", stringArray(optimalOrder))
                    .field("greedyPenalty", greedyPenalty)
                    .field("optimalPenalty", optimalPenalty)
                    .field("cases", caseDetailArray(requests, pathResult))
                    .close();
        });
    }

    private static Json stringArray(String[] values) {
        Json array = Json.array();
        for (String value : values) {
            array.element(value);
        }
        return array;
    }

    private static Json caseDetailArray(CaseRequest[] requests, PathResult pathResult) {
        Json array = Json.array();
        for (CaseRequest request : requests) {
            array.element(Json.object()
                    .field("caseRef", request.caseRef)
                    .field("originFacilityId", request.originFacilityId)
                    .field("triageLevel", request.triageLevel)
                    .field("distance", pathResult.distanceTo(request.originFacilityId)));
        }
        return array;
    }

    // ------------------------------------------------------------------
    // T053/T054 Triage queue: FCFS vs. priority
    // ------------------------------------------------------------------

    private void handleTriage(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String casesParam = requireParam(exchange, "cases"); // "id:arrival:severity,..."
            String[] entries = casesParam.split(",");
            List<TriageCase> cases = List.of(parseTriageCases(entries));

            DetailedResult result = TriageComparison.compareDetailed(cases);

            return Json.object()
                    .field("fcfsOrder", servedCaseArray(result.fcfsOrder))
                    .field("priorityOrder", servedCaseArray(result.priorityOrder))
                    .field("fcfsAverageWait", result.fcfsAverageWait)
                    .field("priorityAverageWait", result.priorityAverageWait)
                    .close();
        });
    }

    private static TriageCase[] parseTriageCases(String[] entries) {
        TriageCase[] cases = new TriageCase[entries.length];
        for (int i = 0; i < entries.length; i++) {
            String[] fields = entries[i].split(":");
            cases[i] = new TriageCase(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
        }
        return cases;
    }

    private static Json servedCaseArray(ServedCase[] served) {
        Json array = Json.array();
        for (ServedCase s : served) {
            array.element(Json.object()
                    .field("caseId", s.caseId())
                    .field("arrivalTime", s.arrivalTime())
                    .field("severityPriority", s.severityPriority())
                    .field("waitTime", s.waitTime()));
        }
        return array;
    }

    // ------------------------------------------------------------------
    // T055 Indexing engine
    // ------------------------------------------------------------------

    private void handleIndexLookup(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String ref = requireParam(exchange, "ref");
            IndexingEngine engine = IndexingEngine.buildFromDatabase(connection);
            IndexingEngine.CaseRow row = engine.findByReference(ref);
            if (row == null) {
                return Json.object().field("found", false).close();
            }
            return Json.object()
                    .field("found", true)
                    .field("caseRef", row.caseRef())
                    .field("triageLevel", row.triageLevel())
                    .field("requestedAt", row.requestedAt())
                    .field("status", row.status())
                    .close();
        });
    }

    private void handleIndexRange(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String from = requireParam(exchange, "from");
            String to = requireParam(exchange, "to");
            IndexingEngine engine = IndexingEngine.buildFromDatabase(connection);
            List<IndexingEngine.CaseRow> rows = engine.findInTimeRange(from, to);

            Json array = Json.array();
            for (IndexingEngine.CaseRow row : rows) {
                array.element(Json.object()
                        .field("caseRef", row.caseRef())
                        .field("triageLevel", row.triageLevel())
                        .field("requestedAt", row.requestedAt())
                        .field("status", row.status()));
            }
            return Json.object().field("count", rows.size()).field("cases", array).close();
        });
    }

    // ------------------------------------------------------------------
    // T052 Knapsack DP
    // ------------------------------------------------------------------

    private void handleKnapsack(HttpExchange exchange) {
        respondJson(exchange, () -> {
            int[] weights = parseIntList(requireParam(exchange, "weights"));
            int[] values = parseIntList(requireParam(exchange, "values"));
            int capacity = Integer.parseInt(requireParam(exchange, "capacity"));

            KnapsackDP.KnapsackResult result = new KnapsackDP().solve(weights, values, capacity);

            Json selected = Json.array();
            for (int index : result.selectedIndices) {
                selected.element(Json.object()
                        .field("index", index)
                        .field("weight", weights[index])
                        .field("value", values[index]));
            }
            return Json.object().field("maxValue", result.maxValue).field("selected", selected).close();
        });
    }

    // ------------------------------------------------------------------
    // T039-T041 Sorting and searching
    // ------------------------------------------------------------------

    private void handleSort(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String algorithm = requireParam(exchange, "algorithm");
            Integer[] data = boxedArray(parseIntList(requireParam(exchange, "data")));

            Sorter<Integer> sorter = switch (algorithm) {
                case "merge" -> new MergeSort<>();
                case "quick" -> new QuickSort<>();
                case "insertion" -> new InsertionSort<>();
                case "selection" -> new SelectionSort<>();
                default -> throw new IllegalArgumentException("unknown sort algorithm: " + algorithm);
            };

            sorter.sort(data, Integer::compareTo);

            Json sorted = Json.array();
            for (int value : data) {
                sorted.element(value);
            }

            Json response = Json.object().field("algorithm", algorithm).field("sorted", sorted)
                    .field("isStable", sorter.isStable())
                    .field("isInPlace", sorter.isInPlace())
                    .field("bestCase", sorter.bestCaseComplexity())
                    .field("worstCase", sorter.worstCaseComplexity());
            if (sorter instanceof edu.ug.nexusb.core.Instrumented instrumented) {
                response.field("comparisons", instrumented.comparisonCount())
                        .field("movements", instrumented.movementCount());
            }
            return response.close();
        });
    }

    private void handleSearch(HttpExchange exchange) {
        respondJson(exchange, () -> {
            String algorithm = requireParam(exchange, "algorithm");
            Integer[] data = boxedArray(parseIntList(requireParam(exchange, "data")));
            int target = Integer.parseInt(requireParam(exchange, "target"));

            LinearBinarySearch<Integer> searcher = new LinearBinarySearch<>();
            int index = algorithm.equals("binary") ? searcher.binarySearch(data, target)
                    : searcher.linearSearch(data, target);

            return Json.object()
                    .field("algorithm", algorithm)
                    .field("index", index)
                    .field("found", index >= 0)
                    .close();
        });
    }

    private static int[] parseIntList(String csv) {
        String[] parts = csv.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i].trim());
        }
        return values;
    }

    private static Integer[] boxedArray(int[] values) {
        Integer[] boxed = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return boxed;
    }

    // ------------------------------------------------------------------
    // HTTP plumbing
    // ------------------------------------------------------------------

    private interface JsonSupplier {
        String get() throws Exception;
    }

    private void respondJson(HttpExchange exchange, JsonSupplier body) {
        try {
            String json = body.get();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (IllegalArgumentException | KeyNotFoundException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) {
        try {
            String json = Json.object().field("error", message == null ? "unknown error" : message).close();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (IOException ignored) {
            // response already broken; nothing more we can do
        }
    }

    private void sendText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String param(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            if (key.equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String requireParam(HttpExchange exchange, String name) {
        String value = param(exchange, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required parameter: " + name);
        }
        return value;
    }
}
