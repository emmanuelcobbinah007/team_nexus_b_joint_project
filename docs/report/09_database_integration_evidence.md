# 9. Database Integration Evidence

## Schema and load path

`data/schema.sql` defines seven tables in SQLite (`facility`, `road_link`
plus its `v_weighted_edge` view, `resource`, `case_request`, `assignment`,
`audit_event`, `algorithm_run` — see
[03_dataset_description.md](03_dataset_description.md) for what each one
holds). `DBLoader` creates the schema and loads all four source CSVs in
foreign-key order, resolving each CSV's text `code` (e.g. `F001`) to the
generated integer primary key as it inserts.

## A real, freshly-loaded database, not a claimed one

Rather than describe the load path only in prose, `nexus.db` was deleted
and rebuilt from scratch for this report (`App --init-db`, which calls
`DBLoader.run()`) and the row counts were read back directly:

```
Loaded 117 rows into facility (from data/locations.csv)
Loaded 217 rows into road_link (skipped 0) (from data/roads.csv)
Loaded 32 rows into resource (skipped 0) (from data/resources.csv)
Loaded 306 rows into case_request (skipped 0) (from data/request.csv)

=== Row count verification ===
facility        expected=117   actual_in_db=117   [OK]
road_link       expected=217   actual_in_db=217   [OK]
resource        expected=32    actual_in_db=32    [OK]
case_request    expected=306   actual_in_db=306   [OK]
```

`DBLoader` runs its own `expected` vs. `actual_in_db` check after every
load rather than trusting a silent insert loop — foreign-key resolution
failures or a malformed CSV row show up as a count mismatch immediately,
not as a quieter downstream bug.

## Read/write evidence per table

| Table | Written by | Read by | Verified this run |
|---|---|---|---|
| `facility` | `DBLoader` (CSV load) | `GraphBuilder` (assembles the routing graph), `IndexingEngine` | 117 rows loaded, exact match |
| `road_link` | `DBLoader` (CSV load) | `GraphBuilder`, via `v_weighted_edge` for the effective travel-time weight every routing algorithm uses | 217 rows loaded, exact match |
| `resource` | `DBLoader` (CSV load) | `app`/`web` dispatch demos | 32 rows loaded, exact match |
| `case_request` | `DBLoader` (CSV load) | `IndexingEngine`, `TriageDispatchEngine`, `ExaminerConsole`/`ApiServer` demos | 306 rows loaded, exact match |
| `algorithm_run` | `DatabaseBenchmark` (T042), one row per timed repetition | Section 8's performance analysis (every CSV under `results/csv/` is itself generated from data that also lands here) | 70 rows written by a single `HeapExperiments` run (7 sizes × 5 repetitions × 2 series = 70), grouped as `Insert: 35`, `Extract: 35` |
| `audit_event` | `AuditDao.insert` (`data/`), called from `TriageDispatchEngine`'s dispatch-commit path | `AuditDao.findLatestForEntity` (undo/audit lookups) | Exercised directly by `AuditDaoTest` (`insert_persistsRowAndFillsGeneratedEventId`, `findLatestForEntity_returnsMostRecentRowForThatEntity`, plus a boundary case with no rows and an invalid-input case asserting the `event_type` `CHECK` constraint rejects an unknown value) rather than by this particular load — 0 rows in the fresh DB above, since no dispatch was committed against it this run |
| `assignment` | *(not yet written by any code path)* | *(not yet read by any code path)* | 0 rows — confirmed via `grep -rln "INSERT INTO assignment" src/main/java` returning no matches |

## `algorithm_run` — proof it's not just a schema, it's actually used

Beyond the row count, `algorithm_run` is what every chart under
`results/graphs/` is ultimately built from — `DatabaseBenchmark.measure()`
writes one row per repetition (`algorithm_name`, `input_size`,
`repetition`, `elapsed_ns`) as it runs, and every `bench/*Experiments`
class in Section 8 reads that same instrumentation back out (via the CSVs
it also writes) into a chart. This is the concrete difference between
"the database is defined" and "the database is load-bearing": deleting
`nexus.db` and rerunning any experiment regenerates real rows, not fixed
sample data checked into the repo.

## The one known gap, named rather than hidden

`assignment` — which resource was matched to which case, under which
dispatch policy — is defined in the schema and described in
[03_dataset_description.md](03_dataset_description.md), but no code path
currently inserts into it: `GreedyDispatch`/`TriageDispatchEngine` compute
a dispatch order and commit an `audit_event`, but stop short of
persisting the resulting resource-to-case pairing as its own row. This is
recorded here explicitly rather than left to be discovered during oral
defense — the schema anticipated a capability (post-hoc reporting on
which resource served which case) that the current console/web demos
don't yet exercise end-to-end.
