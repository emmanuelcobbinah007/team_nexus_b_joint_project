package edu.ug.nexusb.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public final class AuditDao {

    private final Connection conn;

    public AuditDao(Connection conn) {
        this.conn = conn;
    }

    
    public AuditEvent insert(AuditEvent event) throws SQLException {
        String sql = "INSERT INTO audit_event "
            + "(event_type, entity_type, entity_id, previous_state, new_state, occurred_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, event.eventType());
            ps.setString(2, event.entityType());
            ps.setInt(3, event.entityId());
            ps.setString(4, event.previousState());
            ps.setString(5, event.newState());
            ps.setString(6, event.occurredAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return event.withEventId(keys.getLong(1));
            }
        }
    }


    public AuditEvent findLatestForEntity(String entityType, int entityId) throws SQLException {
        String sql = "SELECT event_id, event_type, entity_type, entity_id, "
            + "previous_state, new_state, occurred_at FROM audit_event "
            + "WHERE entity_type = ? AND entity_id = ? "
            + "ORDER BY event_id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AuditEvent(
                    rs.getLong("event_id"),
                    rs.getString("event_type"),
                    rs.getString("entity_type"),
                    rs.getInt("entity_id"),
                    rs.getString("previous_state"),
                    rs.getString("new_state"),
                    rs.getString("occurred_at"));
            }
        }
    }

    public int countAll() throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM audit_event")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}