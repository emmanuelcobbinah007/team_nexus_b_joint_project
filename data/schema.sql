-- Mirrors docs/data_dictionary.md. Keep both in sync.

CREATE TABLE facilities (
    facility_id     TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    type            TEXT NOT NULL,
    latitude        REAL NOT NULL,
    longitude       REAL NOT NULL,
    capacity        INTEGER NOT NULL
);

CREATE TABLE road_links (
    link_id             TEXT PRIMARY KEY,
    from_facility_id    TEXT NOT NULL REFERENCES facilities(facility_id),
    to_facility_id      TEXT NOT NULL REFERENCES facilities(facility_id),
    distance_km         REAL NOT NULL,
    avg_travel_time_min REAL NOT NULL
);

CREATE TABLE cases (
    case_id      TEXT PRIMARY KEY,
    patient_id   TEXT NOT NULL,
    severity     INTEGER NOT NULL CHECK (severity BETWEEN 1 AND 5),
    facility_id  TEXT NOT NULL REFERENCES facilities(facility_id),
    timestamp    TEXT NOT NULL,
    status       TEXT NOT NULL
);

CREATE TABLE resources (
    resource_id  TEXT PRIMARY KEY,
    type         TEXT NOT NULL,
    facility_id  TEXT NOT NULL REFERENCES facilities(facility_id),
    quantity     INTEGER NOT NULL,
    status       TEXT NOT NULL
);
