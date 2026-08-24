# 9. Discussion, Limitations & Future Work

## Trade-offs made deliberately

- **No `java.util` collections in core logic** meant reimplementing
  everything from arrays up — hash tables, priority queues, trees, disjoint
  sets — which cost real time but is the entire point of the brief. `data/`
  and `bench/` are exempted, since file parsing and benchmark scaffolding
  aren't the algorithmic content being assessed.
- **Index-number-derived parameters** (hash table size, generation seed,
  condition weight) trade a small amount of arbitrariness for
  reproducibility that's provably specific to this team's roster — see
  [`parameters.md`](../parameters.md).
- **One package per sub-team** traded some cross-team code reuse
  opportunities for near-zero merge conflicts on core logic — worth it for
  a 15-person team working in parallel under a hard deadline.

## Known limitations, named rather than hidden

- **`IndexingEngine`'s time-range index compares `requested_at` as plain
  lexicographic text**, not a real calendar-aware range query. This works
  correctly for this dataset because every timestamp is written in a
  consistent format and SQLite's own `MIN`/`MAX` under its default `BINARY`
  collation agree with Java's `String.compareTo` on the same data — but
  it's not a general solution, and a real date type would be the correct
  fix outside this project's timeline.
- **`Prim.java` had a genuine `O(V)` performance bug** (a linear scan
  inside what was meant to be an `O(E log V)` algorithm) that went
  undetected until the T073 benchmark specifically compared it against
  Kruskal on the same graphs — a reminder that a correct algorithm's own
  unit tests (which only check *what* it returns) don't catch a
  performance regression in *how* it gets there; only a benchmark that
  compares against a peer algorithm on the same input does. Found and
  fixed; see [08_benchmarks_and_results.md](08_benchmarks_and_results.md).
- **`TriageComparison`'s FCFS mode originally trusted caller-provided list
  order** instead of actually sorting by arrival time — correct only by
  coincidence for the one hardcoded demo case list. Found while making the
  method testable (T054) and fixed.
- **Three of the four Week 4 benchmark experiments** (T070 hash table load
  factor, T071 BST vs. balanced tree, T072 triage-priority vs. FCFS) were
  still outstanding as of this writing — see
  [08_benchmarks_and_results.md](08_benchmarks_and_results.md) for what's
  actually measured versus what's still pending.

## What this suggests for future work

The Prim and FCFS findings above share a pattern worth naming: both bugs
were latent in code whose own unit tests passed, and both were only caught
by building something that exercised the code under conditions its
original author hadn't tested — a comparative benchmark in one case, a
non-trivial input order in the other. A useful next step beyond this
project's scope would be property-based or comparative testing (checking
one implementation's output against an independent second implementation
of the same problem, the way `KruskalTest`/`PrimTest` already cross-check
each other's MST weight) applied more systematically across the codebase.
