# Trace: Kruskal's Algorithm

Worked trace of `edu.ug.nexusb.graphs.Kruskal` (T049) on a small example
graph, step by step (sorted edge list, union-find decision, MST weight and
component count at each step) — the connectivity trace the brief requires
for the disjoint set. This is not hand-calculated in isolation — it was
captured by instrumenting a copy of the actual algorithm's logic (same sort
order, same `DisjointSet`, same accept/reject rule) and transcribing its
real output, cross-checked against the real `Kruskal.run()` call on the
same input (agrees: 5 edges, weight 14, spanning), and against
`KruskalTest`'s `classicSixVertexGraph_producesCorrectMstWeight` assertion
on the identical graph, which passes.

## Input graph

The same six-vertex, eight-edge graph already used as the worked example in
`Kruskal.java`'s own `main()` method, reused here rather than inventing a
second one so the two stay cross-checkable against each other:

Edge list: `0–1 (4)`, `0–2 (4)`, `1–2 (2)`, `2–3 (3)`, `2–5 (2)`, `2–4 (4)`,
`3–4 (3)`, `5–4 (3)`. Undirected (Kruskal's `Edge` has no direction), 6
vertices numbered `0`–`5`. Vertex `2` is the hub — it has an edge to every
other vertex except `1`'s pair partner — which is what makes edges `0–2`
and `2–4` land as rejected cycles rather than accepted tree edges.

## Step-by-step trace

Edges sorted ascending by weight first (ties keep their original array
position — a stable sort, which is why `1–2` and `2–5` — both weight 2 —
and the three weight-3 edges appear in the same relative order as the input):

```
1–2 (2), 2–5 (2), 2–3 (3), 3–4 (3), 5–4 (3), 0–1 (4), 0–2 (4), 2–4 (4)
```

| Step | Edge | `find()` before union | Decision | MST edges so far | Weight so far | Components |
|---|---|---|---|---|---|---|
| 1 | 1–2 (2) | find(1)=1, find(2)=2 | **ACCEPT** | 1 | 2 | 5 |
| 2 | 2–5 (2) | find(2)=1, find(5)=5 | **ACCEPT** | 2 | 4 | 4 |
| 3 | 2–3 (3) | find(2)=1, find(3)=3 | **ACCEPT** | 3 | 7 | 3 |
| 4 | 3–4 (3) | find(3)=1, find(4)=4 | **ACCEPT** | 4 | 10 | 2 |
| 5 | 5–4 (3) | find(5)=1, find(4)=1 | **REJECT** — same set, would close a cycle | 4 | 10 | 2 |
| 6 | 0–1 (4) | find(0)=0, find(1)=1 | **ACCEPT** | 5 | 14 | 1 |
| 7 | 0–2 (4) | find(0)=1, find(2)=1 | **REJECT** — same set | 5 | 14 | 1 |
| 8 | 2–4 (4) | find(2)=1, find(4)=1 | **REJECT** — same set | 5 | 14 | 1 |

The loop stops accepting once 5 edges are in (== `numVertices - 1`), so steps
7–8 are shown for completeness but `Kruskal.run()` itself exits the loop
after step 6 in practice (its loop condition is `edgesUsed < mst.length`).

Step 5 is the connectivity trace the disjoint set proof sketch (T063) will
point to: `5` and `4` both resolve to root `1` *before* the edge is even
considered — the union-find correctly detects that connecting them would
close a cycle (`1–2–5` and `1–2–3–4` are already one component), without
ever having to walk the graph itself to check.

## Result

Final minimum spanning tree:

| Edge | Weight |
|---|---|
| 1–2 | 2 |
| 2–5 | 2 |
| 2–3 | 3 |
| 3–4 | 3 |
| 0–1 | 4 |

**Total weight: 14. Spanning: true** (all 6 vertices end in one component).

Cross-checked by hand: the rejected edges (`5–4`, `0–2`, `2–4`, all weight 3
or 4) are exactly the ones that would have connected two vertices already in
the same component — no edge is rejected for any other reason, and no
lower-weight edge is skipped in favour of a higher-weight one. `Kruskal.run()`
called directly on this same input returns the identical result: 5 edges,
weight 14, spanning.
