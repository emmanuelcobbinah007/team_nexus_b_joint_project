# Hospital & Clinic Operations Optimizer

**Team Nexus B · Group 29 · DCIT 204/308 Joint Data Structures & Algorithms Project**
University of Ghana · 26 July – 22 August 2026

A service-operations platform for a Greater Accra health facility network: it triages
synthetic cases, dispatches ambulances and response teams over a weighted road graph,
indexes records, and reports on its own algorithmic performance.

Every data structure and algorithm in the core logic is **implemented from scratch**.
No `java.util` collections are used anywhere in `src/main/java/edu/ug/nexusb`
outside the `data` and `bench` packages.

---

## Quick start

```bash
git clone https://github.com/emmanuelcobbinah007/team_nexus_b_joint_project.git
cd team_nexus_b_joint_project
mvn clean test          # build and run the full test suite
mvn exec:java           # launch the console menu
```

Requires **Java 17+**, **Maven 3.8+**. The SQLite database file is generated —
it is not committed. Building it from scratch:

```bash
mvn exec:java -Dexec.args="--init-db"    # runs data/schema.sql then loads all CSVs
```

Expected row counts after loading: 50+ facilities, 100+ road links,
300+ cases, 30+ resources.

**Interactive web console** (routing, MST, dispatch, triage, indexing,
knapsack, sorting/searching — all backed by the real algorithm classes,
not a mock):

```bash
mvn compile exec:java -Dexec.mainClass=edu.ug.nexusb.web.ApiServer
```

Then open <http://localhost:8080/>. Auto-initializes the database on first
run, same as `--init-db`.

---

## What the system does

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
| Query admissions by time range | BST, balanced tree, B-tree | binary search |
| Undo the last decision | stack | — |

---

## Repository layout

```
docs/     specifications, logs, trace tables, proof sketches, report sections
data/     schema.sql and the four seed CSVs
results/  experiment output — written by the benchmark harness only, never by hand
src/main/java/edu/ug/nexusb/
  core/        FROZEN interfaces — changes need Technical Lead approval
  data/        Sub-team A — loader, DAO, entities, console menu
  linear/      Sub-team B — list, stack, queue, deque, heap
  trees/       Sub-team C — BST, balanced tree, B-tree, hash table, set/map
  graphs/      Sub-team D — graph, disjoint set, BFS/DFS/Dijkstra/MST
  algorithms/  Sub-team E — sorts, searches, greedy, dynamic programming
  bench/       Sub-team E — benchmark framework
  app/         triage and dispatch engine, wiring
src/test/java/edu/ug/nexusb/   mirrors the package structure exactly
```

**One package per sub-team.** Two people who never edit the same file never
have a merge conflict. `core/` is the only shared area, which is why it is frozen.

---

## Team

| Role | Member |
|---|---|
| Group Leader (outward-facing) | Irene Tetteh |
| Planning & Delivery Lead | Frederick Kankam |
| Technical Lead / Integrator | Cobbinah Emmanuel |

| Sub-team | Lead | Members |
|---|---|---|
| A — Data & Database | Victor Barnieh | Arhin Franca, Baah John Excellence |
| B — Linear Structures | Obeng Jessica | Arthur Philip Kofi, El Masri Bilal |
| C — Trees & Hashing | Cobbinah Emmanuel | Mensah-Dogbevi Princess, Dakwa Nana Kwabena |
| D — Graphs | Frederick Kankam | Kwetey Sylvester, Irene Tetteh |
| E — Sorting & Optimisation | Johnson Kuzagbr | Ajilogba Abdulmalik, Salami Oluwanifemi |

---

## Fixed dates

| Date | Milestone |
|---|---|
| Thu 30 Jul | **Interface freeze** — `core/` signatures locked |
| Wed 5 Aug | Integration checkpoint + balanced-tree decision point |
| Thu 13 Aug | **Feature freeze** — fixes, measurements and writing only |
| Fri 21 Aug | Mock oral defense — all 15 members |
| Sat 22 Aug | Submission, by afternoon |

---

## Data ethics

Facility and road data is **real**, compiled by team members from their own
knowledge of Greater Accra.

All case data is **fully synthetic**. There are no patient names, contact details,
identification numbers, diagnoses or clinical notes anywhere in this repository,
and the schema provides no column in which such data could be stored. Cases carry
a generated reference, an age band and a triage level only.

See [`docs/evidence_note.md`](docs/evidence_note.md).

---

## Contributing

Read [`CONTRIBUTING.md`](CONTRIBUTING.md) before your first push. In short:
branch as `feat/<subteam>/<thing>`, open a pull request, never merge your own
unreviewed work, and no code is "done" without tests for the normal case, the
boundary case and invalid input.

## AI assistance

AI tool use is permitted and must be declared. Every use is recorded with the
prompts in [`docs/ai_usage_log.md`](docs/ai_usage_log.md). Every member must be
able to explain and modify any code submitted under their name.
