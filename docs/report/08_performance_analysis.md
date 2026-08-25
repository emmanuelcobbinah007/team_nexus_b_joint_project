# 8. Performance Analysis

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
longest-bucket length, and average `get()` time at each step. Sweeps
`N = 100` to `20,000`, matching the brief's Section 9 minimum for this
experiment ("100 to 20,000 keys with different table sizes") — the
auto-resize policy means `Capacity` genuinely differs across the range
(223 at N=100, 29,311 by N=12,000+), rather than needing separate runs
pinned to fixed table sizes.

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

| N | Load factor | Capacity | Collisions | Longest bucket | Resizes | Avg. `get()` time |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 0.448 | 223 | 33 | 3 | 2 | 50.6 µs |
| 300 | 0.668 | 449 | 127 | 5 | 3 | 47.4 µs |
| 800 | 0.439 | 1,823 | 342 | 4 | 5 | 54.9 µs |
| 1,500 | 0.410 | 3,659 | 638 | 5 | 6 | 43.6 µs |
| 3,000 | 0.410 | 7,321 | 1,278 | 5 | 7 | 44.2 µs |
| 5,000 | 0.683 | 7,321 | 2,146 | 6 | 7 | 40.4 µs |
| 8,000 | 0.546 | 14,653 | 3,302 | 6 | 8 | 40.4 µs |
| 12,000 | 0.409 | 29,311 | 5,042 | 5 | 9 | 36.6 µs |
| 16,000 | 0.546 | 29,311 | 6,623 | 5 | 9 | 45.7 µs |
| 20,000 | 0.682 | 29,311 | 8,490 | 6 | 9 | 36.6 µs |

Collision count climbs steadily with N (and within each resize interval,
with load factor) — expected under simple uniform hashing, where
collisions are a probabilistic certainty as more keys compete for the same
buckets. What matters operationally is that **`longestBucket()` and
`get()` time both stay bounded** (longest bucket never exceeds 6 across a
200× growth in N, from 100 keys all the way to the brief's 20,000-key
maximum; `get()` time stays in the 36-55 µs range throughout, with no
upward trend) — direct empirical evidence that the resize policy is doing
its job: raw collisions accumulate, but no single bucket is ever allowed
to become a long chain that would degrade lookups toward `O(n)`. The
saw-tooth pattern in load factor (0.41 right after a resize, climbing back
toward 0.68 before the next one) is the resize policy working exactly as
designed — `ChainedHashTable` resizes once load factor crosses its
threshold, so load factor is bounded, not monotonically increasing.

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

## Heap priority dispatch — insert vs. extract time vs. N

**Status: done.** Run via `edu.ug.nexusb.bench.HeapExperiments`, closing
the brief's Section 9 requirement for a dedicated heap insert/extract
timing experiment (distinct from T072's triage-*outcome* comparison
below): builds a `BinaryHeapPriorityQueue` over `N` random (seeded) longs
and times two operations separately — inserting one at a time (`N` calls
to `insert()`) versus bulk `heapify()` followed by draining the whole
heap via `extractTop()`. Sweeps `N = 100` to `20,000`, the brief's stated
range for this experiment.

- Data: [`results/csv/heap_experiments.csv`](../../results/csv/heap_experiments.csv)
- Chart: [`results/graphs/heap_insert_extract.svg`](../../results/graphs/heap_insert_extract.svg)

| N | Insert (total, `N` calls) | Extract (total, `N` calls, after `heapify`) |
|---:|---:|---:|
| 100 | 184.5 µs | 173.0 µs |
| 500 | 198.3 µs | 720.8 µs |
| 1,000 | 290.6 µs | 610.3 µs |
| 5,000 | 1.09 ms | 3.15 ms |
| 10,000 | 1.15 ms | 5.88 ms |
| 15,000 | 1.00 ms | 7.39 ms |
| 20,000 | 1.18 ms | 9.53 ms |

Both columns are the total cost of `N` operations, each individually
`O(log N)` — so the expected shape is `O(N log N)` total, and that is what
both series show: growth is clearly super-linear (20,000 is a 200× growth
in N but insert time grows only ~6× and extract ~55×, so neither is
tracking N directly) but far below the `O(N²)` a broken heap invariant
would produce. **Extract is consistently more expensive than insert at
the same N once N is past a few hundred**, which is the correct asymmetry
for this implementation:
`insert()` does at most one root-ward sift-up path per call, while
`extractTop()` swaps the last element to the root and sifts it all the
way back down — a full-height sift on every single call, against a heap
that is (until near the very end of the drain) close to its maximum size.
`heapify()` itself is `O(N)` (Floyd's bottom-up build) and is not the
component being isolated here — the timed region for `Extract` is the
drain loop only, which is where the `O(N log N)` cost actually lives.

## T072 — triage-priority vs. first-come-first-served outcomes

**Status: done.** Run via `edu.ug.nexusb.bench.TriageExperiments`:
generates deterministic (seeded) synthetic case lists of growing size
(random arrival times, random severity 1-4) and runs both dispatch modes
via `TriageComparison.compare()` (fixed for T054's FCFS-ordering bug — see
[07_correctness_evidence.md](07_correctness_evidence.md)) to see
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
