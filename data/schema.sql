-- ============================================================
-- Hospital & Clinic Operations Optimizer
-- Team Nexus B (Group 29) - DCIT 204/308 Joint DSA Project
-- Target: SQLite 3
--
-- OWNER: Sub-team A (Victor Barnieh)
-- STATUS: Week 1 draft - review and lock by Fri 31 July 2026
--
-- NOTE: All case data loaded into this schema is SYNTHETIC.
--       No patient-identifiable fields exist by design.
-- ============================================================

PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS algorithm_run;
DROP TABLE IF EXISTS audit_event;
DROP TABLE IF EXISTS assignment;
DROP TABLE IF EXISTS case_request;
DROP TABLE IF EXISTS resource;
DROP TABLE IF EXISTS road_link;
DROP TABLE IF EXISTS facility;

-- ---------- 1. FACILITY (graph vertices) --------------------
CREATE TABLE facility (
    facility_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    name            TEXT    NOT NULL,
    facility_type   TEXT    NOT NULL CHECK (facility_type IN (
                        'TEACHING_HOSPITAL','REGIONAL_HOSPITAL','DISTRICT_HOSPITAL',
                        'POLYCLINIC','HEALTH_CENTRE','CHPS_COMPOUND','AMBULANCE_STATION',
                        'BLOOD_BANK','CENTRAL_LAB','OXYGEN_DEPOT','MEDICAL_STORE')),
    district        TEXT    NOT NULL,
    latitude        REAL    NOT NULL CHECK (latitude  BETWEEN  -90 AND  90),
    longitude       REAL    NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    bed_capacity    INTEGER NOT NULL DEFAULT 0 CHECK (bed_capacity >= 0),
    has_emergency   INTEGER NOT NULL DEFAULT 0 CHECK (has_emergency IN (0,1)),
    has_theatre     INTEGER NOT NULL DEFAULT 0 CHECK (has_theatre   IN (0,1)),
    opens_24h       INTEGER NOT NULL DEFAULT 0 CHECK (opens_24h     IN (0,1)),
    care_level      INTEGER NOT NULL CHECK (care_level BETWEEN 1 AND 4)
);

CREATE INDEX idx_facility_district ON facility(district);
CREATE INDEX idx_facility_type     ON facility(facility_type);

-- ---------- 2. ROAD_LINK (weighted graph edges) -------------
CREATE TABLE road_link (
    link_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    from_facility_id   INTEGER NOT NULL REFERENCES facility(facility_id),
    to_facility_id     INTEGER NOT NULL REFERENCES facility(facility_id),
    distance_km        REAL    NOT NULL CHECK (distance_km  > 0),
    base_time_min      REAL    NOT NULL CHECK (base_time_min > 0),
    traffic_weight     REAL    NOT NULL DEFAULT 1.0 CHECK (traffic_weight BETWEEN 1.0 AND 3.0),
    road_condition     TEXT    NOT NULL DEFAULT 'GOOD'
                               CHECK (road_condition IN ('GOOD','FAIR','POOR')),
    is_one_way         INTEGER NOT NULL DEFAULT 0 CHECK (is_one_way IN (0,1)),
    route_name         TEXT,
    CHECK (from_facility_id <> to_facility_id),
    UNIQUE (from_facility_id, to_facility_id)
);

CREATE INDEX idx_link_from ON road_link(from_facility_id);
CREATE INDEX idx_link_to   ON road_link(to_facility_id);

-- Convenience view: effective travel time used by Dijkstra / Prim / Kruskal
CREATE VIEW v_weighted_edge AS
SELECT link_id,
       from_facility_id,
       to_facility_id,
       distance_km,
       ROUND(base_time_min * traffic_weight *
             CASE road_condition WHEN 'GOOD' THEN 1.00
                                 WHEN 'FAIR' THEN 1.15
                                 ELSE 1.35 END, 2) AS effective_time_min,
       is_one_way
FROM road_link;

-- ---------- 3. RESOURCE (supply side) -----------------------
CREATE TABLE resource (
    resource_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    code             TEXT    NOT NULL UNIQUE,
    resource_type    TEXT    NOT NULL CHECK (resource_type IN (
                         'AMBULANCE','RESPONSE_TEAM','THEATRE_SLOT',
                         'ICU_BED_BLOCK','LAB_COURIER','SUPPLY_VEHICLE')),
    home_facility_id INTEGER NOT NULL REFERENCES facility(facility_id),
    capacity         INTEGER NOT NULL DEFAULT 1 CHECK (capacity >= 1),
    care_level       INTEGER NOT NULL CHECK (care_level BETWEEN 1 AND 4),
    available_from   TEXT    NOT NULL DEFAULT '00:00',
    available_to     TEXT    NOT NULL DEFAULT '23:59',
    shift_minutes    INTEGER NOT NULL DEFAULT 480 CHECK (shift_minutes > 0),
    is_available     INTEGER NOT NULL DEFAULT 1 CHECK (is_available IN (0,1))
);

