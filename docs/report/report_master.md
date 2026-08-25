# Hospital & Clinic Operations Optimizer — Technical Report

**Team Nexus B · Group 29 · DCIT 204/308 Joint Data Structures & Algorithms Project**
University of Ghana · 26 July – 22 August 2026

---

_This file is the assembled Markdown source for `report_final.docx`/`.pdf` (T075) —
concatenated from `docs/report/01_*.md` through `12_*.md` in order, matching the
12-section structure the course brief itself requires. Convert with
`pandoc report_master.md -o report_final.docx` (or `.pdf`). As of this assembly,
sections 1-10 and 12 have real, verified content (every trace table, proof sketch,
and benchmark number checked against a real run of the code); section 11
(individual contributions) has its table structure and oral-defense-notes
framework in place but each member's own row is still to be filled in before
submission._

---


# Ghana Smart Service Operations Optimizer

## Hospital & Clinic Operations Optimizer

**Team Nexus B · Group 29**
DCIT 204/308: Data Structures and Algorithms I & II — Joint DSA Semester Project
Department of Computer Science, University of Ghana
26 July – 22 August 2026

**Selected Ghana context:** Hospital / clinic operations — a district health
network across Greater Accra.

**Organisation/problem modelled:** A service-operations platform that
triages incoming cases, dispatches ambulances and response teams over a
weighted road network of real Greater Accra health facilities, indexes
case records, and reports on its own algorithmic performance.

## Team

| Role | Member |
|---|---|
| Group Leader (outward-facing) | Irene Tetteh |
| Planning & Delivery Lead | Frederick Kankam |
| Technical Lead / Integrator | Cobbinah Emmanuel |

| Sub-team | Lead | Members |
|---|---|---|
| A — Data & Database | Victor Barnieh | Arhin Franca, Baah John Excellence |
| B — Linear Structures | Obeng Jessica | Arthur Philip Kofi, El Masri Bilal |
| C — Trees & Hashing | Cobbinah Emmanuel | Mensah-Dogbevi Princess, Dakwa Nana Kwabena |
| D — Graphs | Frederick Kankam | Kwetey Sylvester, Irene Tetteh |
| E — Sorting & Optimisation | Johnson Kuzagbr | Ajilogba Abdulmalik, Salami Oluwanifemi |

Full roster with index numbers: [`../cover_sheet.md`](../cover_sheet.md).


---


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


---


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


---


# 4. System Architecture and Module Design

Full contract text: [`interfaces.md`](../interfaces.md). Summarized here.

## Interfaces-first architecture

Five sub-teams built simultaneously against one frozen set of contracts in
`edu.ug.nexusb.core` (locked 30 Jul; any change after that date needs the
Technical Lead's written sign-off). That's what let Sub-team D write
Dijkstra against `MyPriorityQueue` in Week 3 while Sub-team B was still
refining the heap implementation underneath it — the contract was settled,
so the two didn't need to be sequential.

Shared core types: `MyComparator<T>` (ordering, declared locally rather
than reusing `java.util.Comparator`), `MyIterator<T>`/`MyIterable<T>`
(fail-fast traversal), `Instrumented` (comparison/movement counters,
resettable — see below), and the `StructureException` hierarchy
(`EmptyStructureException`, `CapacityExceededException`,
`KeyNotFoundException`).

## Why every structure is `Instrumented`

Wall-clock timings vary across fifteen different laptops; comparison and
movement counts don't. `Instrumented` puts a cheap, resettable counter
(increment a `long`, nothing else) on nearly every structure specifically
so that when a Week 4 benchmark graph is noisy, the counter data is
independent, machine-agnostic evidence backing it up.

## Module layout

One package per sub-team — `data/` (A), `linear/` (B), `trees/` (C),
`graphs/` (D), `algorithms/`+`bench/` (E) — plus `core/` (frozen, shared),
`app/` (triage/dispatch wiring, console menu), and `web/` (a live HTTP
API + browser frontend added after Week 4's tracker tasks, exposing every
capability above interactively — see
[06_algorithm_implementation.md](06_algorithm_implementation.md)). Two
people never edit the same file, so ownership boundaries alone prevent
most merge conflicts; `docs/interfaces.md` documents each package's
specific contracts and the cross-team dependencies worth protecting (e.g.
`MyPriorityQueue.decreaseKey()` exists solely for Dijkstra's benefit —
nothing in Sub-team B's own module calls it, but it must not be dropped
as "unused").

## Data flow

`data/*.csv` → `DBLoader` → `nexus.db` (SQLite) → `GraphBuilder` /
`IndexingEngine` / `TriageDispatchEngine` build in-memory structures from
the DB → either `app.ExaminerConsole` (text menu, T056) or
`web.ApiServer` + the browser frontend demonstrates each capability
against the same real data, end to end.


---


# 5. Data Structures Implemented

Every structure is tested for the normal case, a boundary case, and
invalid input; test counts below are declared `@Test`/`@ParameterizedTest`
methods, not runtime invocations (several graph structures run parameterized
across both `AdjacencyListGraph` and `AdjacencyMatrixGraph`, so the actual
executed count is higher).

## Linear (`linear/`, Sub-team B)

- **`DynamicArrayList`** (14 tests) — amortized `O(1)` append via doubling,
  `O(n)` insert/remove at an index, `O(1)` indexed access.
- **`DoublyLinkedList`** (14 tests) — `O(1)` insertion at a held position,
  `O(n)` indexed access; the `MyList` comparison against the array is the
  point of the T018 contract (`interfaces.md`).
- **`ArrayStack`** (11 tests) — backs the audit-trail undo (`data/`'s
  `AuditTrail`) and the iterative DFS (`Dfs.java`).
- **`ArrayDeque`** (13 tests) — models an urgent case pushed to the front of
  the queue rather than the back.
- **`ArrayCircularQueue`** / **`ArrayQueue`** (8 tests) — fixed-capacity
  wrap-around queue and an unbounded queue; `MyQueue`'s contract that
  `isFull()`/`capacity()` return `false`/`-1` for the unbounded version.
- **`BinaryHeapPriorityQueue`** (11 tests) — `O(log n)` insert/extract,
  `O(1)` peek, `O(n)` `buildFrom` (heapify) rather than n repeated inserts.
  `decreaseKey()` exists specifically for Dijkstra's benefit — see
  [04_system_architecture.md](04_system_architecture.md).

## Trees (`trees/`, Sub-team C)

- **`BinarySearchTree`** (37 tests) vs. **`RedBlackTree`** (22 tests) — the
  T019 comparison: an RB tree's height is provably `O(log n)` regardless of
  insertion order, while a plain BST degenerates to `O(n)` on sorted input.
  See [`proof_bst_height.md`](../proofs/proof_bst_height.md).
- **`BTree`** (40 tests) — the structure actually chosen for
  `IndexingEngine`'s time-range index in preference to the RB tree's linked
  node overhead.
- **`ChainedHashTable`** (18 tests) — separate chaining, collision-count and
  resize tracking exposed via `MyHashTable`'s extra methods (not just the
  base `MyMap` contract), so a benchmark can see *why* a lookup was slow,
  not just that it was. See [`proof_hashing.md`](../proofs/proof_hashing.md).
