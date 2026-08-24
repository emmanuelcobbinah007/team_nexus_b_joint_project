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

**Status: not yet run** (Johnson Kuzagbr).

## T071 — BST vs. balanced tree on sorted input

**Status: not yet run** (Cobbinah Emmanuel).

## T072 — triage-priority vs. first-come-first-served outcomes

**Status: not yet run** (Obeng Jessica). Note: `TriageComparison.compare()`
(fixed for T054, see the FCFS-ordering bug note in that section) already
returns real average-wait numbers per mode and is ready to drive this
experiment once someone runs it across multiple case-list configurations.
