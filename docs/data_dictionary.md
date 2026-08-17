# Data Dictionary

Field-level definitions for every file in [`data/`](../data/). Sub-team A owns
this file — update it whenever a schema changes.

Loaded by [`DBLoader`](../src/main/java/edu/ug/nexusb/data/DBLoader.java)
(`mvn exec:java -Dexec.args="--init-db"`) into the tables of the same name
(minus `.csv`) in [`data/schema.sql`](../data/schema.sql) — `locations.csv`
loads into `facility`, `roads.csv` into `road_link`, and so on. `facility_id`
in the schema is an internal `AUTOINCREMENT` integer; the CSVs below
reference facilities by their text `code` (e.g. `F001`), which `DBLoader`
resolves to the generated integer ID as it loads.

## locations.csv → `facility`

| Field | Type | Description |
|---|---|---|
| code | string | Text facility reference used by every other CSV (e.g. `F001`) |
| name | string | Facility name |
| facility_type | string | One of `TEACHING_HOSPITAL`, `REGIONAL_HOSPITAL`, `DISTRICT_HOSPITAL`, `POLYCLINIC`, `HEALTH_CENTRE`, `CHPS_COMPOUND`, `AMBULANCE_STATION`, `BLOOD_BANK`, `CENTRAL_LAB`, `OXYGEN_DEPOT`, `MEDICAL_STORE` |
| district | string | Greater Accra district/neighbourhood |
| latitude | float | Decimal degrees |
| longitude | float | Decimal degrees |
| bed_capacity | int | 0 for non-bed facilities (depots, labs) |
| has_emergency | 0/1 | Boolean |
| has_theatre | 0/1 | Boolean |
| opens_24h | 0/1 | Boolean |
| care_level | int | 1–4 |

See [`evidence_note.md`](evidence_note.md) for exactly which rows are
verified real institutions versus synthetic filler.

## roads.csv → `road_link`

| Field | Type | Description |
|---|---|---|
| link_id | int | Unique link identifier |
| from_facility_id | string | References `locations.code` |
| to_facility_id | string | References `locations.code` |
| distance_km | float | Road distance in kilometers |
| base_time_min | float | Base travel time before traffic/condition weighting |
| traffic_weight | float | 1.0–3.0 multiplier |
| road_condition | string | `GOOD` (×1.00), `FAIR` (×1.15), or `POOR` (×1.35) |
| is_one_way | 0/1 | If 0, `DBLoader` inserts both directions |
| route_name | string | Optional; blank if unnamed |

`effective_time_min` — the weight every graph algorithm actually optimises —
is not a column here; it's computed by the `v_weighted_edge` view in
`schema.sql` (`base_time_min × traffic_weight × condition factor`), so
there's exactly one definition of edge cost.

## resources.csv → `resource`

| Field | Type | Description |
|---|---|---|
| resource_id | — | Left blank in the CSV; `DBLoader` doesn't insert it, the schema autoincrements it |
| code | string | Unique resource identifier (e.g. `R001`) |
| resource_type | string | One of `AMBULANCE`, `RESPONSE_TEAM`, `THEATRE_SLOT`, `ICU_BED_BLOCK`, `LAB_COURIER`, `SUPPLY_VEHICLE` |
| home_facility_id | string | References `locations.code` |
| capacity | int | ≥ 1 |
| care_level | int | 1–4 |
| available_from | string | `HH:MM` |
| available_to | string | `HH:MM` |
| shift_minutes | int | > 0 |
| is_available | 0/1 | Boolean |

## request.csv (synthetic) → `case_request`

| Field | Type | Description |
|---|---|---|
| case_id | — | Left blank in the CSV; schema autoincrements it |
| case_ref | string | Unique case reference (e.g. `REQ0001`) |
| origin_facility_id | string | References `locations.code` |
| destination_facility_id | string | References `locations.code`; blank if none |
| case_type | string | e.g. `EMERGENCY_TRANSPORT`, `INTER_FACILITY_REFERRAL`, `LAB_SAMPLE_TRANSPORT` — see `schema.sql` for the full CHECK list |
| triage_level | int | 1 (critical) – 5 (routine) |
| age_band | string | `0-4`, `5-14`, `15-39`, `40-64`, `65+` — **no other demographic fields exist** |
| requested_at | string | Timestamp |
| response_window_min | int | > 0 |
| service_time_min | int | > 0 |
| required_care_level | int | 1–4 |
| status | string | e.g. `PENDING`, `IN_TRANSIT`, `MISSED_WINDOW` — see `schema.sql` for the full CHECK list |
| assigned_resource_id | — | Left blank in the CSV; populated later by the dispatch engine, not the loader |

Schema mirrored in [`data/schema.sql`](../data/schema.sql). Keep both in sync.

## `audit_event` (not CSV-loaded — written by `AuditDao`)

Unlike the tables above, this one isn't populated by `DBLoader` from a CSV.
It's an append-only log written at runtime by
[`AuditDao`](../src/main/java/edu/ug/nexusb/data/AuditDao.java) every time a
decision is recorded or undone, backing the stack-based undo feature in
[`AuditTrail`](../src/main/java/edu/ug/nexusb/data/AuditTrail.java) and
[`AuditLog`](../src/main/java/edu/ug/nexusb/data/AuditLog.java).

| Field | Type | Description |
|---|---|---|
| event_id | int | Autoincrementing primary key |
| event_type | string | One of `TRIAGED`, `ASSIGNED`, `STATUS_CHANGED`, `UNDONE` |
| entity_type | string | One of `CASE`, `RESOURCE`, `ASSIGNMENT` — what kind of thing the decision was about |
| entity_id | int | The ID of that case/resource/assignment |
| previous_state | string | Nullable; what the entity's state was before the decision |
| new_state | string | What the entity's state became after the decision |
| occurred_at | string | ISO-8601 timestamp, set by `AuditEvent.of()` |

Undo doesn't delete rows — it writes a new row with `event_type = UNDONE`
where `previous_state`/`new_state` are swapped from the original event, so
the table stays a complete, append-only history rather than an editable log.

## Index-number-derived parameters

The three algorithm parameters required by the brief (hash table initial
size, dataset generation seed, condition/priority weight factor) are derived
from the team roster's index numbers and documented in
[`parameters.md`](parameters.md), along with the formulas and code locations
where each is consumed.
