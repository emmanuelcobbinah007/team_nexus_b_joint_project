package edu.ug.nexusb.data;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class AuditDaoTest {

    @TempDir Path tempDir;
    private Connection conn;
    private AuditDao dao;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"));
        applySchema(conn);
        dao = new AuditDao(conn);
    }

    @AfterEach
    void tearDown() throws SQLException { conn.close(); }

    @Test
    void insert_persistsRowAndFillsGeneratedEventId() throws SQLException {
        AuditEvent saved = dao.insert(AuditEvent.of(AuditEvent.TRIAGED, AuditEvent.CASE, 42, null, "LEVEL_2"));
        assertNotNull(saved.eventId());
        assertEquals(1, dao.countAll());
    }

    @Test
    void findLatestForEntity_returnsMostRecentRowForThatEntity() throws SQLException {
        dao.insert(AuditEvent.of(AuditEvent.TRIAGED, AuditEvent.CASE, 42, null, "LEVEL_3"));
        dao.insert(AuditEvent.of(AuditEvent.STATUS_CHANGED, AuditEvent.CASE, 42, "LEVEL_3", "LEVEL_1"));
        dao.insert(AuditEvent.of(AuditEvent.TRIAGED, AuditEvent.CASE, 99, null, "LEVEL_4")); // different entity

        AuditEvent latest = dao.findLatestForEntity(AuditEvent.CASE, 42);
        assertEquals(AuditEvent.STATUS_CHANGED, latest.eventType());
        assertEquals("LEVEL_1", latest.newState());
    }

    @Test // boundary
    void findLatestForEntity_withNoRows_returnsNull() throws SQLException {
        assertNull(dao.findLatestForEntity(AuditEvent.CASE, 1));
    }

    @Test // invalid input — violates the event_type CHECK constraint
    void insert_rejectedByCheckConstraint_forUnknownEventType() {
        AuditEvent bad = new AuditEvent(null, "NOT_A_REAL_TYPE", AuditEvent.CASE, 1, null, "x",
            "2026-01-01T00:00:00Z");
        assertThrows(SQLException.class, () -> dao.insert(bad));
    }

    private static void applySchema(Connection conn) throws SQLException, IOException {
        String schema = Files.readString(Path.of("data", "schema.sql"), StandardCharsets.UTF_8);
        StringBuilder noComments = new StringBuilder();
        for (String line : schema.split("\n")) {
            if (!line.strip().startsWith("--")) noComments.append(line).append('\n');
        }
        try (Statement stmt = conn.createStatement()) {
            for (String stmtText : noComments.toString().split(";")) {
                String trimmed = stmtText.trim();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
        }
    }
}