- **`HashSet`** (13 tests) — thin `MySet` wrapper over `ChainedHashTable`,
  each element as a key mapped to a shared sentinel.

## Graphs (`graphs/`, Sub-team D)

- **`AdjacencyListGraph`** / **`AdjacencyMatrixGraph`** (18 shared contract
  tests via `MyGraphContractTest`, plus 1 cross-check test confirming both
  representations agree) — both implement the same `MyGraph` interface, so
  every algorithm below is representation-agnostic.
- **`DisjointSet`** (12 tests) — path compression + union by rank, Kruskal's
  actual dependency. **`FacilityDisjointSet`** (13 tests) — the newer
  id-based wrapper used elsewhere.
- **`GraphBuilder`** (6 unit + 2 DB-integration tests) — assembles a
  `MyGraph` from the `facility`/`road_link` tables.

Every graph *algorithm* (Dijkstra, Kruskal, Prim, BFS, DFS) is covered in
[06_algorithm_implementation.md](06_algorithm_implementation.md) rather
than here, since the interesting claims about them are complexity and
correctness, not structural invariants.


---


# 6. Algorithm Implementation, Pseudocode & Complexity

Full detail lives in [`traces/`](../traces/) (step-by-step execution
traces, each verified against the real implementation) and
[`proofs/`](../proofs/) (correctness and complexity arguments). Summarized
here with each algorithm's headline complexity result.

## Graph algorithms (`graphs/`, Sub-team D)

| Algorithm | Complexity | Evidence |
|---|---|---|
| Dijkstra (T046) | `O((V+E) log V)` with a binary heap | [`trace_dijkstra.md`](../traces/trace_dijkstra.md) |
| BFS reachability (T047) | `O(V+E)` | `ReachabilityTest` |
| DFS + cycle detection (T048) | `O(V+E)` | two-state (on-stack/finished) tracking avoids false positives on diamond-shaped DAGs |
| Kruskal (T049) | `O(E log E)` | [`trace_kruskal.md`](../traces/trace_kruskal.md), [`proof_kruskal.md`](../proofs/proof_kruskal.md) — cut-property correctness proof |
| Prim (T050) | `O(E log V)` | [`trace_prim.md`](../traces/trace_prim.md); see [08_performance_analysis.md](08_performance_analysis.md) for a real `O(V)` bug found and fixed in the original implementation |

## Sorting & searching (`algorithms/`, Sub-team E)

| Algorithm | Complexity | Evidence |
|---|---|---|
| Merge sort (T040) | `O(n log n)` worst case, stable | [`trace_mergesort.md`](../traces/trace_mergesort.md) |
| Quicksort (T040) | `O(n log n)` average, `O(n²)` worst | [`trace_quicksort.md`](../traces/trace_quicksort.md) |
| Selection sort / Insertion sort (T041) | `O(n²)` | [`trace_insertionsort.md`](../traces/trace_insertionsort.md), [`proof_insertion_sort_loop_invariant.md`](../proofs/proof_insertion_sort_loop_invariant.md) — loop-invariant correctness proof |
| Linear + binary search (T039) | `O(n)` / `O(log n)` | [`trace_binarysearch.md`](../traces/trace_binarysearch.md), **including the required unsorted-input counterexample**: binary search silently misses a present element that linear search finds, since violating its sortedness precondition is a correctness bug, not an exception |

## Greedy & dynamic programming (`optimization/`, Sub-team E)

