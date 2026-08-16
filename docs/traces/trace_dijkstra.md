# Trace: Dijkstra's Algorithm

Worked trace of `edu.ug.nexusb.graphs.Dijkstra` (T046) on a small example
graph, step by step (priority queue state, distances, predecessors at each
iteration). This is not hand-calculated in isolation — it was captured by
running an instrumented copy of the actual algorithm and transcribing its
real output, then cross-checked against `DijkstraTest`'s assertions on the
same graph (`findsShortestDistancesOnAClassicExample`,
`reconstructsThePathViaThePredecessorChain`,
`decreaseKeyProducesTheShorterRouteFoundLater`), which pass under both
`AdjacencyListGraph` and `AdjacencyMatrixGraph`.

## Input graph

Five facilities, six directed edges (a CLRS-style textbook example, chosen
because it forces at least one `decreaseKey()` — B is discovered via the
direct A→B edge before the shorter A→C→B route is found):

```
      8
  A ----> B
  |       |
 2|      5|
  v       v
  C ----> D
  |  11   |
 2|      3|
  v       v
  B       E
```

Edge list: `A→B (8)`, `A→C (2)`, `C→B (2)`, `B→D (5)`, `D→E (3)`, `C→D (11)`.
Source vertex: `A`.

## Step-by-step trace

| Step | `extractTop()` | Relaxations performed | Queue after step |
|---|---|---|---|
| 1 | **A** (dist 0) | `A→B`: ∞ → 8, **insert**, pred(B)=A<br>`A→C`: ∞ → 2, **insert**, pred(C)=A | {B:8, C:2} |
| 2 | **C** (dist 2) | `C→B`: 8 → 4, **decreaseKey()**, pred(B)=C<br>`C→D`: ∞ → 13, **insert**, pred(D)=C | {B:4, D:13} |
| 3 | **B** (dist 4) | `B→D`: 13 → 9, **decreaseKey()**, pred(D)=B | {D:9} |
| 4 | **D** (dist 9) | `D→E`: ∞ → 12, **insert**, pred(E)=D | {E:12} |
| 5 | **E** (dist 12) | *(no outgoing edges)* | {} |

Two `decreaseKey()` calls happen in this trace (step 2 on B, step 3 on D) —
both are the scenario `MyPriorityQueue.decreaseKey()` exists for: a vertex
already sitting in the queue gets a shorter route found for it before it's
ever extracted, and the *same* heap entry object is mutated and re-sifted
rather than a second, stale entry being inserted alongside it.

## Result

Final shortest-path tree from A:

| Vertex | Distance | Predecessor | Path from A |
|---|---|---|---|
| A | 0 | — | A |
| B | 4 | C | A → C → B |
| C | 2 | A | A → C |
| D | 9 | B | A → C → B → D |
| E | 12 | D | A → C → B → D → E |

Cross-checked by hand: the direct `A→B` edge (weight 8) is correctly
**not** used for B's final distance — `A→C→B` (2+2=4) is shorter and wins,
which is exactly the relaxation this graph was built to force. Likewise
`A→C→D` (2+11=13) loses to `A→C→B→D` (2+2+5=9).

`visitOrder()` for this run is `[A, C, B, D, E]`, i.e. non-decreasing by
finalized distance (0, 2, 4, 9, 12) — asserted directly by
`visitOrderStartsAtSourceAndIsNonDecreasingByDistance` in `DijkstraTest`.
