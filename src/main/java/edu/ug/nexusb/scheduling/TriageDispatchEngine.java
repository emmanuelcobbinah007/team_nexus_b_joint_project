package edu.ug.nexusb.scheduling;

import edu.ug.nexusb.linear.BinaryHeapPriorityQueue;
import edu.ug.nexusb.linear.MyPriorityQueue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

// Wires the triage heap to case_request in the database:
//   cases (PENDING rows) -> triage heap -> status writes back to the DB
//
// Same DB_URL as DBLoader.java, on purpose - DBLoader opens and closes its
// OWN connection during --init-db, so by the time this engine runs, that
// connection is already gone. This class opens a fresh one to the same
// database file rather than trying to reuse DBLoader's (which isn't
// possible - it's already closed by then).
//
// NOTE ON java.util: DBLoader.java's HashMap usage is fine specifically
// because DBLoader lives in the data/ package, which the CI check exempts
// outright (see .github/workflows/build.yml) - not because "app plumbing"
// is exempt in general. This class lives in scheduling/, which isn't
// exempted, so loadPendingCases() below grows a plain array by hand instead.
public class TriageDispatchEngine implements AutoCloseable {

    private static final String DB_URL = "jdbc:sqlite:nexus.db";

    private final Connection connection;
    private final MyPriorityQueue<TriageCase> triageHeap;

    public TriageDispatchEngine(Connection connection) {
        if (connection == null) {
            throw new RuntimeException("connection cannot be null");
        }
        this.connection = connection;
        this.triageHeap = new BinaryHeapPriorityQueue<>(new TriageComparator());
    }

    // Convenience factory: opens a fresh connection to the same database
    // DBLoader.java builds, instead of the caller having to know the URL.
    public static TriageDispatchEngine connectToDefaultDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        return new TriageDispatchEngine(connection);
    }

    // Step 1: cases -> triage heap
    // Loads every PENDING case and heapifies them all at once (O(n)).
    public void loadPendingCases() throws SQLException {
        String sql = "SELECT case_id, triage_level, requested_at "
                + "FROM case_request WHERE status = 'PENDING'";

        TriageCase[] loaded = new TriageCase[16];
        int count = 0;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                int caseId = results.getInt("case_id");
                int triageLevel = results.getInt("triage_level");
                String requestedAt = results.getString("requested_at");
                if (count == loaded.length) {
                    loaded = Arrays.copyOf(loaded, loaded.length * 2);
                }
                loaded[count++] = new TriageCase(caseId, triageLevel, requestedAt);
            }
        }

        TriageCase[] items = Arrays.copyOf(loaded, count);
        triageHeap.heapify(items);
    }

    // Step 2: triage heap -> status writes
    // Pulls the single most urgent case off the heap, marks it TRIAGED in
    // the database, and logs the change. Returns null if nothing is left.
    public TriageCase dispatchNext() throws SQLException {
        if (triageHeap.isEmpty()) {
            return null;
        }

        TriageCase next = triageHeap.extractTop();
        updateStatus(next.getCaseId(), "TRIAGED");
        logAuditEvent(next.getCaseId(), "PENDING", "TRIAGED");
        return next;
    }

    // Lets a case that arrives after the initial load join the heap
    // without reloading everything from the database.
    public void addCase(TriageCase newCase) {
        triageHeap.insert(newCase);
    }

    public boolean isEmpty() {
        return triageHeap.isEmpty();
    }

    public int size() {
        return triageHeap.size();
    }

    // Closes the connection this engine opened. Only call this if you got
    // the connection from connectToDefaultDatabase() - if the caller handed
    // you their own connection via the constructor, THEY own closing it,
    // not you.
    @Override
    public void close() throws SQLException {
        connection.close();
    }

    // ---- private DB-writing helpers ----

    private void updateStatus(int caseId, String newStatus) throws SQLException {
        String sql = "UPDATE case_request SET status = ? WHERE case_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setInt(2, caseId);
            statement.executeUpdate();
        }
    }

    private void logAuditEvent(int caseId, String previousState, String newState) throws SQLException {
        String sql = "INSERT INTO audit_event "
                + "(event_type, entity_type, entity_id, previous_state, new_state, occurred_at) "
                + "VALUES ('STATUS_CHANGED', 'CASE', ?, ?, ?, datetime('now'))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);
            statement.setString(2, previousState);
            statement.setString(3, newState);
            statement.executeUpdate();
        }
    }
}