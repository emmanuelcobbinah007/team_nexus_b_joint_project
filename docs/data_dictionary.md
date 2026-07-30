# Data Dictionary

Field-level definitions for every file in [`data/`](../data/). Sub-team A owns
this file — update it whenever a schema changes.

## facilities.csv

| Field | Type | Description |
|---|---|---|
| facility_id | string | Unique facility identifier |
| name | string | Facility name |
| type | string | e.g. `hospital`, `clinic`, `depot` |
| latitude | float | Decimal degrees |
| longitude | float | Decimal degrees |
| capacity | int | Bed / handling capacity |

## road_links.csv

| Field | Type | Description |
|---|---|---|
| link_id | string | Unique link identifier |
| from_facility_id | string | References `facilities.facility_id` |
| to_facility_id | string | References `facilities.facility_id` |
| distance_km | float | Road distance in kilometers |
| avg_travel_time_min | float | Average travel time in minutes |

## cases.csv (synthetic)

| Field | Type | Description |
|---|---|---|
| case_id | string | Unique case identifier |
| patient_id | string | Synthetic patient identifier |
| severity | int | Triage severity, 1 (critical) – 5 (minor) |
| facility_id | string | Originating/assigned facility |
| timestamp | datetime | ISO 8601 |
| status | string | e.g. `pending`, `dispatched`, `resolved` |

## resources.csv

| Field | Type | Description |
|---|---|---|
| resource_id | string | Unique resource identifier |
| type | string | e.g. `ambulance`, `bed`, `staff` |
| facility_id | string | References `facilities.facility_id` |
| quantity | int | Units available |
| status | string | e.g. `available`, `in_use`, `offline` |

Schema mirrored in [`data/schema.sql`](../data/schema.sql). Keep both in sync.
