# 10. Responsible Algorithm Selection, Limitations & Conclusion

## Why these algorithms, for this problem

Every algorithm choice in this project was made against a specific
operational consequence, not picked for textbook convenience:

- **Dijkstra over a simpler "nearest facility" heuristic** for routing,
  because ambulance dispatch decisions have a real cost attached to being
  wrong — `O((V+E) log V)` is cheap enough at this dataset's scale (117
  facilities, 217 roads) that there is no reason to trade correctness for
  speed here.
- **A binary heap priority queue for triage, not FCFS**, because arrival
  order and clinical urgency are different things and conflating them is
  itself a correctness bug for a triage system, not just a performance
  one. T072's benchmark (see [08_performance_analysis.md](08_performance_analysis.md))
  makes the trade-off this choice makes explicit and measurable rather
  than assumed: priority mode makes *raw average wait worse*, and that is
  the correct, intended behavior — it is only an improvement once wait is
  weighted by severity, which is the metric triage is actually meant to
  optimize.
- **Greedy nearest-first dispatch was deliberately not trusted as the
  final answer.** [`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md)
  is not a hedge — it is the point: `runGreedyDispatch` and
  `runOptimalDispatch` are two real, callable methods, and the system
  reports both, because a single-factor greedy heuristic (distance alone)
  is fast but demonstrably not fair to higher-triage cases further away.
  Presenting only the greedy result would have been the irresponsible
  choice, since it silently privileges *proximity* to the dispatch station
  over *clinical urgency* without saying so.
- **Kruskal/Prim (MST) for resupply-route planning, not for patient
  routing** — an MST answers "what's the minimum-cost network that keeps
  everything connected," which is the right question for planning fixed
  resupply infrastructure and the wrong one for a single urgent trip
  (where Dijkstra's shortest path is what matters). Using the right
  algorithm for the right sub-problem, rather than one algorithm
  everywhere, is itself a responsible-selection decision.
- **A B-tree, not a Red-Black tree, backs `IndexingEngine`'s time-range
  index**, even though the RB tree was built and proven first — a B-tree's
  wider branching factor means fewer node hops per lookup on the
  dataset's actual access pattern (range queries over `requested_at`),
  and this was a deliberate choice made *after* both structures existed,
  not a default.

## Trade-offs made deliberately

- **No `java.util` collections in core logic** meant reimplementing
  everything from arrays up — hash tables, priority queues, trees,
  disjoint sets — which cost real time but is the entire point of the
  brief. `data/` and `bench/` are exempted, since file parsing and
  benchmark scaffolding aren't the algorithmic content being assessed.
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
  consistent format and SQLite's own `MIN`/`MAX` under its default
  `BINARY` collation agree with Java's `String.compareTo` on the same
  data — but it's not a general solution, and a real date type would be
  the correct fix outside this project's timeline.
- **The `assignment` table is defined but not yet written to by any code
  path** — see [09_database_integration_evidence.md](09_database_integration_evidence.md)
  for the full account. The dispatch/audit path commits an `audit_event`
  but doesn't yet persist the resulting resource-to-case pairing as its
  own row.
- **`Prim.java` had a genuine `O(V)` performance bug** (a linear scan
  inside what was meant to be an `O(E log V)` algorithm) that went
  undetected until the T073 benchmark specifically compared it against
  Kruskal on the same graphs — a reminder that a correct algorithm's own
  unit tests (which only check *what* it returns) don't catch a
  performance regression in *how* it gets there; only a benchmark that
  compares against a peer algorithm on the same input does. Found and
  fixed; see [08_performance_analysis.md](08_performance_analysis.md).
- **`TriageComparison`'s FCFS mode originally trusted caller-provided list
  order** instead of actually sorting by arrival time — correct only by
  coincidence for the one hardcoded demo case list. Found while making the
  method testable (T054) and fixed.

## What this suggests for future work

The Prim and FCFS findings above share a pattern worth naming: both bugs
were latent in code whose own unit tests passed, and both were only
caught by building something that exercised the code under conditions its
original author hadn't tested — a comparative benchmark in one case, a
non-trivial input order in the other. A useful next step beyond this
project's scope would be property-based or comparative testing (checking
one implementation's output against an independent second implementation
of the same problem, the way `KruskalTest`/`PrimTest` already cross-check
each other's MST weight) applied more systematically across the codebase.

Two other concrete follow-ups, both already named where they came up:
finishing the `assignment` write path so dispatch history is queryable
after the fact (above), and rerunning T072 with a severity-weighted wait
metric instead of a flat average, to directly confirm triage-priority
wins under the metric it was actually designed for (see
[08_performance_analysis.md](08_performance_analysis.md)).

## Conclusion

This project set out to answer six operational questions for a Greater
Accra health-facility network — which case to serve next, the fastest
route between facilities, which facilities are reachable under closed
roads, which subset fits a resource constraint, how alternative
structures perform as data grows, and how to persist and reload records —
using only data structures and algorithms built from scratch, against a
real (if partly synthetic) SQLite-backed dataset. Every one of those six
questions has a working, tested implementation reachable from either the
console (`ExaminerConsole`) or the live web console (`web.ApiServer` +
its browser frontend), backed by 459 passing tests, 8 trace tables, 4
proof sketches, 2 counterexamples, and 6 real benchmark experiments
against the brief's own minimum input ranges.

The most valuable outcome was not any single algorithm but the discipline
the "verify by running it" standard imposed throughout: three genuine
bugs (`KnapsackDP`'s zero-weight base case, `Prim`'s `O(V)` bottleneck,
`TriageComparison`'s FCFS ordering) were found this way, not by code
review alone, and each is now a permanent regression test rather than a
one-time fix. With more time, the team would prioritize the `assignment`
write path and a severity-weighted triage metric above — both are
extensions of what already exists, not redesigns, which is itself a sign
the underlying architecture (one interface-bound package per sub-team,
built against a frozen contract) held up under real use.
