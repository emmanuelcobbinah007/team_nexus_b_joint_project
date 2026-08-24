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
