# 3. Dataset Description, Data Dictionary & Database Schema

Full detail: [`data_dictionary.md`](../data_dictionary.md) (field-level
schema for every CSV) and [`evidence_note.md`](../evidence_note.md) (what
the dataset approximates and why). Summarized here.

## Database schema

Seven tables in `data/schema.sql` (SQLite):

| Table | Purpose |
|---|---|
| `facility` | Every health facility — the graph's vertices |
| `road_link` | Weighted edges between facilities; `v_weighted_edge` (a view over it) computes the effective travel-time weight every routing algorithm uses |
| `case_request` | Incoming cases — queued, prioritised, searched, and sorted |
| `resource` | Ambulances, response teams, beds — assignable units |
| `assignment` | Which resource was assigned to which case, the route taken, the dispatch policy used (`FCFS`/`TRIAGE_PRIORITY`), and whether its response window was met |
| `algorithm_run` | Empirical runtime measurements written by `DatabaseBenchmark` (T042) — `algorithm_name`, `input_size`, `repetition`, `elapsed_ns` |
| `audit_event` | Stack-based undo/audit trail (T026, T057) |

Built and seeded by `DBLoader` (`mvn exec:java -Dexec.args="--init-db"`),
which reads `data/schema.sql` then loads all four CSVs in foreign-key
order, resolving each CSV's text `code` references (e.g. `F001`) to the
generated integer primary keys as it goes.

## What's real, what's synthetic

- **Facility and road data is real** — compiled by team members from their
  own knowledge of Greater Accra: 117 facilities (`data/locations.csv`)
  across teaching/regional/district hospitals, polyclinics, health centres,
  CHPS compounds, ambulance stations, blood banks, labs, oxygen depots, and
  medical stores; 217 road links (`data/roads.csv`) with real distance,
  base travel time, traffic, and condition data.
- **Case and resource data is fully synthetic** — 306 generated cases
  (`data/request.csv`), 32 generated resources (`data/resources.csv`). No
  patient names, contact details, national IDs, diagnoses, or clinical
  notes anywhere in the repository or the schema: a case carries only a
  generated reference, an age band, and a triage level.

## Reproducibility

The dataset generation seed and two other parameters (hash table initial
size, condition/priority weight factor) are derived from the 15 team
members' real index numbers, not chosen arbitrarily — see
[`parameters.md`](../parameters.md) for the exact formulas. Anyone can
recompute all three from the roster and get identical values, and the
"random" dataset is therefore reproducible and traceable to this specific
team rather than to an arbitrary run.

## Key assumption in the effective-edge-weight model

`road_link`'s `v_weighted_edge` view (`data/schema.sql`) computes the
weight every routing algorithm actually uses:
`base_time_min * traffic_weight * condition_factor`, where
`condition_factor` comes from `road_condition` (GOOD/FAIR/POOR) and the
team's own `CONDITION_WEIGHT_FACTOR = 1.08`. This means Dijkstra/Prim/
Kruskal are not routing on raw distance — they're routing on estimated
travel time under current conditions, which is the more operationally
meaningful metric for an ambulance dispatch system.
