# NexusB Hospital Ops

A triage/dispatch engine for hospital operations, built from scratch on custom
data structures and algorithms (no external DSA libraries). The engine takes
patient cases, hospital facilities, road links, and resource availability, and
routes/prioritizes dispatch decisions.

This is a team project split across sub-teams, each owning one layer of the
stack under `src/main/java/edu/ug/nexusb/`. See [docs/interfaces.md](docs/interfaces.md)
for the contracts between layers.

## Project structure

- `core/` — frozen interfaces shared by every sub-team (changes require sign-off, see [CODEOWNERS](CODEOWNERS))
- `data/` — Sub-team A: loaders, DAOs, entities, console menu
- `linear/` — Sub-team B: list, stack, queue, deque, heap
- `trees/` — Sub-team C: BST, RB-tree, B-tree, hash, set/map
- `graphs/` — Sub-team D: graph, disjoint set, BFS/DFS/Dijkstra/MST
- `algorithms/` — Sub-team E: sorts, searches, greedy, DP
- `bench/` — Sub-team E: benchmark framework
- `app/` — triage/dispatch engine, wiring of all the above

## Build

Requires JDK 17+ and Maven.

```
mvn compile
```

## Test

```
mvn test
```

## Run

```
mvn exec:java
```

or build a runnable jar and run it directly:

```
mvn package
java -jar target/nexusb-hospital-ops-0.1.0-SNAPSHOT.jar
```

## Docs

- [docs/data_dictionary.md](docs/data_dictionary.md) — field-level definitions for everything in `data/`
- [docs/interfaces.md](docs/interfaces.md) — the frozen contracts in `core/`, human-readable
- [docs/evidence_note.md](docs/evidence_note.md) — evidence/assumptions behind the synthetic dataset
- [docs/dev_log.md](docs/dev_log.md) — weekly dev log
- [docs/ai_usage_log.md](docs/ai_usage_log.md) — AI tool + prompt usage log
- [docs/traces/](docs/traces/) — hand-worked algorithm traces
- [docs/proofs/](docs/proofs/) — correctness/complexity proofs
- [docs/report/](docs/report/) — final report, one file per section

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch/PR/commit conventions.
