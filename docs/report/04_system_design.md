# 4. System Design & Architecture

Full contract text: [`interfaces.md`](../interfaces.md). Summarized here.

## Interfaces-first architecture

Five sub-teams built simultaneously against one frozen set of contracts in
`edu.ug.nexusb.core` (locked 30 Jul; any change after that date needs the
Technical Lead's written sign-off). That's what let Sub-team D write
Dijkstra against `MyPriorityQueue` in Week 3 while Sub-team B was still
refining the heap implementation underneath it — the contract was settled,
so the two didn't need to be sequential.

Shared core types: `MyComparator<T>` (ordering, declared locally rather
than reusing `java.util.Comparator`), `MyIterator<T>`/`MyIterable<T>`
(fail-fast traversal), `Instrumented` (comparison/movement counters,
resettable — see below), and the `StructureException` hierarchy
(`EmptyStructureException`, `CapacityExceededException`,
`KeyNotFoundException`).

## Why every structure is `Instrumented`

Wall-clock timings vary across fifteen different laptops; comparison and
movement counts don't. `Instrumented` puts a cheap, resettable counter
(increment a `long`, nothing else) on nearly every structure specifically
so that when a Week 4 benchmark graph is noisy, the counter data is
independent, machine-agnostic evidence backing it up.

## Module layout

One package per sub-team — `data/` (A), `linear/` (B), `trees/` (C),
`graphs/` (D), `algorithms/`+`bench/` (E) — plus `core/` (frozen, shared)
and `app/` (triage/dispatch wiring, console menu). Two people never edit
the same file, so ownership boundaries alone prevent most merge conflicts;
`docs/interfaces.md` documents each package's specific contracts and the
cross-team dependencies worth protecting (e.g. `MyPriorityQueue
.decreaseKey()` exists solely for Dijkstra's benefit — nothing in
Sub-team B's own module calls it, but it must not be dropped as
"unused").

## Data flow

`data/*.csv` → `DBLoader` → `nexus.db` (SQLite) → `GraphBuilder` /
`IndexingEngine` / `TriageDispatchEngine` build in-memory structures from
the DB → `app.ExaminerConsole` demonstrates each capability against real
data end to end (T056).
