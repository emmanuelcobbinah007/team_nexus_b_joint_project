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

## Index-number-derived parameters

The three algorithm parameters required by the brief (hash table initial
size, dataset generation seed, condition/priority weight factor) are derived
from the team roster's index numbers and documented in
[`parameters.md`](parameters.md), along with the formulas and code locations
where each is consumed.
