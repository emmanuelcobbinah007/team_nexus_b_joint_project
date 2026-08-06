# Frozen Interface Contracts

**Status:** locked at the interface freeze, Thursday 30 July 2026.
Any change to a signature in `edu.ug.nexusb.core` after that date requires the
Technical Lead's written approval in the group chat.

This is the human-readable companion to the Javadoc. It records *why* each
contract looks the way it does, which is what the oral defense asks about.

---

## Why interfaces come first

Five sub-teams build simultaneously. Sub-team D writes Dijkstra against
`MyPriorityQueue` in Week 3 while Sub-team B is still refining the heap. That
only works if the contract is settled and stable. Freezing it converts a
sequential project into a parallel one.

---

## Shared types

| Type | Purpose |
|---|---|
| `MyComparator<T>` | Ordering strategy. Declared here rather than reusing `java.util.Comparator` so that no core logic depends on the Collections Framework. |
| `MyIterator<T>` / `MyIterable<T>` | Traversal. Implementations fail fast: modifying a structure during iteration throws rather than returning stale data. |
| `Instrumented` | Comparison and movement counters, resettable by the benchmark harness. |
| `StructureException` and subclasses | `EmptyStructureException`, `CapacityExceededException`, `KeyNotFoundException`. |

**Why `Instrumented` is on nearly everything.** Wall-clock timings vary across
fifteen different laptops. Comparison counts do not. When the Week 4 graphs are
noisy, the counter data is the defensible evidence. Counters must be cheap —
increment a `long`, nothing else. Adding them later would invalidate every
measurement already taken.

---

## T018 — Sub-team B: linear structures

| Interface | Notes |
|---|---|
| `MyList<T>` | Backs both the dynamic array and the linked list. The array offers O(1) indexing and amortised O(1) append; the linked list offers O(1) insertion at a held position but O(n) indexing. The report compares them. |
| `MyStack<T>` | Backs the audit-trail undo and the iterative DFS. All operations O(1). |
| `MyQueue<T>` | Implemented twice: unbounded linked, and fixed-capacity circular. `isFull()` and `capacity()` return `false` / `-1` for the unbounded version. Wrap-around is a required trace-table subject. |
| `MyDeque<T>` | Models a prepared case pushed back to the front of the line rather than the end. |
| `MyPriorityQueue<T>` | Triage. `insert` and `extractMin` O(log n), `peekMin` O(1), `buildFrom` O(n). |

**Cross-team dependency to protect:** `MyPriorityQueue.decreaseKey()` exists
solely because Dijkstra needs it when a shorter route to an already-discovered
facility is found. Nothing in Sub-team B's own module calls it. It must not be
dropped as "unused".

**Why `buildFrom` is separate from repeated `insert`.** Heapifying in O(n) by
sifting down from the last internal node is measurably faster than n insertions
at O(n log n). Having both makes a clean experiment and a strong defense answer.

---

## T019 — Sub-team C: trees and hashing

Lives in `edu.ug.nexusb.trees`, not `edu.ug.nexusb.core` — same reasoning as
T020: none of these four are part of the small set of types actually shared
across every package (see "Shared types" above, which *does* live in `core`).

| Interface | Notes |
|---|---|
| `MyMap<K,V>` | The base contract shared by the hash table and the tree-backed map, so both can be swapped and benchmarked against each other through identical operations. |
| `MySet<T>` | Visited-marking in traversals (BFS/DFS depend on this), duplicate detection on load. Extends `MyIterable<T>` directly rather than exposing a separate accessor, since a set's only natural iteration view is its own elements. |
| `MyHashTable<K,V>` | Extends `MyMap` with observability, backed by separate chaining. |
| `MyTree<K,V>` | Extends `MyMap`, shared by BST, balanced tree and B-tree. |

**Why `get`/`remove` return `null` on a miss instead of throwing.** Lookups are
a hot path in the dispatch engine (case-by-reference, facility-by-ID).
Forcing every miss through an exception there would be awkward for callers
that expect misses to be routine — unlike `MyGraph.weightOf`, where a missing
edge genuinely is exceptional. Since `null` means "not found," `null` keys
(`MyMap`) and `null` elements (`MySet`) are both rejected outright with
`IllegalArgumentException`, so a stored `null` is never ambiguous with a miss.

**Why `add`/`remove` on `MySet` return `boolean`.** Directly serves the
duplicate-detection-on-load use case: a caller can tell inline whether it
just hit a duplicate, without a separate `contains` check first. `MySet` also
has `clear()`, because BFS/DFS need a fresh, empty visited-set on every
traversal run rather than reallocating a new `MySet` on every call.

