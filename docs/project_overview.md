# Team Nexus B — Project & Codebase Overview

**Hospital & Clinic Operations Optimizer**
DCIT 204/308 Joint Data Structures & Algorithms Project, Group 29, University of Ghana

This document is a full, narrative walk-through of what this project is, how
the codebase is organized, and — the main point of it — exactly how each data
structure and algorithm the brief requires got implemented *in service of a
real scenario*, not as an isolated textbook exercise. It complements the
formal 12-section report in [`report/`](report/) (which exists to satisfy the
brief's exact rubric) by explaining things in plain language, with real code,
for anyone picking up the codebase for the first time — a teammate prepping
for the oral defense, a marker, or a future maintainer.

Every number in this document was checked against the running system while
writing it, not copied from memory — see the verification note at the end of
each section that has one.

---

## 1. The scenario, in one paragraph

A district health network across Greater Accra — 117 real facilities
(teaching hospitals down to CHPS compounds and ambulance stations), linked by
217+ real roads — needs to triage incoming cases, dispatch ambulances and
response teams along the fastest available route, keep working when roads
close, plan a resupply backbone connecting every facility at minimum cost,
allocate a limited resource budget without exceeding it, look up and
range-query case records fast, and measure its own performance as the data
grows. Every one of those needs maps directly onto a data structure or
algorithm the course covers — this project didn't invent artificial reasons
to use a heap or a graph; the scenario already needed one.

## 2. How the codebase is organized

Fifteen people worked on one codebase without constant merge conflicts by
giving each of five sub-teams **its own package**, all built against a single
frozen contract (`core/`) agreed on before anyone wrote implementation code:

| Package | Owns | Sub-team |
|---|---|---|
| `core/` | Shared interfaces only — `MyComparator`, `MyIterator`/`MyIterable`, `Instrumented`, the `StructureException` hierarchy. Frozen after interface freeze; nothing else lives here. | — |
| `data/` | CSV loading, SQLite schema/DAO layer, the console demo | A |
| `linear/` | Dynamic array, linked list, stack, deque, circular/unbounded queue, binary heap priority queue | B |
| `trees/` | BST, Red-Black tree, B-tree, chained hash table, hash set | C |
| `graphs/` | Graph representations, disjoint set, BFS, DFS, Dijkstra, Kruskal, Prim | D |
| `algorithms/` + `bench/` | Sorting, searching, the benchmark/measurement framework | E |
| `optimization/`, `scheduling/` | Greedy dispatch, 0/1 knapsack DP, triage/FCFS comparison | shared, wired through `app/` |
| `app/` | The console menu and triage/dispatch engine that wires every package together against the real database | shared |
| `web/` | A live HTTP API + browser frontend exposing every capability above interactively | added after the tracker's Week 4 scope, shared |

**Why this mattered in practice**: two people never edit the same file, so
ownership boundaries alone prevent most merge conflicts — verified by this
project's own git history, which has essentially zero conflict-resolution
commits despite 15 contributors. The one real cost of this design showed up
whenever a bug fix legitimately needed to touch more than one package (e.g.
`GraphBuilder` used by both `graphs/` and `web/`) — those were coordinated
explicitly rather than silently merged.

**Why every structure is `Instrumented`**: wall-clock timings vary across
fifteen different laptops; comparison and movement counts don't.
`core.Instrumented` puts a cheap, resettable counter on nearly every
structure specifically so that when a benchmark graph looks noisy, there's
independent, machine-agnostic evidence backing up (or contradicting) it.

**The hard rule everything below has to satisfy**: no `java.util` collections
(`ArrayList`, `HashMap`, `PriorityQueue`, `Stack`, `LinkedList`, `ArrayDeque`,
`TreeMap`/`TreeSet`, `Vector`) anywhere under `src/main` except `data/` and
`bench/`, which are exempted because file parsing and benchmark scaffolding
aren't the algorithmic content being assessed. This is enforced by a CI regex
check on every push, not just a convention — a forgotten `import
java.util.ArrayList` fails the build automatically.

## 3. The data model

`data/schema.sql` defines seven SQLite tables. `facility` and `road_link`
(with its `v_weighted_edge` view) are the routing graph's vertices and edges;
`case_request` and `resource` are what gets triaged and dispatched;
`algorithm_run` is written to by every benchmark for reproducible
measurement; `audit_event` backs a stack-based undo trail; `assignment` is
defined but not yet written to by any code path (a known, explicitly
documented gap, not a hidden one — see §11).

`v_weighted_edge` computes the number every routing algorithm actually uses:

```sql
effective_time_min = base_time_min * traffic_weight * condition_factor
```

where `condition_factor` comes from `road_condition` (GOOD/FAIR/POOR) and the
team's own `CONDITION_WEIGHT_FACTOR = 1.08`. This means Dijkstra, Prim, and
Kruskal are never routing on raw distance — they route on *estimated travel
time under current conditions*, which is the operationally meaningful metric
for an ambulance dispatch system.

**Facility and road data is real** (compiled by the team from their own
knowledge of Greater Accra); **case and resource data is synthetic**,
generated from an index-number-derived seed (`GENERATION_SEED = 79731`,
`docs/parameters.md`) so the "random" dataset is reproducible and
traceable to this specific team's roster, not an arbitrary run. No patient
names, contact details, or clinical notes exist anywhere in the schema.

## 4. Data structures — why each one, and how it's built

Every structure below is tested for the normal case, a boundary case, and
invalid input — audited directly (not assumed) during the project, which is
how a hand-rolled `DisjointSetTest` with no `@Test` annotations that Maven
silently never ran got caught and fixed.

### Linear structures (`linear/`)

- **`DynamicArrayList`** — amortized `O(1)` append via doubling. Backs
  anywhere a growable, indexable list is needed without `java.util.ArrayList`.
- **`DoublyLinkedList`** — `O(1)` insertion at a held position. The `MyList`
  contract's comparison point against the array-backed version.
- **`ArrayStack`** — backs the audit-trail undo (`data.AuditTrail`) and the
  iterative version of DFS.
- **`ArrayDeque`** — models an urgent case pushed to the *front* of a queue
  rather than the back, the structural reason a deque exists in this project
  at all rather than a plain queue.
- **`ArrayCircularQueue` / `ArrayQueue`** — a fixed-capacity wrap-around
  queue and an unbounded one, satisfying `MyQueue`'s contract that
  `isFull()`/`capacity()` return `false`/`-1` on the unbounded version.
- **`BinaryHeapPriorityQueue`** — the structure the whole triage story
  depends on. `insert()` appends then sifts up; `extractTop()` swaps the
  last element into the root, shrinks, and sifts down; `heapify()` builds a
  heap from an existing array bottom-up in `O(n)` rather than `n` repeated
  inserts (`O(n log n)`) — used specifically by the T072 experiment and
  Dijkstra's frontier. `decreaseKey()` exists purely for Dijkstra's benefit
  — nothing inside `linear/` itself calls it, but it's part of the frozen
  contract because Sub-team D's algorithm needs it.

```java
// BinaryHeapPriorityQueue.java — the two operations everything above rests on
public void insert(T value) {
    ensureCapacity(count + 1);
    items[count] = value;
    siftUp(count);
    count++;
}

public T extractTop() {
    T top = items[0];
    count--;
    items[0] = items[count];
    items[count] = null;
    if (count > 0) siftDown(0);
    return top;
}
```

### Trees & hashing (`trees/`)

- **`BinarySearchTree` vs. `RedBlackTree`** — the deliberate comparison at
  the heart of this project's trees story: a plain BST degenerates to
  `O(n)` on sorted input (verified by experiment — see §9), while the RB
  tree's rotations keep height provably `O(log n)` regardless of insertion
  order (`docs/proofs/proof_bst_height.md`).
- **`BTree`** — chosen over the RB tree for `IndexingEngine`'s time-range
  index specifically because its wider branching factor means fewer node
  hops per range query on the dataset's actual access pattern, a deliberate
  choice made *after* both structures already existed and were compared,
  not a default.
- **`ChainedHashTable`** — separate chaining with an auto-resize policy
  triggered once load factor crosses 0.75:

```java
// ChainedHashTable.java
private static final double LOAD_FACTOR_THRESHOLD = 0.75;
...
if (loadFactor() > LOAD_FACTOR_THRESHOLD) {
    resize();
}
```

  It exposes collision count, longest-bucket length, and resize count as
  first-class methods (beyond the base `MyMap` contract) precisely so a
  benchmark can see *why* a lookup was slow, not just that it was —
  `IndexingEngine` uses it for O(1)-average case-reference lookup.
- **`HashSet`** — a thin `MySet` wrapper over `ChainedHashTable`, each
  element stored as a key mapped to a shared sentinel value.

### Graphs (`graphs/`)

- **`AdjacencyListGraph` / `AdjacencyMatrixGraph`** — both implement the
  same `MyGraph` interface (18 shared contract tests plus a dedicated
  cross-check test confirming both representations agree on every query),
  so every algorithm below is representation-agnostic — it was written
  once, against the interface, not once per representation.
- **`DisjointSet`** — path compression + union by rank, Kruskal's actual
  dependency for cycle detection in near-`O(1)` amortized time per
  operation.
- **`GraphBuilder`** — assembles a `MyGraph` from the `facility`/`road_link`
  tables: one directed edge per row, plus its reverse when the road isn't
  flagged one-way. (This exact piece of logic is what a real data bug
  exploited — see §11.)

## 5. Algorithms — tied to the scenario, one by one

This is the section that actually answers "how did we implement the
algorithms" — each one below exists because the scenario needed the specific
question it answers, not because the brief has a checkbox for it.

### Dijkstra's shortest path — "what's the fastest route right now?"

**Scenario question**: given an ambulance station and a case, what's the
fastest route under current road conditions?

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

Real implementation (`graphs/Dijkstra.java`) builds its own priority queue
(`BinaryHeapPriorityQueue<Entry>`, ordered by distance) and its own
`vertexId -> array index` bookkeeping, since this project builds every
structure from scratch rather than relying on object identity inside a
library priority queue. Complexity: `O((V+E) log V)` with the binary heap.
Verified step-by-step against a real 6-vertex example in
[`traces/trace_dijkstra.md`](traces/trace_dijkstra.md).

### BFS reachability — "what's still reachable if these roads are closed?"

**Scenario question**: flooding or roadworks close specific roads — which
facilities can still be reached at all?

`graphs/Reachability.bfsReachable` takes a source and a set of closed edge
keys, and returns the reachable set — `O(V+E)`. This is exactly what the web
console's Routing tab's "close road" feature exercises live.

### DFS cycle detection — "is there a referral loop?"

**Scenario question**: could Facility A refer a case to B, which refers back
to A, forming a loop that never resolves?

`graphs/Dfs.java` does a whole-network DFS with two-state (on-stack/finished)
tracking, which specifically avoids the false positive a naive
one-state visited check would raise on a diamond-shaped DAG (A→B, A→C,
B→D, C→D is *not* a cycle, but a careless visited-only check can
misclassify it as one). `O(V+E)`.

### Kruskal & Prim — "what's the cheapest way to keep every facility connected?"

**Scenario question**: planning a fixed resupply backbone (not a single
urgent trip) — what's the minimum-cost set of roads that keeps every
facility connected at all?

```
KRUSKAL(edges, numVertices):
    sort edges ascending by weight
    dsu := new DisjointSet(numVertices)
    mst := []
    for edge in sorted edges:
        if dsu.union(edge.src, edge.dest):   // true iff this doesn't close a cycle
            mst.add(edge)
    return mst, sum(mst edge weights)
```

Real code (`graphs/Kruskal.java`):

```java
for (int i = 0; i < sorted.length && edgesUsed < mst.length; i++) {
    Edge edge = sorted[i];
    if (dsu.union(edge.src, edge.dest)) {
        mst[edgesUsed] = edge;
        totalWeight += edge.weight;
        edgesUsed++;
    }
}
```

`Prim.java` takes the alternative frontier-heap approach instead of
edge-sorting — grow one tree from a start vertex, always adding the
cheapest edge leaving the current tree. Both are `O(E log V)`-class and are
cross-checked against each other in `PrimTest` to confirm they agree on
total weight, correctness backed by the cut-property exchange argument in
[`proofs/proof_kruskal.md`](proofs/proof_kruskal.md).

**A real bug this algorithm exposed**: `Prim.java` originally did a
**linear `O(V)` scan** (`indexOf(String[], String)`) to map a vertex ID to
its array index, called on every edge and every extraction — an unintended
`O(V)` factor bolted onto what was supposed to be `O(E log V)`. It went
undetected by unit tests (which only check *what* Prim returns) until a
benchmark specifically compared it against Kruskal on the same graph and
found Prim 55-70× slower for no algorithmic reason. Fixed by building a
`vertexId -> index` hash lookup once instead of scanning for it on every
call — 1.8× to 9.3× faster depending on graph size, with identical MST
weights before and after. The lesson this left behind: a correct
algorithm's own tests don't catch a performance regression in *how* it gets
there — only a comparative benchmark does.

### Greedy dispatch vs. optimal dispatch — "which case first?"

**Scenario question**: with one resource and several waiting cases at
different distances and urgency levels, what order minimizes harm?

Two real, callable methods exist side by side, on purpose:

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

The single-character difference between these two — multiplying by
`triageLevel` or not — is deliberately the entire mechanism behind the
brief's required greedy-failure counterexample
([`counterexamples/counterexample_greedy_dispatch.md`](counterexamples/counterexample_greedy_dispatch.md)):
nearest-first greedy privileges *proximity* over *clinical urgency* without
saying so, and scores measurably worse (8.75 vs. 7.0 penalty units) on an
engineered case where a far-away urgent case waits behind a nearby routine
one. Presenting only the greedy result would have been the irresponsible
choice — the system reports both, explicitly, so the trade-off is visible
rather than hidden. Both run in `O(n log n)`, dominated by the sort.

### 0/1 Knapsack DP — "what fits the shift's resource budget?"

**Scenario question**: given a fixed shift budget (staff-hours, beds,
equipment slots), which combination of resource commitments maximizes value
without exceeding it?

```java
// KnapsackDP.java — the actual recurrence
for (int i = 0; i <= n; i++) {
    for (int w = 0; w <= capacity; w++) {
        if (i == 0) {
            dp[i][w] = 0;
        } else if (weights[i - 1] <= w) {
            dp[i][w] = Math.max(values[i - 1] + dp[i - 1][w - weights[i - 1]], dp[i - 1][w]);
        } else {
            dp[i][w] = dp[i - 1][w];
        }
    }
}
```

`O(n · capacity)` time and space; backtracks from `dp[n][capacity]` to
recover *which* items were chosen, not just the max value, verified
cell-for-cell against a hand-traced example in
[`traces/trace_knapsack.md`](traces/trace_knapsack.md).

**A real bug this uncovered**: the original base case was `if (i == 0 || w
== 0)`, which zeroed out `dp[i][0]` for every row — meaning a **zero-weight
item was always dropped even when it should always be includable at any
capacity**, including zero. Found while writing a boundary test
(`zeroWeightItemIsAlwaysWorthIncluding`), fixed to `if (i == 0)` only. A
genuine algorithmic correctness bug caught by insisting on boundary-case
tests, not a hypothetical one.

### Sorting & searching (`algorithms/`)

Four sorts (merge, quick, insertion, selection) and two searches (linear,
binary) — used throughout the system for ordering case lists by arrival
time or severity, and for looking up a case reference in an unsorted vs.
sorted context. The one required counterexample here isn't a made-up edge
case: **binary search on unsorted input silently returns "not found" for a
present element**, because the algorithm's entire correctness argument
depends on the sortedness precondition — violating it is a correctness bug,
not something that throws an exception. Worked through in
[`traces/trace_binarysearch.md`](traces/trace_binarysearch.md) and asserted
directly by a real test.

### Hashing & indexing — "look up a case by reference, or by time range"

**Scenario question**: given a case reference string, find its record fast;
given a time window, find every case requested in it.

`app/IndexingEngine` wires the two structures above to their actual jobs:
`ChainedHashTable` for `O(1)`-average reference lookup, `BTree` for ordered
range scans. Both are exercised against real database rows, not just
synthetic in-memory data (`IndexingEngineDatabaseIntegrationTest`).

## 6. The triage/dispatch engine — where everything meets

`app/TriageDispatchEngine` and `bench/TriageComparison` are where the
priority queue's whole reason for existing becomes concrete: FCFS mode
serves cases in arrival order; priority mode always serves the
highest-severity waiting case next, via the binary heap. Benchmarked at
scale (T072), priority mode's *raw average wait is consistently worse*, not
better — and that's correct, not a bug. It deliberately makes low-severity
cases (which can tolerate the wait) wait longer specifically so high-severity
cases get served fast; that trade only reads as an improvement once wait is
weighted by severity, the same point the greedy-dispatch counterexample
makes independently with completely different generated data.

**A real bug found here too**: `TriageComparison`'s FCFS mode originally
trusted the caller-provided list order instead of actually sorting by
arrival time — correct only by coincidence for the one hardcoded demo case
list, and wrong the moment a differently-ordered list was fed in. Found
while making the method testable, fixed to sort explicitly.

## 7. The live web console

`web/ApiServer` (a bare `com.sun.net.httpserver.HttpServer`, zero new Maven
dependency, consistent with the project's build-it-yourself standard) exposes
every capability above as a JSON API, and `resources/web/index.html` (a
single-page frontend, no build step, no framework) calls it. Nothing in
`ApiServer` re-implements an algorithm — every response comes from calling
the exact same classes the test suite exercises.

The frontend renders on a real satellite map (Leaflet + Esri World Imagery),
plotting all 117 facilities and roads at their actual coordinates rather than
an abstract diagram. Results (a Dijkstra path, an MST, a dispatch order) draw
with direction arrows computed from real compass bearing per hop, auto-focus
to hide unrelated background roads, and zoom in tight to just the nodes
involved — hover a marker for its name/code. Run it with:

```
mvn exec:java -Dexec.mainClass=edu.ug.nexusb.app.App -Dexec.args="--init-db"   # once
mvn compile exec:java -Dexec.mainClass=edu.ug.nexusb.web.ApiServer
```
then open `http://localhost:8080/`.

## 8. Testing & correctness — what "verified" means here

**459 tests, all passing** on a clean build as of this writing (confirmed by
actually running the full suite while writing this document, not quoted from
memory). Every structure and algorithm is tested for the normal case, a
boundary case, and invalid input — audited directly rather than assumed,
which is how a `DisjointSetTest` with no working `@Test` annotations (silently
never run by Maven despite looking covered) got caught. 8 trace tables and 4
proof sketches cover all three of the brief's required proof categories (loop
invariant, induction/recursion, greedy/DP correctness), plus 2 required
counterexamples. Full detail: [`report/07_correctness_evidence.md`](report/07_correctness_evidence.md).

Every trace table, proof, and benchmark number in this project's
documentation was either generated by actually running the real
implementation and transcribing the output, or hand-computed and then
checked against a real run before being written down.

## 9. Performance, briefly

Six benchmark experiments (search/sort comparison, hash table load factor to
20,000 keys, BST vs. balanced tree, heap insert/extract, graph algorithms vs.
size/density, triage-priority vs. FCFS) — all with real CSV data and SVG
charts under [`results/`](../results/), interpreted (not just plotted) in
[`report/08_performance_analysis.md`](report/08_performance_analysis.md).
Headline results: a plain BST degenerates to height exactly `N-1` on sorted
input while a Red-Black tree over the same input stays logarithmic; the hash
table's longest bucket never exceeds 6 across a 200× growth in key count;
Prim's fix turned a 55-70× performance outlier into a class-appropriate
`O(E log V)` curve.

## 10. Known limitations, named rather than hidden

- **The `assignment` table is defined but not yet written to** by any code
  path — dispatch commits an audit event but doesn't yet persist the
  resulting resource-to-case pairing as its own row.
- **`IndexingEngine`'s time-range index compares timestamps as plain text**,
  which works correctly for this dataset's consistent format but isn't a
  general date-aware solution.
- **Until recently, the road network was a zero-cycle spanning tree** — every
  road was load-bearing with no redundancy, meaning a single road closure
  could disconnect part of the network, and Dijkstra had no alternative route
  to choose between two points even when a shorter one was geographically
  obvious. Fixed by (1) correcting 15 roads that were incorrectly flagged
  one-way with no reverse road recorded, which alone had made 64 of 117
  facilities unreachable from Korle Bu, and (2) adding one redundant road to
  every facility that had degree ≤ 1 (29 facilities), connecting each to its
  nearest previously-unconnected geographic neighbor. A real routing case
  that used to take 33 hops / 126 effective-minutes now resolves in 4 hops /
  15 minutes, and Kruskal's MST is now a genuine selection (302 total weight)
  rather than being forced to include literally every road (466, when there
  were no alternatives at all).

## 11. Where to go next

- The formal, brief-compliant report (12 sections, matches the course rubric
  exactly): [`report/00_index.md`](report/00_index.md)
- Interface contracts every package was built against:
  [`interfaces.md`](interfaces.md)
- Index-number-derived reproducibility parameters:
  [`parameters.md`](parameters.md)
- Submission readiness checklist: [`submission_checklist.md`](submission_checklist.md)
