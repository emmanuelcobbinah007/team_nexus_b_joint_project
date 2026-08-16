package edu.ug.nexusb.app;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.trees.ChainedHashTable;
import edu.ug.nexusb.trees.MyHashTable;
import edu.ug.nexusb.trees.MyTree;
import edu.ug.nexusb.trees.RedBlackTree;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Indexes the {@code case_request} table two ways over the same rows, so
 * the two capabilities the README commits to - "look up a case by
 * reference" and "query admissions by time range" - each go through the
 * structure actually suited to it, rather than one index pretending to
 * serve both:
 *
 * <ul>
 *   <li>{@link #findByReference} - a {@link ChainedHashTable} keyed by
 *       {@code case_ref}, O(1) average case.
 *   <li>{@link #findInTimeRange} - a {@link RedBlackTree} keyed by
 *       {@code requested_at}, using {@link MyTree#rangeKeys} to answer a
 *       range query a hash table cannot.
 * </ul>
 *
 * <p>Split into a database-reading half ({@link #buildFromDatabase}) and a
 * pure logic half ({@link #buildFromRows}), the same shape {@code GraphBuilder}
 * already uses, so the indexing logic itself can be unit-tested with
 * hand-written rows, independent of a live database.
 */
public final class IndexingEngine {

    /** One {@code case_request} row, already read out of the database. */
    public record CaseRow(String caseRef, int triageLevel, String requestedAt, String status) {
    }

    /**
     * Every case sharing one {@code requested_at} timestamp.
     *
     * <p>A plain {@code timestamp -> CaseRow} tree would silently overwrite
     * one case with another if two ever shared an exact timestamp, since
     * {@code MyMap.put} replaces on a duplicate key. Grouping into a bucket
     * keeps every case reachable regardless of how many share a timestamp.
     */
    public static final class CaseGroup {
        private CaseRow[] rows = new CaseRow[2];
        private int count;

        private void add(CaseRow row) {
            if (count == rows.length) {
                rows = Arrays.copyOf(rows, rows.length * 2);
            }
            rows[count++] = row;
        }

        /** @return how many cases share this timestamp */
        public int size() {
            return count;
        }

        /**
         * @param index position within this group, {@code 0} to {@link #size()} - 1
         * @return the case at that position
         * @throws IndexOutOfBoundsException if {@code index} is out of range
         */
        public CaseRow get(int index) {
            if (index < 0 || index >= count) {
                throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + count);
            }
            return rows[index];
        }
    }

    private final MyHashTable<String, CaseRow> byReference = new ChainedHashTable<>();
    private final MyTree<String, CaseGroup> byRequestedAt = new RedBlackTree<>(stringOrder());

    private IndexingEngine() {
    }

    private static MyComparator<String> stringOrder() {
        return String::compareTo;
    }

    /**
     * Builds both indexes from the given database connection.
     *
     * @param conn open JDBC connection to the project's SQLite database
     * @return an engine with every {@code case_request} row indexed both ways
     * @throws SQLException if the query fails
     */
    public static IndexingEngine buildFromDatabase(Connection conn) throws SQLException {
        return buildFromRows(readCaseRows(conn));
    }

    /**
     * Pure logic: builds both indexes from already-fetched rows, with no
     * database access at all. This is what tests call directly with
     * hand-written data.
     *
     * @param rows the rows to index
     * @return an engine with every given row indexed both ways
     */
    public static IndexingEngine buildFromRows(List<CaseRow> rows) {
        IndexingEngine engine = new IndexingEngine();
        for (CaseRow row : rows) {
            engine.index(row);
        }
        return engine;
    }

    private void index(CaseRow row) {
        byReference.put(row.caseRef(), row);
        CaseGroup group = byRequestedAt.get(row.requestedAt());
        if (group == null) {
            group = new CaseGroup();
            byRequestedAt.put(row.requestedAt(), group);
        }
        group.add(row);
    }

    /**
     * Looks up a single case by its reference - the hash index, O(1) average case.
     *
     * @param caseRef the case reference to look up (e.g. {@code "REQ0001"})
     * @return the matching row, or {@code null} if no case has that reference
     */
    public CaseRow findByReference(String caseRef) {
        return byReference.get(caseRef);
    }

    /**
     * Returns every case requested within {@code [from, to]}, inclusive -
     * the tree index's range query, the operation the hash index cannot do.
     *
     * @param from the lower bound, inclusive (e.g. {@code "2026-07-01 00:00:00"})
     * @param to the upper bound, inclusive
     * @return every matching case, grouped by timestamp in ascending order;
     *     empty if none match
     */
    public List<CaseRow> findInTimeRange(String from, String to) {
        CaseRow[] buffer = new CaseRow[16];
        int size = 0;
        for (var it = byRequestedAt.rangeKeys(from, to).iterator(); it.hasNext(); ) {
            String timestamp = it.next();
            CaseGroup group = byRequestedAt.get(timestamp);
            for (int i = 0; i < group.size(); i++) {
                if (size == buffer.length) {
                    buffer = Arrays.copyOf(buffer, buffer.length * 2);
                }
                buffer[size++] = group.get(i);
            }
        }
        return List.of(Arrays.copyOf(buffer, size));
    }

    /** @return how many cases are indexed */
    public int caseCount() {
        return byReference.size();
    }

    /** @return the hash index, for direct inspection (e.g. {@code loadFactor()}, {@code collisionCount()}) */
    public MyHashTable<String, CaseRow> referenceIndex() {
        return byReference;
    }

    /** @return the tree index, for direct inspection (e.g. {@code height()}, {@code isBalanced()}) */
    public MyTree<String, CaseGroup> timeIndex() {
        return byRequestedAt;
    }

    private static List<CaseRow> readCaseRows(Connection conn) throws SQLException {
        CaseRow[] rows = new CaseRow[16];
        int size = 0;
        String sql = "SELECT case_ref, triage_level, requested_at, status FROM case_request";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (size == rows.length) {
                    rows = Arrays.copyOf(rows, rows.length * 2);
                }
                rows[size++] = new CaseRow(
                        rs.getString("case_ref"),
                        rs.getInt("triage_level"),
                        rs.getString("requested_at"),
                        rs.getString("status"));
            }
        }
        return List.of(Arrays.copyOf(rows, size));
    }
}
