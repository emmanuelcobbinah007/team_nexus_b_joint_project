# Team Nexus B — Hospital & Clinic Operations Optimizer

DCIT 204/308 Joint DSA Project, Group 29. Java 17 + Maven + SQLite. Full
brief and conventions: README.md, CONTRIBUTING.md, docs/interfaces.md.

## The one rule that matters

**Edit only your own sub-team's package (plus its mirrored test package).**
Two people who never touch the same file never get a merge conflict.

| Package (`src/{main,test}/java/edu/ug/nexusb/...`) | Sub-team | Branch |
|---|---|---|
| `core/` | Shared, frozen — changes need the Technical Lead's sign-off in the group chat | — |
| `data/` | A — loader, DAO, entities, console menu | `sub-team-a` |
| `linear/` | B — list, stack, queue, deque, heap | `sub-team-b` |
| `trees/` | C — BST, balanced tree, B-tree, hash table, set/map | `sub-team-c` |
| `graphs/` | D — graph, disjoint set, BFS/DFS/Dijkstra/Prim/Kruskal | `sub-team-d` |
| `algorithms/`, `bench/` | E — sorts, searches, greedy, DP, benchmark framework | `sub-team-e` |
| `app/` | shared wiring (triage/dispatch engine, console menu) | coordinate in chat |

`core/` holds ONLY types genuinely shared across packages (Instrumented,
MyComparator, MyIterator/MyIterable, the StructureException hierarchy). If
you're about to add a type that's specific to your own module, it almost
certainly belongs in your own package, not `core/`.

## Branches — exactly six, no more

`main`, `sub-team-a`, `sub-team-b`, `sub-team-c`, `sub-team-d`, `sub-team-e`.
No `feat/...`, `fix/...`, or scratch branches — work directly on your own
sub-team branch and open a PR into `main` at least twice a week, even if the
feature is incomplete. If a Claude Code session (yours or a teammate's) is
about to create a new branch for a task, stop and use the matching
sub-team branch instead.

## Before writing code

1. Find your Task ID (`T0xx`) in the Master Task Tracker, read its row in the
   Task Guide sheet (what "done" means, who's waiting on you).
2. Read the matching section of `docs/interfaces.md` — it's the frozen
   contract you're building against, and the *why* behind each method.
3. Check `docs/parameters.md` for the three index-number-derived constants
   (hash table size, generation seed, condition/priority weight) — use them,
   don't hardcode magic numbers.

## Hard constraints

- **No `java.util` collections** (ArrayList, HashMap, PriorityQueue, Stack,
  LinkedList, ArrayDeque, TreeMap/TreeSet, Vector) anywhere under `src/main`
  except `data/` and `bench/`. CI (`.github/workflows/build.yml`) fails the
  build on a forgotten import — don't rely on it catching things, avoid the
  import in the first place.
- Every structure/algorithm needs tests for the **normal case, boundary
  case, and invalid input** — this is where marks are actually lost, not on
  the hard parts.
- Public methods carry Javadoc. Aim for `javac -Xlint:all` and
  `javadoc -Xdoclint:all` with zero warnings.
- If your task requires a trace table or proof sketch (see the Task Guide),
  it's not done without one.

## Commits and PRs

- Commit format: `<area>: <what changed>` (e.g. `graphs: add Dijkstra with
  predecessor table`), present tense, under ~70 chars.
- PRs use the template's four checkboxes and must name the Task ID — that's
  what ties commits back to individual contribution statements later.
- Nobody merges their own unreviewed PR into `main`; CODEOWNERS assigns the
  reviewer.

## Dates that don't move

Interface freeze: done (30 Jul). Feature freeze: **13 Aug** — after this,
fix/measure/write only. Mock defense: 21 Aug. Submission: **22 Aug**.