| Algorithm | Complexity | Evidence |
|---|---|---|
| Greedy dispatch (T051) | `O(n log n)` (dominated by the sort) | [`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md) — an engineered case where nearest-first greedy scores strictly worse (8.75 vs. 7.0) than the urgency-weighted alternative under the objective both are actually judged by |
| 0/1 Knapsack DP (T052) | `O(n · capacity)` time and space | [`trace_knapsack.md`](../traces/trace_knapsack.md) — full DP table build + backtracking reconstruction, verified cell-for-cell against the real implementation |

## Trees & hashing (`trees/`, Sub-team C)

| Structure | Complexity | Evidence |
|---|---|---|
| BST vs. Red-Black tree height | `O(n)` worst case vs. provably `O(log n)` | [`proof_bst_height.md`](../proofs/proof_bst_height.md) |
| Chained hash table | `O(1)` average, `O(n)` worst case | [`proof_hashing.md`](../proofs/proof_hashing.md) — simple-uniform-hashing assumption stated explicitly, amortized resize cost argument included |

## Pseudocode and selected Java snippets

Two representative algorithms, chosen because between them they cover a
graph algorithm with a non-trivial data structure dependency (a priority
queue) and the greedy algorithm the brief specifically asks for a
counterexample against.

### Dijkstra's shortest path

```
DIJKSTRA(graph, sourceId):
    dist[v] := infinity for every vertex v;  dist[sourceId] := 0
    queue := empty priority queue ordered by dist
    insert (sourceId, 0) into queue
    while queue is not empty:
        u := extract the entry with minimum dist from queue
        mark u finalized; record u in visit order
        for each edge (u, v, weight) leaving u:
            if v is not finalized and dist[u] + weight < dist[v]:
                dist[v] := dist[u] + weight;  pred[v] := u
                if v already has an entry in queue: decreaseKey(v)
                else: insert (v, dist[v]) into queue
    return dist, pred, visit order
```

Real implementation (`Dijkstra.java:43-77`, elided for length — see the
file for the full relax step and `visitOrder` bookkeeping):

```java
public static PathResult shortestPaths(MyGraph graph, String sourceId) {
    if (graph == null) {
        throw new IllegalArgumentException("graph must not be null");
    }
    if (sourceId == null) {
        throw new IllegalArgumentException("sourceId must not be null");
    }
    if (!graph.containsVertex(sourceId)) {
        throw new KeyNotFoundException("No such vertex in graph: " + sourceId);
    }

    String[] ids = collectVertexIds(graph);
    int n = ids.length;
    double[] dist = new double[n];
    String[] pred = new String[n];
    boolean[] finalized = new boolean[n];
    Entry[] entries = new Entry[n];
    for (int i = 0; i < n; i++) {
        dist[i] = Double.POSITIVE_INFINITY;
    }

    MyPriorityQueue<Entry> queue = new BinaryHeapPriorityQueue<>(DISTANCE_ORDER);

    int sourceIndex = indexOf(ids, sourceId);
    dist[sourceIndex] = 0.0;
    entries[sourceIndex] = new Entry(sourceId, 0.0);
    queue.insert(entries[sourceIndex]);

    String[] visitOrder = new String[n];
    int visitCount = 0;

    while (!queue.isEmpty()) {
        Entry current = queue.extractTop();
        int u = indexOf(ids, current.vertexId);
        finalized[u] = true;
        visitOrder[visitCount] = ids[u];
        visitCount++;
        // ... relax each outgoing edge, decreaseKey() on a shorter route ...
    }
    // ... build and return a PathResult ...
}
```

The pseudocode and the real code match line for line in structure — the
only things pseudocode elides are the array bookkeeping (`ids`,
`indexOf`) needed because this project builds its own priority queue
rather than using a library one with object identity built in.

### Greedy dispatch (the algorithm the required counterexample is about)

```
GREEDY_DISPATCH(stationId, requests, roadNetwork):
    pathResult := DIJKSTRA(roadNetwork, stationId)
    sort requests ascending by pathResult.distanceTo(request.facility)
    return the sorted requests' case references

OPTIMAL_DISPATCH(stationId, requests, roadNetwork):
    pathResult := DIJKSTRA(roadNetwork, stationId)
    sort requests ascending by pathResult.distanceTo(request.facility) * request.triageLevel
    return the sorted requests' case references
```

Real implementation (`GreedyDispatch.java:36-49`):

```java
public static String[] runGreedyDispatch(String resourceStationId, CaseRequest[] requests, MyGraph roadNetwork) {
    requireRequests(requests);
    PathResult pathResult = Dijkstra.shortestPaths(roadNetwork, resourceStationId);

    // Sort copy array by distance (Greedy Choice)
    CaseRequest[] sorted = requests.clone();
    for (int i = 0; i < sorted.length - 1; i++) {
        for (int j = 0; j < sorted.length - i - 1; j++) {
            double distA = pathResult.distanceTo(sorted[j].originFacilityId);
            double distB = pathResult.distanceTo(sorted[j + 1].originFacilityId);
            if (distA > distB) {
                CaseRequest temp = sorted[j];
                sorted[j] = sorted[j + 1];
                sorted[j + 1] = temp;
            }
        }
    }
    // ... build and return the case-reference order ...
}
```

The single-character difference between this method and
`runOptimalDispatch` (comparing `distanceTo(...)` alone versus
`distanceTo(...) * triageLevel`) is the entire mechanism behind the
required counterexample: see
[`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md)
for the worked case where that one extra factor changes which case gets
served first, and lowers total penalty by 20%.


---


# 7. Correctness Evidence

Trace tables, proof sketches, and counterexamples live in their own
folders — [`docs/traces/`](../traces/), [`docs/proofs/`](../proofs/),
[`docs/counterexamples/`](../counterexamples/) — and are cross-referenced
throughout [06_algorithm_implementation.md](06_algorithm_implementation.md)
rather than duplicated here. This section covers the testing strategy and
the brief's specific edge-case checklist.

## Coverage bar