CREATE INDEX idx_resource_home ON resource(home_facility_id);
CREATE INDEX idx_resource_type ON resource(resource_type);

-- ---------- 4. CASE_REQUEST (SYNTHETIC workload) ------------
-- No name, contact, address, ID number, or clinical notes exist here.
CREATE TABLE case_request (
    case_id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    case_ref                TEXT    NOT NULL UNIQUE,
    origin_facility_id      INTEGER NOT NULL REFERENCES facility(facility_id),
    destination_facility_id INTEGER          REFERENCES facility(facility_id),
    case_type               TEXT    NOT NULL CHECK (case_type IN (
                                'EMERGENCY_TRANSPORT','INTER_FACILITY_REFERRAL',
                                'LAB_SAMPLE_TRANSPORT','BLOOD_DELIVERY','OXYGEN_RESUPPLY',
                                'OUTPATIENT_APPOINTMENT','ELECTIVE_PROCEDURE')),
    triage_level            INTEGER NOT NULL CHECK (triage_level BETWEEN 1 AND 5),
    age_band                TEXT    NOT NULL CHECK (age_band IN ('0-4','5-14','15-39','40-64','65+')),
    requested_at            TEXT    NOT NULL,
    response_window_min     INTEGER NOT NULL CHECK (response_window_min > 0),
    service_time_min        INTEGER NOT NULL CHECK (service_time_min   > 0),
    required_care_level     INTEGER NOT NULL CHECK (required_care_level BETWEEN 1 AND 4),
    status                  TEXT    NOT NULL DEFAULT 'PENDING' CHECK (status IN (
                                'PENDING','TRIAGED','ASSIGNED','IN_TRANSIT',
                                'COMPLETED','MISSED_WINDOW','CANCELLED')),
    assigned_resource_id    INTEGER          REFERENCES resource(resource_id)
);

CREATE INDEX idx_case_status ON case_request(status);
CREATE INDEX idx_case_triage ON case_request(triage_level);
CREATE INDEX idx_case_time   ON case_request(requested_at);

-- ---------- 5. ASSIGNMENT ------------------------------------
CREATE TABLE assignment (
    assignment_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id        INTEGER NOT NULL REFERENCES case_request(case_id),
    resource_id    INTEGER NOT NULL REFERENCES resource(resource_id),
    assigned_at    TEXT    NOT NULL,
    route_time_min REAL    NOT NULL CHECK (route_time_min >= 0),
    route_path     TEXT,
    policy_used    TEXT    NOT NULL CHECK (policy_used IN ('FCFS','TRIAGE_PRIORITY')),
    met_window     INTEGER NOT NULL DEFAULT 0 CHECK (met_window IN (0,1))
);

CREATE INDEX idx_assign_case   ON assignment(case_id);
CREATE INDEX idx_assign_policy ON assignment(policy_used);

-- ---------- 6. AUDIT_EVENT (append-only; backs undo) --------
CREATE TABLE audit_event (
    event_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type     TEXT    NOT NULL CHECK (event_type IN (
                       'TRIAGED','ASSIGNED','STATUS_CHANGED','UNDONE')),
    entity_type    TEXT    NOT NULL CHECK (entity_type IN ('CASE','RESOURCE','ASSIGNMENT')),
    entity_id      INTEGER NOT NULL,
    previous_state TEXT,
    new_state      TEXT,
    occurred_at    TEXT    NOT NULL
);

CREATE INDEX idx_audit_time ON audit_event(occurred_at);

-- ---------- 7. ALGORITHM_RUN (performance lab) --------------
CREATE TABLE algorithm_run (
    run_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    algorithm_name TEXT    NOT NULL,
    structure_name TEXT,
    input_size     INTEGER NOT NULL CHECK (input_size >= 0),
    input_kind     TEXT    NOT NULL DEFAULT 'RANDOM'
                           CHECK (input_kind IN ('RANDOM','SORTED','REVERSE','DUPLICATES')),
    repetition     INTEGER NOT NULL CHECK (repetition BETWEEN 1 AND 10),
    elapsed_ns     INTEGER NOT NULL CHECK (elapsed_ns >= 0),
    comparisons    INTEGER,
    collisions     INTEGER,
    machine_spec   TEXT
);

CREATE INDEX idx_run_algo ON algorithm_run(algorithm_name, input_size);

-- ---------- Sanity checks to run after loading --------------
-- SELECT COUNT(*) FROM facility;      -- expect >= 50
-- SELECT COUNT(*) FROM road_link;     -- expect >= 100
-- SELECT COUNT(*) FROM case_request;  -- expect >= 300
-- SELECT COUNT(*) FROM resource;      -- expect >= 30
-- SELECT COUNT(*) FROM road_link r LEFT JOIN facility f
--   ON r.from_facility_id = f.facility_id WHERE f.facility_id IS NULL;  -- expect 0
