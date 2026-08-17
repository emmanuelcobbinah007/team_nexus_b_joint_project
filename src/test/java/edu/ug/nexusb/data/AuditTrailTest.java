package edu.ug.nexusb.data;

import org.junit.jupiter.api.Test;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;

class AuditTrailTest {

    @Test
    void undoLast_returnsMostRecentlyRecordedEventFirst() {
        AuditTrail trail = new AuditTrail();
        AuditEvent first = AuditEvent.of(AuditEvent.TRIAGED, AuditEvent.CASE, 1, null, "LEVEL_3");
        AuditEvent second = AuditEvent.of(AuditEvent.ASSIGNED, AuditEvent.ASSIGNMENT, 7, null, "RES_5");

        trail.record(first);
        trail.record(second);

        assertEquals(second, trail.undoLast()); // LIFO: last in, first undone
        assertEquals(first, trail.undoLast());
    }

    @Test
    void size_and_hasHistory_trackRecordedEvents() {
        AuditTrail trail = new AuditTrail();
        assertFalse(trail.hasHistory());
        assertEquals(0, trail.size());

        trail.record(AuditEvent.of(AuditEvent.TRIAGED, AuditEvent.CASE, 1, null, "LEVEL_2"));

        assertTrue(trail.hasHistory());
        assertEquals(1, trail.size());
    }

    @Test // boundary case
    void undoLast_onEmptyTrail_throwsEmptyStackException() {
        AuditTrail trail = new AuditTrail();
        assertThrows(EmptyStackException.class, trail::undoLast);
    }

    @Test // boundary case
    void undoLast_afterDrainingToEmpty_throwsOnNextCall() {
        AuditTrail trail = new AuditTrail();
        trail.record(AuditEvent.of(AuditEvent.TRIAGED, AuditEvent.CASE, 1, null, "LEVEL_1"));
        trail.undoLast();

        assertThrows(EmptyStackException.class, trail::undoLast);
    }

    @Test // invalid input
    void record_rejectsNullEvent() {
        AuditTrail trail = new AuditTrail();
        assertThrows(IllegalArgumentException.class, () -> trail.record(null));
    }
}
