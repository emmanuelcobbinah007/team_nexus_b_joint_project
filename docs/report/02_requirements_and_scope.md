# 2. Requirements & Scope

## Functional capabilities

| Capability | Structures used | Algorithms used |
|---|---|---|
| Triage incoming cases | binary heap | heap-based priority dispatch |
| Baseline comparison policy | queue, circular queue | first-come-first-served |
| Route an ambulance | graph, binary heap | Dijkstra |
| Reachability when roads close | graph, queue | BFS |
| Detect referral loops | graph, stack | DFS + cycle detection |
| Plan the resupply backbone | graph, disjoint set, heap | Kruskal, Prim |
| Assign nearest available unit | heap | greedy (+ documented counterexample) |
| Allocate a limited shift budget | dynamic array | dynamic programming (knapsack) |
| Look up a case by reference | hash table | hashing |
| Query admissions by time range | BST, balanced tree, B-tree | binary search / range query |
| Undo the last decision | stack | — |

## Structural requirement

Every structure and algorithm implemented from scratch (see
[01_introduction.md](01_introduction.md)); every one tested for the
normal case, a boundary case, and invalid input — not just the happy path,
since that is explicitly where the brief says marks are actually lost.

## Out of scope

- Real patient data of any kind — the schema has no column that could hold
  it (see [03_data_and_assumptions.md](03_data_and_assumptions.md)).
- A production-grade persistence layer — `nexus.db` is a local, gitignored
  SQLite file regenerated from `data/*.csv` on demand, not a managed
  database.
- A UI beyond the console menu (`ExaminerConsole`) — this is a DSA
  demonstration project, not a deployable product.

## Ownership boundaries

One package per sub-team (`README.md`'s repository layout table), so two
people never edit the same file and never hit a merge conflict on core
logic. `core/` is the only shared package, frozen after interface freeze
(30 Jul) and changeable only with the Technical Lead's sign-off.
