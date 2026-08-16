package edu.ug.nexusb.app;

import edu.ug.nexusb.app.IndexingEngine.CaseRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises {@link IndexingEngine#buildFromDatabase} against the project's
 * real schema ({@code data/schema.sql}), same reasoning as
 * {@code GraphBuilderDatabaseIntegrationTest}: a throwaway SQLite database
 * per run, built from the one schema file the rest of the project actually
 * uses, so this can never drift from it.
 */
class IndexingEngineDatabaseIntegrationTest {

    @TempDir
    Path tempDir;

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        Path dbFile = tempDir.resolve("test.db");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        applySchema(conn);
        insertFacility(1, "F001");
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    @Test
    void buildsBothIndexesFromRealCaseRequestRows() throws SQLException {
        insertCaseRequest("REQ0001", 1, "EMERGENCY_TRANSPORT", 1, "2026-07-04 04:46:00");
        insertCaseRequest("REQ0002", 1, "OUTPATIENT_APPOINTMENT", 4, "2026-07-05 09:00:00");

        IndexingEngine engine = IndexingEngine.buildFromDatabase(conn);

        assertEquals(2, engine.caseCount());
        CaseRow found = engine.findByReference("REQ0001");
        assertNotNull(found);
        assertEquals(1, found.triageLevel());

        List<CaseRow> inRange = engine.findInTimeRange("2026-07-04 00:00:00", "2026-07-04 23:59:59");
        assertEquals(1, inRange.size());
        assertEquals("REQ0001", inRange.get(0).caseRef());
    }

    @Test
    void emptyDatabaseProducesAnEmptyEngine() throws SQLException {
        IndexingEngine engine = IndexingEngine.buildFromDatabase(conn);

        assertEquals(0, engine.caseCount());
    }

    private void insertFacility(int id, String code) throws SQLException {
        String sql = "INSERT INTO facility "
            + "(facility_id, code, name, facility_type, district, latitude, longitude, care_level) "
            + "VALUES (" + id + ", '" + code + "', '" + code + " Facility', 'DISTRICT_HOSPITAL', "
            + "'Test District', 5.6, -0.2, 2)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void insertCaseRequest(String caseRef, int originFacilityId, String caseType,
            int triageLevel, String requestedAt) throws SQLException {
        String sql = "INSERT INTO case_request "
            + "(case_ref, origin_facility_id, case_type, triage_level, age_band, requested_at, "
            + "response_window_min, service_time_min, required_care_level, status) VALUES ("
            + "'" + caseRef + "', " + originFacilityId + ", '" + caseType + "', " + triageLevel + ", "
            + "'15-39', '" + requestedAt + "', 30, 20, 2, 'PENDING')";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void applySchema(Connection conn) throws SQLException, IOException {
        String schema = Files.readString(Path.of("data", "schema.sql"), StandardCharsets.UTF_8);
        String withoutComments = stripCommentLines(schema);
        try (Statement stmt = conn.createStatement()) {
            for (String statement : withoutComments.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static String stripCommentLines(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.strip().startsWith("--")) {
                result.append(line).append('\n');
            }
        }
        return result.toString();
    }
}
