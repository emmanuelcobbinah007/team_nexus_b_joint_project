package edu.ug.nexusb.data;

import java.time.Instant;

public record AuditEvent(
        Long eventId, String eventType, String entityType, int entityId,
        String previousState, String newState, String occurredAt) {

    public static final String TRIAGED = "TRIAGED";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";
    public static final String UNDONE = "UNDONE";

    public static final String CASE = "CASE";
    public static final String RESOURCE = "RESOURCE";
    public static final String ASSIGNMENT = "ASSIGNMENT";

    public AuditEvent {
        if (eventType == null || entityType == null) {
            throw new IllegalArgumentException("eventType and entityType are required");
        }
    }

    public static AuditEvent of(String eventType, String entityType, int entityId,
            String previousState, String newState) {
        return new AuditEvent(null, eventType, entityType, entityId, previousState, newState,
                Instant.now().toString());
    }

    public AuditEvent withEventId(long id) {
        return new AuditEvent(id, eventType, entityType, entityId, previousState, newState, occurredAt);
    }
}
