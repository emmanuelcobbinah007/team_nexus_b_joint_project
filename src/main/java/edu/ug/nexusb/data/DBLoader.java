package edu.ug.nexusb.data;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * DBLoader
 * --------
 * Builds health.db from data/schema.sql, then loads:
 *   data/locations.csv -> facility
 *   data/roads.csv      -> road_link
 *   data/resources.csv  -> resource
 *   data/request.csv    -> case_request
 *
 * facility_id in the schema is an internal AUTOINCREMENT integer, but
 * roads.csv / resources.csv / request.csv reference facilities by their
 * text "code" (e.g. F001). This loader inserts facility rows first,
 * captures the code -> generated integer id mapping, then uses that
 * map to resolve foreign keys on every later insert.
 *
 * Entry point: called from edu.ug.nexusb.app.App when run with --init-db
 */
public class DBLoader {

    private static final String DB_URL = "jdbc:sqlite:health.db";
    private static final String DATA_DIR = "data/";

    private static final Map<String, Integer> facilityIdByCode = new HashMap<>();

    /** Runs the full build-and-load sequence. Called by App.main(). */
    public static void run() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            System.out.println("Connected to " + DB_URL);

            runSchema(conn, DATA_DIR + "schema.sql");
            conn.commit();
            System.out.println("Schema created.\n");

            int facilityRows = loadFacilities(conn, DATA_DIR + "locations.csv");
            conn.commit();

            int roadRows = loadRoadLinks(conn, DATA_DIR + "roads.csv");
            conn.commit();

            int resourceRows = loadResources(conn, DATA_DIR + "resources.csv");
            conn.commit();

            int caseRows = loadCaseRequests(conn, DATA_DIR + "request.csv");
            conn.commit();

            System.out.println("\n=== Row count verification ===");
            verifyCounts(conn, "facility", facilityRows);
            verifyCounts(conn, "road_link", roadRows);
            verifyCounts(conn, "resource", resourceRows);
            verifyCounts(conn, "case_request", caseRows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSchema(Connection conn, String schemaPath) throws IOException, SQLException {
        String sql = Files.readString(Path.of(schemaPath));
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (line.trim().startsWith("--")) continue;
            cleaned.append(line).append("\n");
        }
        try (Statement stmt = conn.createStatement()) {
            for (String statement : cleaned.toString().split(";")) {
                String s = statement.trim();
                if (!s.isEmpty()) stmt.execute(s);
            }
        }
    }