**Why the hash table exposes its internals.** `collisionCount()`,
`loadFactor()`, `longestBucket()` and `resizeCount()` are not diagnostics — they
are the Week 4 load-factor experiment. `resizeCount()` in particular is what
lets the report explain the timing spikes in the graph rather than merely
showing them. All four exist from the first commit for that reason, alongside
`capacity()`, which lets a test confirm the table actually started at the
seeded `INITIAL_TABLE_SIZE` and grew as expected rather than back-deriving
capacity from `loadFactor()` and `size()`. Implementations are expected to
widen `Instrumented.resetCounters()` to also zero `collisionCount()` and
`resizeCount()` — the T021 harness resets counters before each of its three
timed repetitions, and without this widening, collision/resize counts from
one repetition would leak into the next one's measurement. This does not
change the `Instrumented` contract itself or affect any other structure that
implements it; it is behavior local to `MyHashTable`.

**Why one `MyTree` for three trees.** The BST-versus-balanced-tree experiment is
only a fair comparison if both run through the same interface and the same
harness. `height()` is the measurement (edge-count convention: `-1` for an
empty tree, `0` for a single node); `isBalanced()` doubles as a test oracle
after randomised insertion (trivially `true` for a B-tree, which is balanced
by construction). `rangeKeys()` — inclusive of both bounds, empty if the
range holds no keys or `from > to` — is the operation a hash table cannot
perform, and the reason the indexing engine keeps a tree as well as a table.
Ordering is pluggable via `comparator()` rather than requiring `K` to
implement `Comparable`, the same choice already made for `MyPriorityQueue`.

**Initial table size** is the first index-number-derived parameter and must come
from configuration, never a literal.

---

## T020 — Sub-team D: graphs

Lives in `edu.ug.nexusb.graphs`, not `edu.ug.nexusb.core` — like every other
sub-team's contracts, it is not part of the small set of types actually
shared across packages (see "Shared types" above, which *does* live in
`core`).

| Type | Notes |
|---|---|
| `Edge` | Immutable record. Rejects negative weights in its compact constructor, because Dijkstra's correctness argument depends on non-negative weights. |
| `PathResult` | Holds distances **and** the predecessor chain, so the console menu can display an actual route. `visitOrder()` supplies the Dijkstra trace table. |
| `MyGraph` | Implemented as adjacency list and adjacency matrix behind one interface. |
| `MyDisjointSet` | Union by rank plus path compression, both required. |

**Weight semantics.** Every edge weight is effective travel time in minutes as
computed by the `v_weighted_edge` view — base time × traffic weight × road
condition factor. One definition, so every algorithm optimises the same
quantity.

**Why `representationName()` exists.** It writes `ADJACENCY_LIST` or
`ADJACENCY_MATRIX` into `algorithm_run.structure_name`, which turns the
representation comparison into a database query. It also gives the sub-team a
free test oracle: both implementations must agree on every query for the same
input.

**Why `removeEdge` exists.** It models a road closed by flooding or roadworks,
which is what the BFS reachability demonstration needs.

**`maxDepth()` on the disjoint set** is evidence that path compression genuinely
flattens the structure — the measurement behind the required proof sketch.

---

## T021 — Sub-team E: algorithms and measurement

| Type | Notes |
|---|---|
| `Sorter<T>` | One interface, four implementations, one harness. |
| `Searcher<T>` | `requiresSortedInput()` makes binary search's precondition explicit and testable. |
| `BenchmarkResult` | Maps field for field onto the `algorithm_run` table. |
| `Benchmark` | The single timing path for all six experiments. |

**Why `Sorter` reports its own properties.** `isStable()`, `isInPlace()` and the
two complexity labels force each implementation to be honest about itself, and
they populate the comparison table in the report directly. Stability matters
here: cases of equal triage level must stay in request-time order, so an
unstable sort needs an explicit tie-break.

**Measurement rules baked into `Benchmark`.** Untimed warm-up iterations first
so the JIT has settled; `System.nanoTime()` and never `currentTimeMillis()`;
three repetitions recorded individually rather than pre-averaged so the report
can show variance; and fresh input generated between repetitions so a sort is
never handed an already-sorted list by accident. Six improvised stopwatches
would produce six sets of numbers that cannot legitimately share an axis.

---

## Change log

| Date | Change | Approved by |
|---|---|---|
| 30 Jul 2026 | Initial freeze — 25 types in `core` | Cobbinah Emmanuel |
| 6 Aug 2026 | T019 frozen — `MyMap`, `MySet`, `MyHashTable`, `MyTree` added in `edu.ug.nexusb.trees` | Cobbinah Emmanuel |
