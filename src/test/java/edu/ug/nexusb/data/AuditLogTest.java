package edu.ug.nexusb.data;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.EmptyStackException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class AuditLogTest {

    @TempDir Path tempDir;
    private Connection conn;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"));
        applySchema(conn);
        auditLog = new AuditLog(conn);
    }

    @AfterEach
    void tearDown() throws SQLException { conn.close(); }

    @Test
    void undoLast_returnsOriginalEventAndWritesUndoneMarkerRow() throws SQLException {
        auditLog.record(AuditEvent.TRIAGED, AuditEvent.CASE, 42, null, "LEVEL_3");
        AuditEvent undone = auditLog.undoLast();

        assertEquals("LEVEL_3", undone.newState());
        AuditDao dao = new AuditDao(conn);
        assertEquals(2, dao.countAll()); // original + UNDONE marker
        assertEquals(AuditEvent.UNDONE, dao.findLatestForEntity(AuditEvent.CASE, 42).eventType());
    }

    @Test // boundary
    void undoLast_withNoHistory_throwsAndWritesNothing() throws SQLException {
        assertThrows(EmptyStackException.class, () -> auditLog.undoLast());
        assertEquals(0, new AuditDao(conn).countAll());
    }

    @Test // invalid input
    void record_rejectedByCheckConstraint_forUnknownEntityType() {
        assertThrows(SQLException.class,
            () -> auditLog.record(AuditEvent.TRIAGED, "NOT_AN_ENTITY", 1, null, "x"));
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