    private static int loadFacilities(Connection conn, String csvPath) throws IOException, SQLException, com.opencsv.exceptions.CsvValidationException {
        String insertSql = "INSERT INTO facility " +
                "(code, name, facility_type, district, latitude, longitude, bed_capacity, has_emergency, has_theatre, opens_24h, care_level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int count = 0;
        try (CSVReader reader = new CSVReader(new FileReader(csvPath));
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            reader.readNext();
            String[] row;
            while ((row = reader.readNext()) != null) {
                String code = row[0];
                ps.setString(1, code);
                ps.setString(2, row[1]);
                ps.setString(3, row[2]);
                ps.setString(4, row[3]);
                ps.setDouble(5, Double.parseDouble(row[4]));
                ps.setDouble(6, Double.parseDouble(row[5]));
                ps.setInt(7, Integer.parseInt(row[6]));
                ps.setInt(8, Integer.parseInt(row[7]));
                ps.setInt(9, Integer.parseInt(row[8]));
                ps.setInt(10, Integer.parseInt(row[9]));
                ps.setInt(11, Integer.parseInt(row[10]));
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    if (keys.next()) facilityIdByCode.put(code, keys.getInt(1));
                }
                count++;
            }
        }
        System.out.println("Loaded " + count + " rows into facility (from " + csvPath + ")");
        return count;
    }

    private static int loadRoadLinks(Connection conn, String csvPath) throws IOException, SQLException, com.opencsv.exceptions.CsvValidationException {
        String insertSql = "INSERT INTO road_link " +
                "(from_facility_id, to_facility_id, distance_km, base_time_min, traffic_weight, road_condition, is_one_way, route_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int count = 0, skipped = 0;
        try (CSVReader reader = new CSVReader(new FileReader(csvPath));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            reader.readNext();
            String[] row;
            while ((row = reader.readNext()) != null) {
                Integer fromId = facilityIdByCode.get(row[1]);
                Integer toId = facilityIdByCode.get(row[2]);
                if (fromId == null || toId == null) {
                    System.out.println("  SKIPPED road_link row (link_id=" + row[0] + "): unresolved code");
                    skipped++;
                    continue;
                }
                ps.setInt(1, fromId);
                ps.setInt(2, toId);
                ps.setDouble(3, Double.parseDouble(row[3]));
                ps.setDouble(4, Double.parseDouble(row[4]));
                ps.setDouble(5, Double.parseDouble(row[5]));
                ps.setString(6, row[6]);
                ps.setInt(7, Integer.parseInt(row[7]));
                String routeName = (row.length > 8 && !row[8].isBlank()) ? row[8] : null;
                ps.setString(8, routeName);
                ps.executeUpdate();
                count++;
            }
        }
        System.out.println("Loaded " + count + " rows into road_link (skipped " + skipped + ") (from " + csvPath + ")");
        return count;
    }

    private static int loadResources(Connection conn, String csvPath) throws IOException, SQLException, com.opencsv.exceptions.CsvValidationException {
        String insertSql = "INSERT INTO resource " +
                "(code, resource_type, home_facility_id, capacity, care_level, available_from, available_to, shift_minutes, is_available) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int count = 0, skipped = 0;
        try (CSVReader reader = new CSVReader(new FileReader(csvPath));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            reader.readNext();
            String[] row;
            while ((row = reader.readNext()) != null) {
                Integer homeFacilityId = facilityIdByCode.get(row[3]);
                if (homeFacilityId == null) {
                    System.out.println("  SKIPPED resource row (code=" + row[1] + "): unresolved home_facility_id");
                    skipped++;
                    continue;
                }
                ps.setString(1, row[1]);
                ps.setString(2, row[2]);
                ps.setInt(3, homeFacilityId);
                ps.setInt(4, Integer.parseInt(row[4]));
                ps.setInt(5, Integer.parseInt(row[5]));
                ps.setString(6, row[6]);
                ps.setString(7, row[7]);
                ps.setInt(8, Integer.parseInt(row[8]));
                ps.setInt(9, Integer.parseInt(row[9]));
                ps.executeUpdate();
                count++;
            }
        }
        System.out.println("Loaded " + count + " rows into resource (skipped " + skipped + ") (from " + csvPath + ")");
        return count;
    }

    private static int loadCaseRequests(Connection conn, String csvPath) throws IOException, SQLException, com.opencsv.exceptions.CsvValidationException {
        String insertSql = "INSERT INTO case_request " +
                "(case_ref, origin_facility_id, destination_facility_id, case_type, triage_level, age_band, " +
                "requested_at, response_window_min, service_time_min, required_care_level, status, assigned_resource_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int count = 0, skipped = 0;
        try (CSVReader reader = new CSVReader(new FileReader(csvPath));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            reader.readNext();
            String[] row;
            while ((row = reader.readNext()) != null) {
                String caseRef = row[1];
                Integer originId = facilityIdByCode.get(row[2]);
                String destCode = row[3];
                Integer destId = destCode.isBlank() ? null : facilityIdByCode.get(destCode);
                if (originId == null) {
                    System.out.println("  SKIPPED case_request row (case_ref=" + caseRef + "): unresolved origin");
                    skipped++;
                    continue;
                }
                ps.setString(1, caseRef);
                ps.setInt(2, originId);
                if (destId != null) ps.setInt(3, destId); else ps.setNull(3, java.sql.Types.INTEGER);
                ps.setString(4, row[4]);
                ps.setInt(5, Integer.parseInt(row[5]));
                ps.setString(6, row[6]);
                ps.setString(7, row[7]);
                ps.setInt(8, Integer.parseInt(row[8]));
                ps.setInt(9, Integer.parseInt(row[9]));
                ps.setInt(10, Integer.parseInt(row[10]));
                ps.setString(11, row[11]);
                ps.setNull(12, java.sql.Types.INTEGER);
                ps.executeUpdate();
                count++;
            }
        }
        System.out.println("Loaded " + count + " rows into case_request (skipped " + skipped + ") (from " + csvPath + ")");
        return count;
    }

    private static void verifyCounts(Connection conn, String table, int expected) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table);
            rs.next();
            int actual = rs.getInt(1);
            String status = (actual == expected) ? "OK" : "MISMATCH";
            System.out.printf("%-15s expected=%-5d actual_in_db=%-5d [%s]%n", table, expected, actual, status);
        }
    }
}
