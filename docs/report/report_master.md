# Hospital & Clinic Operations Optimizer — Technical Report

**Team Nexus B · Group 29 · DCIT 204/308 Joint Data Structures & Algorithms Project**
University of Ghana · 26 July – 22 August 2026

---

_This file is the assembled Markdown source for `report_final.docx`/`.pdf` (T075) —
concatenated from `docs/report/01_*.md` through `11_*.md` in order. Convert with
`pandoc report_master.md -o report_final.docx` (or `.pdf`) once sections 10-11 are
filled in. As of this assembly, sections 1-9 have real content, including all four
Week 4 experiments (T070-T073); sections 10 (individual contributions) and 11
(conclusion) are intentionally still stubs — they depend on every member's own
statement and on the rest of Week 4 (T076-T081). Do not treat this file as final
until those are in._

---

# 1. Introduction

**Team Nexus B · Group 29 · DCIT 204/308 Joint Data Structures & Algorithms
Project**, University of Ghana, 26 July – 22 August 2026.

## What was built

A service-operations platform for a synthetic Greater Accra health facility
network: it triages incoming cases, dispatches ambulances and response
teams over a weighted road graph, indexes case records for fast lookup, and
measures its own algorithmic performance. Fifteen members split across five
sub-teams, one package each (`data/`, `linear/`, `trees/`, `graphs/`,
`algorithms/`+`bench/`), coordinated through a shared `core/` package of
frozen interfaces and an `app/` layer that wires the pieces together.

## Why it exists

The brief's core requirement is that every data structure and algorithm in
the system be implemented from scratch — no `java.util` collections
anywhere in `src/main/java/edu/ug/nexusb` outside the `data/` and `bench/`
packages, which are exempted for file parsing, database plumbing, and
benchmark scaffolding rather than core algorithmic logic. That constraint
is enforced by a CI check on every push
(`.github/workflows/build.yml`), not just a style guideline: a forgotten
`import java.util.ArrayList` fails the build.

The problem domain — dispatch, triage, routing, records — was chosen
because it naturally needs almost every structure and algorithm the course
covers, and because each one maps to a concrete, motivated use case rather
than an abstract exercise: a priority queue for triage isn't a demo of
heaps, it's how an emergency case actually jumps the line ahead of a
routine one.

## Scope of this document

Sections 2-5 cover what the system does and how it's built; sections 6-8
cover the algorithms, their correctness/complexity evidence, and the
benchmark results measuring them; section 9 is an honest accounting of
trade-offs and known limitations; sections 10-11 close with individual
contributions and a conclusion, written last, once the rest is settled.


---

# 2. Requirements & Scope

## Functional capabilities

| Capability | Structures used | Algorithms used |
|---|---|---|
| Triage incoming cases | binary heap | heap-based priority dispatch |
| Baseline comparison policy | queue, circular queue | first-come-first-served |
| Route an ambulance | graph, binary heap | Dijkstra |
| Reachability when roads close | graph, queue | BFS |
| Detect referral loops | graph, stack | DFS + cycle detection |
| Plan the resupply backbone | graph, disjoint set, heap | Kruskal, Prim |
| Assign nearest available unit | heap | greedy (+ documented counterexample) |
| Allocate a limited shift budget | dynamic array | dynamic programming (knapsack) |
| Look up a case by reference | hash table | hashing |
| Query admissions by time range | BST, balanced tree, B-tree | binary search / range query |
| Undo the last decision | stack | — |

## Structural requirement

Every structure and algorithm implemented from scratch (see
[01_introduction.md](01_introduction.md)); every one tested for the
normal case, a boundary case, and invalid input — not just the happy path,
since that is explicitly where the brief says marks are actually lost.

## Out of scope

- Real patient data of any kind — the schema has no column that could hold
  it (see [03_data_and_assumptions.md](03_data_and_assumptions.md)).
- A production-grade persistence layer — `nexus.db` is a local, gitignored
  SQLite file regenerated from `data/*.csv` on demand, not a managed
  database.
