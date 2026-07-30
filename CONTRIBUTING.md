# Contributing

Fifteen people, four weeks, one branch that must always build. These rules exist
to keep that true. Read once, then work normally.

---

## 1. Before you write code

1. Find your task in the **Master Task Tracker** and note the Task ID (`T0xx`).
2. Read the same task on the **Task Guide** sheet — it says what "done" means
   and who is waiting on you.
3. Check the task's **Depends On** column. If those are not `Done`, say so in the
   group chat rather than working around them.

## 2. Branches

| Branch | Purpose |
|---|---|
| `main` | Always builds, always green. Protected. |
| `feat/<subteam>/<thing>` | New work — e.g. `feat/graphs/dijkstra` |
| `fix/<subteam>/<thing>` | Corrections, and the only kind of branch after the feature freeze |

Sub-teams are `data`, `linear`, `trees`, `graphs`, `algorithms`, `bench`, `app`.

**Merge into `main` at least twice a week even if the feature is incomplete.**
Small merges are boring; week-long branches turn integration into a rescue
operation.

## 3. Commits

```
<area>: <what changed>

graphs: add Dijkstra with predecessor table
trees:  fix hash table resize losing entries on rehash
docs:   add Kruskal proof sketch
```

Keep the subject under about 70 characters and in the present tense. The weekly
dev log is reconstructed from `git log`, so a clear history is not cosmetic.

## 4. Pull requests

Every change reaches `main` through a pull request. The template asks for four
things; all four must be genuinely true, not ticked out of habit.

- **Nobody merges their own unreviewed work.** The Technical Lead
  (Cobbinah) reviews every pull request. His own work is reviewed by the
  Planning & Delivery Lead (Frederick), who also covers reviews whenever the
  Technical Lead is unavailable for more than 24 hours.
- `CODEOWNERS` assigns the right reviewer automatically. Do not reassign it.
- Keep pull requests small. One structure or one algorithm per pull request
  reviews in ten minutes; a whole module takes an hour and gets rubber-stamped.

## 5. Definition of done

A change is not done until **all** of these hold:

- [ ] Tests exist for the **normal case**, the **boundary case** and **invalid input**
- [ ] `mvn test` passes locally
- [ ] No `java.util` collection is imported anywhere under `src/main` outside
      `data/` and `bench/`
- [ ] Public types and methods carry Javadoc
- [ ] Any trace table or proof sketch the task requires is written and committed
- [ ] The Task ID appears in the pull request description

The boundary and invalid-input cases are where marks are lost. Empty structure,
single element, full capacity, duplicate key, null argument, unsorted input to
binary search, disconnected graph — write those tests first, they are quick.

## 6. The two freezes

**Interface freeze — Thursday 30 July.** After this date any change to a
signature in `core/` requires the Technical Lead's written approval in the group
chat. This is what lets five sub-teams build simultaneously.

**Feature freeze — Thursday 13 August.** After this date only fixes,
measurements and documentation are merged. Pull requests adding features will be
closed. This protects the marks attached to testing, experiments and the report.

## 7. What never gets committed

- `target/`, `*.class`, `.idea/`, `.vscode/`
- `*.db` — the database is **generated** from `data/schema.sql` plus the CSVs.
  Committing it means fifteen conflicting binaries.
- Anything in `results/` written by hand. Only the benchmark harness writes there.
- Any real patient data. It should not exist; if you are ever offered some,
  decline and tell the Group Leader.

## 8. Getting unstuck

Post in the group chat the day you are blocked, not the day before the deadline.
Mark the task `Blocked` in the tracker at the same time. Nobody is judged for
being stuck; the two-missed-pings rule exists for silence, not for difficulty.
