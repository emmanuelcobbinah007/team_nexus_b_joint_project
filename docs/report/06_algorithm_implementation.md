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
