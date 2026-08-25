# 2. Problem Statement, Assumptions, Input-Output Definitions & System Boundaries

## The problem

An organisation operating across Greater Accra's health network receives
service requests (cases), stores them in a database, prioritises urgent
jobs, assigns resources, finds routes between locations, monitors
connectivity between zones, supports search and reporting, and evaluates
its own algorithm performance. Concretely, the system answers:

i. Which service request should be handled next under FIFO, urgency, and
   priority-based rules?
ii. What is the fastest route from one facility to another under
    weighted-road conditions?
iii. Which facilities are reachable from the current dispatch point (given
     some roads may be closed)?
iv. Which subset of requests or resources can be selected under a
    budget/capacity constraint?
v. How do alternative data structures and algorithms perform as the
   dataset grows?
vi. How can the system persist records and reload them for later analysis?

## Why a hospital network, not a hospital building

A single hospital does not have fifty locations connected by a hundred
roads — modelling corridors as roads would look invented. A district
network of facilities linked by real Accra roads gives a genuine map,
genuine travel times, and a genuine reason for every algorithm the brief
requires — including the strongest possible demonstration of a priority
queue, since triage is exactly what one was invented for.

## Core structural requirement

Every data structure and algorithm in
`src/main/java/edu/ug/nexusb` (outside `data/` and `bench/`, exempted for
file parsing, database plumbing, and benchmark scaffolding) is implemented
from scratch — no `java.util` collections. Enforced by CI on every push,
not just a style guideline.

## Assumptions

- **Facility and road data is real**, compiled by team members from their
  own knowledge of Greater Accra; **case and resource data is fully
  synthetic**, generated from an index-number-derived seed
  (`docs/parameters.md`). See
  [03_dataset_description.md](03_dataset_description.md).
- **Road weight is effective travel time**, not raw distance:
  `base_time_min × traffic_weight × condition_factor`, where
  `condition_factor` comes from road condition and the team's own
  `CONDITION_WEIGHT_FACTOR = 1.08`. Every routing algorithm (Dijkstra,
  Prim, Kruskal) operates on this derived weight.
- **A single dispatch resource per decision.** The triage/dispatch
  problems modelled here assume one ambulance/unit is being scheduled at a
  time (sequential service), not fleet-wide simultaneous dispatch — this
  is what makes "dispatch order" a meaningful thing to optimize at all
  (see [06_algorithm_implementation.md](06_algorithm_implementation.md)'s
  greedy-dispatch discussion).
- **Time is discretized for the triage/FCFS comparison** (`compareDetailed`
  models each case as costing a fixed 2 time-units to service) — a
  simplification that keeps the comparison tractable and reproducible,
  not a claim that all cases take equal real-world time.

## Input-output definitions (major operations)

| Operation | Input | Output | Precondition |
|---|---|---|---|
| `Dijkstra.shortestPaths` | a `MyGraph`, a source vertex ID | a `PathResult` (distance/predecessor/path to every reachable vertex) | source vertex exists in the graph |
| `Reachability.bfsReachable` | a `MyGraph`, source ID, set of closed edge keys | set of reachable vertex IDs | source vertex exists |
| `Dfs.traverse` | a `MyGraph` | visit order + cycle info (if any) | none (handles disconnected graphs) |
| `Kruskal.run` / `Prim.minimumSpanningTree` | a graph (edge array / `MyGraph`) | MST edges + total weight | graph must be connected for a true spanning tree (`Prim` throws otherwise) |
| `GreedyDispatch.runGreedyDispatch` / `runOptimalDispatch` | station ID, case requests, road network | dispatch order (case references) | station and every case's facility exist in the graph |
| `KnapsackDP.solve` | weights, values, capacity (all non-negative) | max value + selected item indices | weights/values same length, capacity ≥ 0 |
| `TriageComparison.compare` / `compareDetailed` | a list of cases (ID, arrival time, severity) | average wait (or full per-case detail) under FCFS and priority order | case list non-null and non-empty |
| Sorters (`Sorter.sort`) | an array, a comparator | the array sorted in place | none — every sorter handles `null`/empty input as a no-op |
| Searchers (`Searcher.linearSearch` / `binarySearch`) | an array, a target | index of target, or `-1` | binary search additionally requires the array already sorted by the same ordering — **violating this precondition is a silent correctness bug, not an exception**; see the counterexample in [06_algorithm_implementation.md](06_algorithm_implementation.md) |

## System boundaries — out of scope

- Real patient data of any kind — the schema has no column that could hold
  it (see [03_dataset_description.md](03_dataset_description.md)).
- A production-grade persistence layer — `nexus.db` is a local, gitignored
  SQLite file regenerated from `data/*.csv` on demand, not a managed
  database.
- A UI beyond the console menu (`ExaminerConsole`) and the live web console
  (`ApiServer`) — this is a DSA demonstration project, not a deployable
  product; per the brief, "this is not a UI-design project."

## Ownership boundaries

One package per sub-team (README's repository layout table), so two people
never edit the same file and never hit a merge conflict on core logic.
`core/` is the only shared package, frozen after interface freeze (30 Jul)
and changeable only with the Technical Lead's sign-off.
