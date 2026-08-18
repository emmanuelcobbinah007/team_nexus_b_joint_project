package edu.ug.nexusb.app;

import edu.ug.nexusb.app.IndexingEngine.CaseRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingEngineTest {

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void findByReferenceReturnsTheMatchingRow() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING"),
                new CaseRow("REQ0002", 4, "2026-07-04 05:00:00", "ASSIGNED")));

        CaseRow found = engine.findByReference("REQ0002");

        assertEquals("REQ0002", found.caseRef());
        assertEquals(4, found.triageLevel());
        assertEquals("ASSIGNED", found.status());
        assertEquals(2, engine.caseCount());
    }

    @Test
    void findInTimeRangeReturnsOnlyRowsWithinInclusiveBounds() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-01 00:00:00", "PENDING"),
                new CaseRow("REQ0002", 2, "2026-07-05 00:00:00", "PENDING"),
                new CaseRow("REQ0003", 3, "2026-07-10 00:00:00", "PENDING"),
                new CaseRow("REQ0004", 4, "2026-07-15 00:00:00", "PENDING")));

        List<CaseRow> inRange = engine.findInTimeRange("2026-07-05 00:00:00", "2026-07-10 00:00:00");

        assertEquals(2, inRange.size());
        assertTrue(inRange.stream().anyMatch(r -> r.caseRef().equals("REQ0002")));
        assertTrue(inRange.stream().anyMatch(r -> r.caseRef().equals("REQ0003")));
    }

    @Test
    void casesSharingAnExactTimestampAreAllRetrievable() {
        // The whole reason CaseGroup exists: a naive timestamp -> CaseRow
        // map would silently drop one of these on a duplicate-key put().
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING"),
                new CaseRow("REQ0002", 2, "2026-07-04 04:46:00", "PENDING"),
                new CaseRow("REQ0003", 3, "2026-07-04 04:46:00", "PENDING")));

        List<CaseRow> sameTimestamp = engine.findInTimeRange("2026-07-04 04:46:00", "2026-07-04 04:46:00");

        assertEquals(3, sameTimestamp.size());
        assertTrue(sameTimestamp.stream().anyMatch(r -> r.caseRef().equals("REQ0001")));
        assertTrue(sameTimestamp.stream().anyMatch(r -> r.caseRef().equals("REQ0002")));
        assertTrue(sameTimestamp.stream().anyMatch(r -> r.caseRef().equals("REQ0003")));
        assertEquals(3, engine.caseCount());
    }

    @Test
    void indexAccessorsExposeDiagnostics() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING")));

        assertEquals(1, engine.referenceIndex().size());
        assertEquals(1, engine.timeIndex().size());
        assertTrue(engine.timeIndex().isBalanced());
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void emptyEngineHasNoCasesAndAnswersEmpty() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of());

        assertEquals(0, engine.caseCount());
        assertNull(engine.findByReference("REQ0001"));
        assertEquals(0, engine.findInTimeRange("2026-01-01 00:00:00", "2026-12-31 23:59:59").size());
    }

    @Test
    void findInTimeRangeWithNoMatchesReturnsEmpty() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING")));

        assertEquals(0, engine.findInTimeRange("2027-01-01 00:00:00", "2027-12-31 23:59:59").size());
    }

    @Test
    void findInTimeRangeWithFromAfterToReturnsEmpty() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING")));

        assertEquals(0, engine.findInTimeRange("2026-12-01 00:00:00", "2026-01-01 00:00:00").size());
    }

    @Test
    void findByReferenceForMissingCaseReturnsNull() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING")));

        assertNull(engine.findByReference("REQ9999"));
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void findByReferenceRejectsNull() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of());
        assertThrows(IllegalArgumentException.class, () -> engine.findByReference(null));
    }

    @Test
    void findInTimeRangeRejectsNullBounds() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of());
        assertThrows(IllegalArgumentException.class, () -> engine.findInTimeRange(null, "2026-01-01 00:00:00"));
        assertThrows(IllegalArgumentException.class, () -> engine.findInTimeRange("2026-01-01 00:00:00", null));
    }

    @Test
    void caseGroupGetRejectsOutOfRangeIndex() {
        IndexingEngine engine = IndexingEngine.buildFromRows(List.of(
                new CaseRow("REQ0001", 1, "2026-07-04 04:46:00", "PENDING")));
        IndexingEngine.CaseGroup group = engine.timeIndex().get("2026-07-04 04:46:00");

        assertThrows(IndexOutOfBoundsException.class, () -> group.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> group.get(1));
    }
}
