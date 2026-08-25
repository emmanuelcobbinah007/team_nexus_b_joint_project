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
