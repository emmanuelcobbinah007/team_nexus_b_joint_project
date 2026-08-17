package edu.ug.nexusb.data;

import java.sql.Connection;
import java.sql.SQLException;


public final class AuditLog {

    private final AuditTrail trail;
    private final AuditDao dao;

    public AuditLog(Connection conn) {
        this.trail = new AuditTrail();
        this.dao = new AuditDao(conn);
    }

    /** Records a decision: pushes it onto the in-memory stack and persists it. */
    public AuditEvent record(String eventType, String entityType, int entityId,
            String previousState, String newState) throws SQLException {
        AuditEvent event = AuditEvent.of(eventType, entityType, entityId, previousState, newState);
        AuditEvent saved = dao.insert(event);
        trail.record(saved);
        return saved;
    }

    /**
     * Reverses the most recent decision: pops it off the stack, writes an
     * UNDONE marker row, and returns the original event so the caller
     * (the app layer) can apply the actual state reversal — e.g. set the
     * case's triage level back to previousState.
     *
     * @throws java.util.EmptyStackException if there's nothing to undo
     */
    public AuditEvent undoLast() throws SQLException {
        AuditEvent undone = trail.undoLast();
        dao.insert(AuditEvent.of(AuditEvent.UNDONE, undone.entityType(), undone.entityId(),
            undone.newState(), undone.previousState()));
        return undone;
    }

    public boolean hasHistory() {
        return trail.hasHistory();
    }
}