- A UI beyond the console menu (`ExaminerConsole`) — this is a DSA
  demonstration project, not a deployable product.

## Ownership boundaries

One package per sub-team (`README.md`'s repository layout table), so two
people never edit the same file and never hit a merge conflict on core
logic. `core/` is the only shared package, frozen after interface freeze
(30 Jul) and changeable only with the Technical Lead's sign-off.


---

# 3. Data & Assumptions

Full detail: [`data_dictionary.md`](../data_dictionary.md) (field-level
schema for every CSV) and [`evidence_note.md`](../evidence_note.md) (what
the dataset approximates and why). Summarized here.

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

# 4. System Design & Architecture

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
`graphs/` (D), `algorithms/`+`bench/` (E) — plus `core/` (frozen, shared)
and `app/` (triage/dispatch wiring, console menu). Two people never edit
the same file, so ownership boundaries alone prevent most merge conflicts;
`docs/interfaces.md` documents each package's specific contracts and the
cross-team dependencies worth protecting (e.g. `MyPriorityQueue
.decreaseKey()` exists solely for Dijkstra's benefit — nothing in
Sub-team B's own module calls it, but it must not be dropped as
"unused").

## Data flow

`data/*.csv` → `DBLoader` → `nexus.db` (SQLite) → `GraphBuilder` /
`IndexingEngine` / `TriageDispatchEngine` build in-memory structures from
the DB → `app.ExaminerConsole` demonstrates each capability against real
data end to end (T056).


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
  [04_system_design.md](04_system_design.md).

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
[06_algorithms_and_complexity.md](06_algorithms_and_complexity.md) rather
than here, since the interesting claims about them are complexity and
correctness, not structural invariants.


---

# 6. Algorithms & Complexity Analysis

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
| Prim (T050) | `O(E log V)` | [`trace_prim.md`](../traces/trace_prim.md); see [08_benchmarks_and_results.md](08_benchmarks_and_results.md) for a real `O(V)` bug found and fixed in the original implementation |

## Sorting & searching (`algorithms/`, Sub-team E)

| Algorithm | Complexity | Evidence |
|---|---|---|
| Merge sort (T040) | `O(n log n)` worst case, stable | [`trace_mergesort.md`](../traces/trace_mergesort.md) |
| Quicksort (T040) | `O(n log n)` average, `O(n²)` worst | [`trace_quicksort.md`](../traces/trace_quicksort.md) |
| Selection sort / Insertion sort (T041) | `O(n²)` | boundary + null-input coverage |
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


---

# 7. Testing & Verification

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

**457 tests, all passing**, as of the T073/Prim-fix pass (`mvn test`) —
comfortably over the brief's 40-test minimum. Breakdown by package:

- `trees/`: 130 declared test methods (BST 37, B-tree 40, RB-tree 22,
  chained hash table 18, hash set 13)
- `graphs/`: ~90 declared test methods across structures and algorithms
  (several parameterized across both graph representations, so the
  executed count is higher than the declared count)
- `linear/`: 65 declared test methods
- `algorithms/`+`optimization/`: ~60 declared test methods
- `bench/`, `data/`, `app/`: integration and demo-scenario tests

## What "verified" means in this report

Every trace table, proof sketch, and benchmark number in this report was
either (a) generated by actually running the real implementation and
transcribing its output, or (b) hand-computed and then checked against a
real run before being written down — not asserted from theory alone. Where
a discrepancy between expectation and measurement turned up (the Prim
`O(V)` bottleneck, the DP zero-weight edge case, the FCFS ordering bug),
it was investigated to a root cause and fixed rather than left unexplained.


---

# 8. Benchmark Methodology & Results

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
longest-bucket length, and average `get()` time at each step.

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

| N | Load factor | Capacity | Collisions | Longest bucket | Resizes |
|---:|---:|---:|---:|---:|---:|
| 10 | 0.19 | 53 | 1 | 2 | 0 |
| 100 | 0.45 | 223 | 33 | 3 | 2 |
| 500 | 0.55 | 907 | 216 | 5 | 4 |
| 2000 | 0.55 | 3659 | 831 | 5 | 6 |

Collision count climbs steadily with N (and within each resize interval,
with load factor) — expected under simple uniform hashing, where
collisions are a probabilistic certainty as more keys compete for the same
buckets. What matters operationally is that **`longestBucket()` and
`get()` time both stay bounded** (longest bucket never exceeds 5 across a
200× growth in N; `get()` time stays in the 20-55 µs range throughout,
with no upward trend) — direct empirical evidence that the resize policy
is doing its job: raw collisions accumulate, but no single bucket is ever
allowed to become a long chain that would degrade lookups toward `O(n)`.

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

## T072 — triage-priority vs. first-come-first-served outcomes

**Status: done.** Run via `edu.ug.nexusb.bench.TriageExperiments`:
generates deterministic (seeded) synthetic case lists of growing size
(random arrival times, random severity 1-4) and runs both dispatch modes
via `TriageComparison.compare()` (fixed for T054's FCFS-ordering bug — see
[07_testing_and_verification.md](07_testing_and_verification.md)) to see
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

# 9. Discussion, Limitations & Future Work

## Trade-offs made deliberately

- **No `java.util` collections in core logic** meant reimplementing
  everything from arrays up — hash tables, priority queues, trees, disjoint
  sets — which cost real time but is the entire point of the brief. `data/`
  and `bench/` are exempted, since file parsing and benchmark scaffolding
  aren't the algorithmic content being assessed.
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
  consistent format and SQLite's own `MIN`/`MAX` under its default `BINARY`
  collation agree with Java's `String.compareTo` on the same data — but
  it's not a general solution, and a real date type would be the correct
  fix outside this project's timeline.
- **`Prim.java` had a genuine `O(V)` performance bug** (a linear scan
  inside what was meant to be an `O(E log V)` algorithm) that went
  undetected until the T073 benchmark specifically compared it against
  Kruskal on the same graphs — a reminder that a correct algorithm's own
  unit tests (which only check *what* it returns) don't catch a
  performance regression in *how* it gets there; only a benchmark that
  compares against a peer algorithm on the same input does. Found and
  fixed; see [08_benchmarks_and_results.md](08_benchmarks_and_results.md).
- **`TriageComparison`'s FCFS mode originally trusted caller-provided list
  order** instead of actually sorting by arrival time — correct only by
  coincidence for the one hardcoded demo case list. Found while making the
  method testable (T054) and fixed.
- **Three of the four Week 4 benchmark experiments** (T070 hash table load
  factor, T071 BST vs. balanced tree, T072 triage-priority vs. FCFS) were
  still outstanding as of this writing — see
  [08_benchmarks_and_results.md](08_benchmarks_and_results.md) for what's
  actually measured versus what's still pending.

## What this suggests for future work

The Prim and FCFS findings above share a pattern worth naming: both bugs
were latent in code whose own unit tests passed, and both were only caught
by building something that exercised the code under conditions its
original author hadn't tested — a comparative benchmark in one case, a
non-trivial input order in the other. A useful next step beyond this
project's scope would be property-based or comparative testing (checking
one implementation's output against an independent second implementation
of the same problem, the way `KruskalTest`/`PrimTest` already cross-check
each other's MST weight) applied more systematically across the codebase.


---

# 10. Individual Contributions

**Status: not yet written (T077, "write individual contribution statement").**

Each of the 15 members writes their own statement here (or in a linked
per-member file) — task IDs owned, PRs merged, and anything notable about
how the work was actually split versus how the tracker originally assigned
it (several tasks in this project were reassigned mid-stream; contribution
statements should reflect what each person actually built, not just their
original tracker row).


---

# 11. Conclusion

**Status: not yet written (T075) — write this last.**

Scope for this section: a short summary of what was built, whether it met
the brief's requirements, and what the team would do differently with more
time. Should be written after every other section, once there's an actual
result to summarize.