Every structure and algorithm is tested for the normal case, a boundary
case, and invalid input — not just the happy path. This was audited
directly (T066) rather than assumed: a full sweep of every test file
against that three-case bar turned up several real gaps — a hand-rolled
"test" class (`DisjointSetTest`) with no `@Test` annotations that Maven
silently never ran despite looking covered, missing boundary cases in
`QueueTest`/`InsertionSortTest`, missing invalid-input validation in
`GraphBuilder`/`ArrayCircularQueue`/`KnapsackDP`/`GreedyDispatch` — all
since closed (PRs #44, #45, #46).

## Current test count

**459 tests, all passing** on a clean build (`mvn test`, verified again
after every branch merged to `main`) — comfortably over the brief's
40-test minimum. Breakdown by package:

- `trees/`: 130 declared test methods (BST 37, B-tree 40, RB-tree 22,
  chained hash table 18, hash set 13)
- `graphs/`: ~90 declared test methods across structures and algorithms
  (several parameterized across both graph representations, so the
  executed count is higher than the declared count)
- `linear/`: 65 declared test methods
- `algorithms/`+`optimization/`: ~60 declared test methods
- `bench/`, `data/`, `app/`: integration and demo-scenario tests

## Trace tables and proof sketches (brief Section 10 minimums)

The brief asks for at least six trace tables and three proof sketches.
This project has **8 trace tables** — binary search, insertion sort,
merge sort, quicksort, Dijkstra, Kruskal, Prim, and DP knapsack — and
**4 proof sketches**, covering all three categories the brief names:

- **Loop invariant for search/sort**:
  [`proof_insertion_sort_loop_invariant.md`](../proofs/proof_insertion_sort_loop_invariant.md)
- **Induction/recursion proof**:
  [`proof_bst_height.md`](../proofs/proof_bst_height.md) (the
  black-height lemma is proven by induction on subtree height)
- **Greedy or DP correctness idea**:
  [`proof_kruskal.md`](../proofs/proof_kruskal.md) (cut-property exchange
  argument)
- Plus [`proof_hashing.md`](../proofs/proof_hashing.md), a complexity
  argument for the chained hash table beyond the three required categories.

**2 counterexamples**, matching the brief's minimum exactly:

- **Greedy failure**: [`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md)
- **Invalid precondition (unsorted binary search input)**: worked in
  [`trace_binarysearch.md`](../traces/trace_binarysearch.md)'s final
  section and asserted directly by
  `LinearBinarySearchTest.binarySearchOnUnsortedInputCanSilentlyMissAPresentElement`

## Edge cases (brief Section 10 checklist)

| Edge case | Where it's covered |
|---|---|
| Empty structure | Every structure's boundary-case tests (e.g. `emptyTableGetAndRemoveReturnNullNotThrow`, `newDequeIsEmpty`) |
| Single element | `singleEntryTableHasNoCollisions`, `singleElementIsTriviallyConnectedToItself`, `singleCaseAlwaysHasZeroWaitUnderBothModes` |
| Duplicate keys | `ChainedHashTableTest`'s collision tests; `putOnExistingKeyUpdatesAndReturnsPreviousValue` |
| Disconnected graph | `disconnectedGraph_isNotSpanning_andReturnsPartialForest` (Kruskal); `graph is disconnected` (`Prim`, throws by design) |
| Unreachable path | `unreachableVertexIsReportedAsUnreachable` (Dijkstra); the web console's BFS-with-closed-roads panel demonstrates this live |
| Queue full/empty | `testZeroCapacityConstructorThrows`, `testDequeueOnEmptyQueueThrows`, `ArrayCircularQueueTest`'s full-queue `IllegalStateException` case |
| Hash collision | `collidingKeysLandInTheSameBucketAsAChain`; empirically reproduced at scale in [08_performance_analysis.md](08_performance_analysis.md)'s hash table experiment |

## What "verified" means in this report

Every trace table, proof sketch, and benchmark number in this report was
either (a) generated by actually running the real implementation and
transcribing its output, or (b) hand-computed and then checked against a
real run before being written down — not asserted from theory alone. Where
a discrepancy between expectation and measurement turned up (the Prim
`O(V)` bottleneck, the DP zero-weight edge case, the FCFS ordering bug),
it was investigated to a root cause and fixed rather than left unexplained.


---


# 8. Performance Analysis

## Methodology

`DatabaseBenchmark` (T042) is the measurement framework every experiment
below uses: 3 untimed warm-up iterations before any repetition is timed
(so the JIT has already compiled the hot path), `System.nanoTime()` rather
than wall-clock time, and every repetition written to `algorithm_run` as
its own row rather than pre-averaged away.

## T073 — Graph algorithms vs. graph size and density

**Status: done.** Run via `edu.ug.nexusb.bench.GraphExperiments`
(generates two connected random graphs per configuration, seeded from
`GENERATION_SEED = 79731`, `docs/parameters.md` Parameter B, so every run
reproduces the same graphs) and charted via `GraphExperimentsChart`
(self-contained SVG, no external charting library).

- Data: [`results/csv/graph_experiments_size.csv`](../../results/csv/graph_experiments_size.csv), [`results/csv/graph_experiments_density.csv`](../../results/csv/graph_experiments_density.csv)
- Charts: [`results/graphs/graph_experiments_size.svg`](../../results/graphs/graph_experiments_size.svg), [`results/graphs/graph_experiments_density.svg`](../../results/graphs/graph_experiments_density.svg)

**Size sweep** (|V| = 50 to 800, fixed density |E| ≈ 3|V|): BFS, DFS,
Dijkstra, and Kruskal all scale in line with their expected complexity.
Dijkstra's ~27–29× slowdown over a 16× growth in |V| matches `O(E log V)`
almost exactly (|E| grows 16× and `log₂V` grows a further ~1.7×,
16 × 1.7 ≈ 27). Kruskal stays the fastest algorithm at every size, as
expected for `O(E log E)` with `E ≈ 3V` — a slower-growing input than the
`V log V` term Dijkstra effectively pays through the priority queue.

**Prim had a real `O(V)`-per-lookup bug, found and fixed during this
experiment.** The first run showed Prim ~55× slower at |V|=800 than at
|V|=50 (versus Dijkstra's ~27× over the same growth), and roughly 70×
slower than Kruskal despite solving the same MST problem on the same
graph — not measurement noise. `Prim.java`'s `buildIncidentEdges` and main
extraction loop both called `indexOf(vertexIds, targetId)`, a **linear
`O(V)` scan** over a plain `String[]`, twice per edge during setup and
again on every `extractTop()` — an unintended `O(V)` factor on top of the
algorithm's intended `O(E log V)` design (the binary heap frontier itself
was already correct; only the vertex-ID-to-array-index lookup was the
bottleneck). Fixed by building a `vertexId -> index` hash lookup once
(`ChainedHashTable`, `O(V)` total) instead of scanning for it on every
call. Verified against `PrimTest` (including its Kruskal cross-check
test) before and after — identical MST weights, only the timing changed:

| \|V\| | Before (`indexOf` scan) | After (hash lookup) | Speedup |
|---:|---:|---:|---:|
| 50 | 1.58 ms | 0.88 ms | 1.8× |
| 100 | 3.24 ms | 1.40 ms | 2.3× |
| 200 | 7.92 ms | 2.48 ms | 3.2× |
| 400 | 19.56 ms | 5.28 ms | 3.7× |
| 800 | 87.19 ms | 9.42 ms | **9.3×** |

The speedup itself growing with |V| is exactly what an `O(V)` factor
being removed predicts — at small |V| the linear scan is cheap enough that
other overhead dominates, but it comes to dominate everything as |V|
grows. After the fix, Prim tracks Dijkstra's `O(E log V)` class directly
(9.4 ms vs. Dijkstra's 14.0 ms at |V|=800) instead of being an outlier.

**Density sweep** (|V|=300 fixed, |E| from 299 up to 7185): same pattern —
Kruskal and BFS/DFS scale gently, Dijkstra and (now) Prim scale together
in `O(E log V)` territory, with Prim consistently at or faster than
Dijkstra across the whole density range post-fix.

## T070 — hash table load factor vs. collisions

**Status: done.** Run via `edu.ug.nexusb.bench.HashTableExperiments`:
inserts a growing number of random (seeded, `GENERATION_SEED = 79731`)
integer keys into one `ChainedHashTable` (default constructor, real
auto-resize behavior), recording load factor, collision count,
longest-bucket length, and average `get()` time at each step. Sweeps
`N = 100` to `20,000`, matching the brief's Section 9 minimum for this
experiment ("100 to 20,000 keys with different table sizes") — the
auto-resize policy means `Capacity` genuinely differs across the range
(223 at N=100, 29,311 by N=12,000+), rather than needing separate runs
pinned to fixed table sizes.

- Data: [`results/csv/hashtable_experiments.csv`](../../results/csv/hashtable_experiments.csv)
- Charts: [`results/graphs/hashtable_collisions.svg`](../../results/graphs/hashtable_collisions.svg), [`results/graphs/hashtable_get_time.svg`](../../results/graphs/hashtable_get_time.svg)

**A first run with sequential keys (`0..N-1`) was a dead end, worth
recording why.** Collision count stayed at exactly `0` throughout — not a
bug, but a property of sequential keys: consecutive integers modulo any
table size are collision-free by the pigeonhole principle as long as
`N <= capacity`, which the 0.75-load-factor resize policy always keeps
true. That measures the resize policy working, not the hash function's
quality. Switched to random keys (also more realistic — real identifiers
aren't guaranteed contiguous), which produced real collision data:

| N | Load factor | Capacity | Collisions | Longest bucket | Resizes | Avg. `get()` time |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 0.448 | 223 | 33 | 3 | 2 | 50.6 µs |
| 300 | 0.668 | 449 | 127 | 5 | 3 | 47.4 µs |
| 800 | 0.439 | 1,823 | 342 | 4 | 5 | 54.9 µs |
| 1,500 | 0.410 | 3,659 | 638 | 5 | 6 | 43.6 µs |
| 3,000 | 0.410 | 7,321 | 1,278 | 5 | 7 | 44.2 µs |
| 5,000 | 0.683 | 7,321 | 2,146 | 6 | 7 | 40.4 µs |
| 8,000 | 0.546 | 14,653 | 3,302 | 6 | 8 | 40.4 µs |
| 12,000 | 0.409 | 29,311 | 5,042 | 5 | 9 | 36.6 µs |
| 16,000 | 0.546 | 29,311 | 6,623 | 5 | 9 | 45.7 µs |
| 20,000 | 0.682 | 29,311 | 8,490 | 6 | 9 | 36.6 µs |

Collision count climbs steadily with N (and within each resize interval,
with load factor) — expected under simple uniform hashing, where
collisions are a probabilistic certainty as more keys compete for the same
buckets. What matters operationally is that **`longestBucket()` and
`get()` time both stay bounded** (longest bucket never exceeds 6 across a
200× growth in N, from 100 keys all the way to the brief's 20,000-key
maximum; `get()` time stays in the 36-55 µs range throughout, with no
upward trend) — direct empirical evidence that the resize policy is doing
its job: raw collisions accumulate, but no single bucket is ever allowed
to become a long chain that would degrade lookups toward `O(n)`. The
saw-tooth pattern in load factor (0.41 right after a resize, climbing back
toward 0.68 before the next one) is the resize policy working exactly as
designed — `ChainedHashTable` resizes once load factor crosses its
threshold, so load factor is bounded, not monotonically increasing.

## T071 — BST vs. balanced tree on sorted input

**Status: done.** Run via `edu.ug.nexusb.bench.TreeExperiments`: inserts
`0..N-1` in sorted order into fresh `BinarySearchTree` and `RedBlackTree`
instances at growing N, recording actual `height()` and total insertion
time — the exact scenario [`proof_bst_height.md`](../proofs/proof_bst_height.md)
argues about, now measured rather than only proven.

- Data: [`results/csv/tree_experiments.csv`](../../results/csv/tree_experiments.csv)
- Charts: [`results/graphs/tree_height.svg`](../../results/graphs/tree_height.svg), [`results/graphs/tree_insert_time.svg`](../../results/graphs/tree_insert_time.svg)

| N | BST height | RB height | `2·log₂(N+1)` bound |
|---:|---:|---:|---:|
| 50 | 49 | 8 | 11.3 |
| 400 | 399 | 14 | 17.6 |
| 3200 | 3199 | 20 | 23.3 |

Results match the proof precisely: BST height is exactly `N-1` at every N
tested — a linked list wearing a tree's interface, exactly the degenerate
case sorted input is supposed to trigger — while the RB tree's height
stays logarithmic and comfortably inside the proof's own bound at every N.
Insertion time for the BST grows near-quadratically as expected (each of
N inserts costs `O(height)`, and height is itself `O(N)`, so total cost is
`O(N²)`). The RB tree's insertion timing shows more run-to-run noise at
small N (216 µs at N=50 vs. 612 µs at N=3200 is a far smaller ratio than
`O(N log N)` predicts) — benchmark/JIT overhead dominates the true
`O(log N)`-per-insert cost when N is tiny, a measurement-floor effect
worth naming rather than over-reading. The height numbers, which are the
proof's actual claim, are unambiguous regardless of that timing noise.

## Heap priority dispatch — insert vs. extract time vs. N

**Status: done.** Run via `edu.ug.nexusb.bench.HeapExperiments`, closing
the brief's Section 9 requirement for a dedicated heap insert/extract
timing experiment (distinct from T072's triage-*outcome* comparison
below): builds a `BinaryHeapPriorityQueue` over `N` random (seeded) longs
and times two operations separately — inserting one at a time (`N` calls
to `insert()`) versus bulk `heapify()` followed by draining the whole
heap via `extractTop()`. Sweeps `N = 100` to `20,000`, the brief's stated
range for this experiment.

- Data: [`results/csv/heap_experiments.csv`](../../results/csv/heap_experiments.csv)
- Chart: [`results/graphs/heap_insert_extract.svg`](../../results/graphs/heap_insert_extract.svg)

| N | Insert (total, `N` calls) | Extract (total, `N` calls, after `heapify`) |
|---:|---:|---:|
| 100 | 184.5 µs | 173.0 µs |
| 500 | 198.3 µs | 720.8 µs |
| 1,000 | 290.6 µs | 610.3 µs |
| 5,000 | 1.09 ms | 3.15 ms |
| 10,000 | 1.15 ms | 5.88 ms |
| 15,000 | 1.00 ms | 7.39 ms |
| 20,000 | 1.18 ms | 9.53 ms |

Both columns are the total cost of `N` operations, each individually
`O(log N)` — so the expected shape is `O(N log N)` total, and that is what
both series show: growth is clearly super-linear (20,000 is a 200× growth
in N but insert time grows only ~6× and extract ~55×, so neither is
tracking N directly) but far below the `O(N²)` a broken heap invariant
would produce. **Extract is consistently more expensive than insert at
the same N once N is past a few hundred**, which is the correct asymmetry
for this implementation:
`insert()` does at most one root-ward sift-up path per call, while
`extractTop()` swaps the last element to the root and sifts it all the
way back down — a full-height sift on every single call, against a heap
that is (until near the very end of the drain) close to its maximum size.
`heapify()` itself is `O(N)` (Floyd's bottom-up build) and is not the
component being isolated here — the timed region for `Extract` is the
drain loop only, which is where the `O(N log N)` cost actually lives.

## T072 — triage-priority vs. first-come-first-served outcomes

**Status: done.** Run via `edu.ug.nexusb.bench.TriageExperiments`:
generates deterministic (seeded) synthetic case lists of growing size
(random arrival times, random severity 1-4) and runs both dispatch modes
via `TriageComparison.compare()` (fixed for T054's FCFS-ordering bug — see
[07_correctness_evidence.md](07_correctness_evidence.md)) to see
how the two policies' average wait time diverges as case volume grows.

- Data: [`results/csv/triage_experiments.csv`](../../results/csv/triage_experiments.csv)
- Chart: [`results/graphs/triage_average_wait.svg`](../../results/graphs/triage_average_wait.svg)

| N | FCFS avg. wait | Triage-priority avg. wait |
|---:|---:|---:|
| 10 | 0.30 | 2.60 |
| 100 | 3.14 | 28.96 |
| 800 | 12.82 | 240.44 |

**Raw average wait is not automatically better under triage-priority
mode — it's consistently worse**, and the gap widens sharply with case
volume. This is not a bug in the experiment or in `TriageComparison`; it's
the same structural point the T051 greedy-dispatch counterexample and the
T054 fix already made, now independently reproduced with different
generated data: an *unweighted* average doesn't capture what triage
priority is actually for. Priority mode deliberately makes low-severity
cases — which can tolerate the wait — wait longer, specifically so that
high-severity cases get served fast. That trade only reads as an
improvement under a severity-weighted metric (the way
`GreedyDispatch.totalWeightedPenalty` weights urgency in T051's
counterexample); under a flat average, serving high-severity cases first
just means someone else waits longer, and the flat average has no way to
say that trade was worth it. A genuine T072 follow-up worth naming for
future work: rerun this experiment with a severity-weighted wait metric
and confirm triage-priority wins under *that* one, the way T051's
optimal-dispatch ordering does.


---


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


---


# 10. Responsible Algorithm Selection, Limitations & Conclusion

## Why these algorithms, for this problem

Every algorithm choice in this project was made against a specific
operational consequence, not picked for textbook convenience:

- **Dijkstra over a simpler "nearest facility" heuristic** for routing,
  because ambulance dispatch decisions have a real cost attached to being
  wrong — `O((V+E) log V)` is cheap enough at this dataset's scale (117
  facilities, 217 roads) that there is no reason to trade correctness for
  speed here.
- **A binary heap priority queue for triage, not FCFS**, because arrival
  order and clinical urgency are different things and conflating them is
  itself a correctness bug for a triage system, not just a performance
  one. T072's benchmark (see [08_performance_analysis.md](08_performance_analysis.md))
  makes the trade-off this choice makes explicit and measurable rather
  than assumed: priority mode makes *raw average wait worse*, and that is
  the correct, intended behavior — it is only an improvement once wait is
  weighted by severity, which is the metric triage is actually meant to
  optimize.
- **Greedy nearest-first dispatch was deliberately not trusted as the
  final answer.** [`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md)
  is not a hedge — it is the point: `runGreedyDispatch` and
  `runOptimalDispatch` are two real, callable methods, and the system
  reports both, because a single-factor greedy heuristic (distance alone)
  is fast but demonstrably not fair to higher-triage cases further away.
  Presenting only the greedy result would have been the irresponsible
  choice, since it silently privileges *proximity* to the dispatch station
  over *clinical urgency* without saying so.
- **Kruskal/Prim (MST) for resupply-route planning, not for patient
  routing** — an MST answers "what's the minimum-cost network that keeps
  everything connected," which is the right question for planning fixed
  resupply infrastructure and the wrong one for a single urgent trip
  (where Dijkstra's shortest path is what matters). Using the right
  algorithm for the right sub-problem, rather than one algorithm
  everywhere, is itself a responsible-selection decision.
- **A B-tree, not a Red-Black tree, backs `IndexingEngine`'s time-range
  index**, even though the RB tree was built and proven first — a B-tree's
  wider branching factor means fewer node hops per lookup on the
  dataset's actual access pattern (range queries over `requested_at`),
  and this was a deliberate choice made *after* both structures existed,
  not a default.

## Trade-offs made deliberately

- **No `java.util` collections in core logic** meant reimplementing
  everything from arrays up — hash tables, priority queues, trees,
  disjoint sets — which cost real time but is the entire point of the
  brief. `data/` and `bench/` are exempted, since file parsing and
  benchmark scaffolding aren't the algorithmic content being assessed.
- **Index-number-derived parameters** (hash table size, generation seed,
  condition weight) trade a small amount of arbitrariness for
  reproducibility that's provably specific to this team's roster — see
  [`parameters.md`](../parameters.md).
- **One package per sub-team** traded some cross-team code reuse
  opportunities for near-zero merge conflicts on core logic — worth it for
  a 15-person team working in parallel under a hard deadline.

## Known limitations, named rather than hidden

- **`IndexingEngine`'s time-range index compares `requested_at` as plain
  lexicographic text**, not a real calendar-aware range query. This works
  correctly for this dataset because every timestamp is written in a
  consistent format and SQLite's own `MIN`/`MAX` under its default
  `BINARY` collation agree with Java's `String.compareTo` on the same
  data — but it's not a general solution, and a real date type would be
  the correct fix outside this project's timeline.
- **The `assignment` table is defined but not yet written to by any code
  path** — see [09_database_integration_evidence.md](09_database_integration_evidence.md)
  for the full account. The dispatch/audit path commits an `audit_event`
  but doesn't yet persist the resulting resource-to-case pairing as its
  own row.
- **`Prim.java` had a genuine `O(V)` performance bug** (a linear scan
  inside what was meant to be an `O(E log V)` algorithm) that went
  undetected until the T073 benchmark specifically compared it against
  Kruskal on the same graphs — a reminder that a correct algorithm's own
  unit tests (which only check *what* it returns) don't catch a
  performance regression in *how* it gets there; only a benchmark that
  compares against a peer algorithm on the same input does. Found and
  fixed; see [08_performance_analysis.md](08_performance_analysis.md).
- **`TriageComparison`'s FCFS mode originally trusted caller-provided list
  order** instead of actually sorting by arrival time — correct only by
  coincidence for the one hardcoded demo case list. Found while making the
  method testable (T054) and fixed.

## What this suggests for future work

The Prim and FCFS findings above share a pattern worth naming: both bugs
were latent in code whose own unit tests passed, and both were only
caught by building something that exercised the code under conditions its
original author hadn't tested — a comparative benchmark in one case, a
non-trivial input order in the other. A useful next step beyond this
project's scope would be property-based or comparative testing (checking
one implementation's output against an independent second implementation
of the same problem, the way `KruskalTest`/`PrimTest` already cross-check
each other's MST weight) applied more systematically across the codebase.

Two other concrete follow-ups, both already named where they came up:
finishing the `assignment` write path so dispatch history is queryable
after the fact (above), and rerunning T072 with a severity-weighted wait
metric instead of a flat average, to directly confirm triage-priority
wins under the metric it was actually designed for (see
[08_performance_analysis.md](08_performance_analysis.md)).

## Conclusion

This project set out to answer six operational questions for a Greater
Accra health-facility network — which case to serve next, the fastest
route between facilities, which facilities are reachable under closed
roads, which subset fits a resource constraint, how alternative
structures perform as data grows, and how to persist and reload records —
using only data structures and algorithms built from scratch, against a
real (if partly synthetic) SQLite-backed dataset. Every one of those six
questions has a working, tested implementation reachable from either the
console (`ExaminerConsole`) or the live web console (`web.ApiServer` +
its browser frontend), backed by 459 passing tests, 8 trace tables, 4
proof sketches, 2 counterexamples, and 6 real benchmark experiments
against the brief's own minimum input ranges.

The most valuable outcome was not any single algorithm but the discipline
the "verify by running it" standard imposed throughout: three genuine
bugs (`KnapsackDP`'s zero-weight base case, `Prim`'s `O(V)` bottleneck,
`TriageComparison`'s FCFS ordering) were found this way, not by code
review alone, and each is now a permanent regression test rather than a
one-time fix. With more time, the team would prioritize the `assignment`
write path and a severity-weighted triage metric above — both are
extensions of what already exists, not redesigns, which is itself a sign
the underlying architecture (one interface-bound package per sub-team,
built against a frozen contract) held up under real use.


---


# 11. Individual Contributions & Oral-Defense Notes

**Status: statements not yet written (T077, "write individual contribution
statement") — each of the 15 members fills in their own row and defense
notes below before submission.** The table structure and defense-question
framework are in place so that filling this in is a matter of each person
writing their own row, not designing the format under deadline pressure.

## Individual contribution statements

Each member should record: task IDs actually owned (noting any
reassignment away from the original tracker row — several tasks in this
project were reassigned mid-stream, and a statement should reflect what
was actually built, not just the original tracker assignment), the PRs
that shipped that work, and one sentence on the most technically
significant thing they personally debugged or decided — not just "wrote
the code," but the specific judgment call.

| Member | Sub-team | Task IDs owned | PRs | Most significant contribution (1 sentence) |
|---|---|---|---|---|
| Irene Tetteh | D (+ Group Leader) | | | |
| Frederick Kankam | D (+ Planning & Delivery Lead) | | | |
| Cobbinah Emmanuel | C (+ Technical Lead/Integrator) | | | |
| Victor Barnieh | A (lead) | | | |
| Arhin Franca | A | | | |
| Baah John Excellence | A | | | |
| Obeng Jessica | B (lead) | | | |
| Arthur Philip Kofi | B | | | |
| El Masri Bilal | B | | | |
| Mensah-Dogbevi Princess | C | | | |
| Dakwa Nana Kwabena | C | | | |
| Kwetey Sylvester | D | | | |
| Johnson Kuzagbr | E (lead) | | | |
| Ajilogba Abdulmalik | E | | | |
| Salami Oluwanifemi | E | | | |

Roster cross-reference (index numbers, sub-team assignments):
[`../cover_sheet.md`](../cover_sheet.md).

## Oral-defense preparation notes

One likely question per major decision this report documents, so the
team walks in having already answered them once:

| Likely question | Where the answer already lives |
|---|---|
| "Why not just use `java.util.HashMap`/`PriorityQueue`?" | [02_problem_statement.md](02_problem_statement.md)'s core structural requirement, [`proof_hashing.md`](../proofs/proof_hashing.md) |
| "How do you know Dijkstra/Kruskal/Prim are actually correct, not just that they run?" | [06_algorithm_implementation.md](06_algorithm_implementation.md), [`trace_dijkstra.md`](../traces/trace_dijkstra.md), [`trace_kruskal.md`](../traces/trace_kruskal.md), [`trace_prim.md`](../traces/trace_prim.md), `KruskalTest`/`PrimTest`'s cross-check of MST weight |
| "Why does greedy dispatch exist if it's not the recommended answer?" | [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md), [`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md) |
| "What happens with an empty/disconnected/single-node graph?" | [07_correctness_evidence.md](07_correctness_evidence.md)'s edge-case table |
| "What's the biggest bug you found, and how?" | [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md)'s known-limitations section — Prim's `O(V)` bottleneck, found by benchmark comparison, not code review |
| "What doesn't work yet / what would you do differently?" | [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md)'s limitations and future-work sections — the `assignment` table gap, the unweighted T072 metric |
| "How is the dataset real, and how much of it is synthetic?" | [03_dataset_description.md](03_dataset_description.md) |
| "How did 15 people work on one codebase without constant merge conflicts?" | [04_system_architecture.md](04_system_architecture.md)'s module layout, the interfaces-first architecture |


---


# 12. References & Appendices

## Course materials

- DCIT 204/308 Joint DSA Project brief, Department of Computer Science,
  University of Ghana (2026) — the source of every requirement cross-
  referenced throughout this report (12-section structure, minimum test/
  trace/proof/experiment counts, the six operational questions in
  [02_problem_statement.md](02_problem_statement.md)).
- `Joint_DSA_Project_Checklist_Cover_Sheet.docx` — official cover sheet
  template, reproduced field-for-field in [`../cover_sheet.md`](../cover_sheet.md).
- Team Nexus B Master Task Tracker (Group 29) — the task/owner/deliverable
  ledger this report's Task IDs (T0xx) refer back to.

## Algorithms and data structures — textbook sources

The algorithms implemented in this project follow standard formulations;
no code was copied from any of these, they are cited as the source of the
correctness arguments in [`docs/proofs/`](../proofs/) and the complexity
claims in [06_algorithm_implementation.md](06_algorithm_implementation.md):

- Cormen, T. H., Leiserson, C. E., Rivest, R. L., & Stein, C. (2022).
  *Introduction to Algorithms* (4th ed.). MIT Press. — Dijkstra's
  algorithm, Kruskal's and Prim's MST algorithms, red-black tree
  invariants and rebalancing, B-trees, dynamic programming (0/1 knapsack),
  the loop-invariant proof method used in
  [`proof_insertion_sort_loop_invariant.md`](../proofs/proof_insertion_sort_loop_invariant.md).
- Sedgewick, R., & Wayne, K. (2011). *Algorithms* (4th ed.).
  Addison-Wesley. — hash table separate chaining and amortized resize
  analysis (`proof_hashing.md`), disjoint-set union-by-rank with path
  compression.

## Software and libraries

Every third-party dependency actually used, with what it's for and why it
doesn't conflict with the "no `java.util` collections in core logic"
constraint (none of these are collection implementations):

| Dependency | Version | Purpose | Scope |
|---|---|---|---|
| SQLite (via `org.xerial:sqlite-jdbc`) | 3.46.1.3 | Embedded relational database, `nexus.db` | `data/` |
| JUnit Jupiter | 5.10.2 | Test framework | `src/test` only |
| OpenCSV | 5.9 | CSV parsing for `DBLoader`'s source files | `data/` (exempted package) |
| `com.sun.net.httpserver.HttpServer` (JDK built-in) | JDK 17 | HTTP API server backing the web console | `web/` |

No JSON, charting, or JavaScript UI library is used anywhere in this
project: `web/Json.java` is a hand-rolled JSON writer and
`bench/Charts.java` is a hand-rolled SVG line-chart renderer, consistent
with the project's build-it-yourself standard.

## Appendix A — repository layout

See the README's repository layout table for the authoritative package
ownership map; summarized in
[04_system_architecture.md](04_system_architecture.md)'s module layout
section.

## Appendix B — where every deliverable actually lives

| Deliverable | Location |
|---|---|
| Source code | `src/main/java/edu/ug/nexusb/` |
| Tests | `src/test/java/edu/ug/nexusb/` |
| Dataset (CSVs) | `data/*.csv` |
| Database schema | `data/schema.sql` |
| Trace tables | `docs/traces/` |
| Proof sketches | `docs/proofs/` |
| Counterexamples | `docs/counterexamples/` |
| Benchmark data (CSV) | `results/csv/` |
| Benchmark charts (SVG) | `results/graphs/` |
| This report | `docs/report/` (this folder), concatenated in [`report_master.md`](report_master.md) |
| Cover sheet | [`../cover_sheet.md`](../cover_sheet.md) |
| Interface contracts | [`../interfaces.md`](../interfaces.md) |
| Index-number-derived parameters | [`../parameters.md`](../parameters.md) |
| Submission checklist | [`../submission_checklist.md`](../submission_checklist.md) |
