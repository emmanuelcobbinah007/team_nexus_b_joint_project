# Proof: Kruskal's Algorithm

Correctness and complexity argument for the implementation in
`edu.ug.nexusb.graphs.Kruskal`.

## Correctness

**Cut property.** For a cut `(S, V\S)` of a connected weighted graph, if
edge `e` is a minimum-weight edge crossing the cut, then some MST of the
graph contains `e`.

*Proof (exchange argument).* Let `T` be any MST. If `e ∈ T`, done. Otherwise,
adding `e` to `T` creates exactly one cycle (a spanning tree plus any
non-tree edge always does). That cycle has an endpoint in `S` and an
endpoint in `V\S` — since it's a cycle, it must cross `(S, V\S)` again at
some other edge `f ∈ T`, `f ≠ e`. Because `e` is a minimum-weight edge
crossing the cut, `weight(e) ≤ weight(f)`. Swap: `T' = T − f + e`. `T'` is
still a spanning tree (removing `f` breaks the cycle `e` created, and
`T` was connected before, so `T'` is too, with the same edge count), and
`weight(T') = weight(T) − weight(f) + weight(e) ≤ weight(T)`. Since `T` was
already minimum, `weight(T') = weight(T)`, so `T'` is also an MST — and it
contains `e`. ∎

**Kruskal always picks a safe edge.** Let `A_k` be the set of edges
`Kruskal.run` has accepted after `k` iterations of its main loop
(`Kruskal.java:51-58`) — a forest, since an edge is only accepted when
`dsu.union(edge.src, edge.dest)` returns `true`, i.e. its endpoints are in
different components, so acceptance can never close a cycle. Claim: `A_k`
is always a subset of some MST.

*Proof, by induction on k.*
- **Base case** (`k = 0`): the empty set is a subset of every MST.
- **Inductive step:** assume `A_k ⊆` some MST. Let `e = (u, v)` be the next
  edge Kruskal accepts, and let `S` be `u`'s connected component in the
  forest `(V, A_k)` at that moment (`v ∉ S`, since acceptance requires
  `u` and `v` in different components). No edge of `A_k` crosses `(S, V\S)`
  by construction — `S` is one whole component of that forest — so this
  cut *respects* `A_k`.

  `e` is a minimum-weight edge crossing `(S, V\S)`: suppose some edge `f`
  with `weight(f) < weight(e)` also crosses it. Because edges are processed
  in non-decreasing weight order (`mergeSort(sorted, 0, sorted.length - 1)`
  at `Kruskal.java:44`), `f` was considered before `e`. Union-find
  components only ever merge, never split, so "same component when `f` was
  considered" implies "same component now" — meaning if `f`'s endpoints
  were in the *same* component back then, they're still in the same
  component now, contradicting that `f` crosses `(S, V\S)` right now. So
  `f`'s endpoints must have been in *different* components when
  considered — but then Kruskal would have accepted `f` into `A_k`, which
  would put `f ∈ A_k` crossing `(S, V\S)`, contradicting that the cut
  respects `A_k`. Either way, no such `f` exists, so `e` is a light edge
  for a cut that respects `A_k`.

  By the cut property (applied with `A_k` fixed and this specific cut),
  `e` is safe: `A_k ∪ {e} ⊆` some MST. ∎

**Termination.** The loop stops once `edgesUsed == numVertices - 1`
(`mst.length`, `Kruskal.java:51`) or the edge list is exhausted. For a
connected graph, Kruskal ends with exactly `|V| - 1` edges that are, by the
induction above, a subset of some MST — and since every MST also has
exactly `|V| - 1` edges, a same-size subset of an MST *is* that MST. For a
disconnected graph, `dsu.setCount() > 1` at the end (`Kruskal.java:64`),
`isSpanning` is reported `false`, and the same argument applies
independently within each connected component, so the result is a maximum
spanning forest — the optimal spanning tree of each component.

This isn't just asserted: `KruskalTest.classicSixVertexGraph_producesCorrectMstWeight`
checks the actual minimum weight against a hand-computed value,
`duplicateAndParallelEdges_cheaperParallelEdgeIsChosen` confirms the
algorithm doesn't just take the first edge between two vertices but the
lightest one, and `disconnectedGraph_isNotSpanning_andReturnsPartialForest`
exercises the non-spanning case the induction above covers separately.

## Complexity

Let `V` = `numVertices`, `E` = `edges.length`.

- **Sorting:** copying the input array is `O(E)`
  (`System.arraycopy`, `Kruskal.java:43`); `mergeSort` is `O(E log E)`.
- **Disjoint-set init:** `new DisjointSet(numVertices)` allocates two
  length-`V` arrays and initializes each element as its own root — `O(V)`.
- **Main loop:** `O(E)` iterations, each doing one `union` (which is one
  or two `find` calls). `DisjointSet` implements both path compression
  (`find`, `DisjointSet.java:24-38`) and union by rank
  (`union`, `DisjointSet.java:41-64`) — both independently verified by
  execution, not just present in the code
  (`testPathCompressionFlattensTree`, `testUnionByRankKeepsTreeShallow`
  in `DisjointSetTest`). With both in place, the amortized cost per
  `find`/`union` is `O(α(V))` (Tarjan), where `α` is the inverse Ackermann
  function — effectively constant for any `V` this project will ever
  construct.
- **Total:** `O(E log E + V + E · α(V))`. Since `α(V) = O(log E)` for any
  realistic `V`, this simplifies to `O(E log E)` — sorting dominates,
  exactly as the standard result for Kruskal's algorithm states.
- **Space:** `O(V + E)` — the sorted edge-array copy (`O(E)`), the two
  disjoint-set arrays (`O(V)`), and the output `mst`/`trimmed` arrays
  (`O(V)`).
