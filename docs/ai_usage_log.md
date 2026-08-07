# AI Usage Log

Record of AI tool usage on this project: tool, date, what it was used for, and
the prompt (or a link/summary of it). Log every significant use — this is
evidence for the report, not just a courtesy.

| Date | Tool | Used for | Prompt (summary) |
|---|---|---|---|
| 2026-07-30 | Claude Code | Repo scaffolding | Set up top-level project structure (docs/data/results/src layout, pom.xml, CONTRIBUTING, CODEOWNERS) |
| 2026-08-04 | Claude Code | T005 parameters + T020 interface draft | Derived the three index-number parameters from the roster; drafted `MyGraph`/`MyDisjointSet`/`PathResult`/`Edge` and the shared `core` types (`Instrumented`, `MyIterator`/`MyIterable`, `StructureException`) against `docs/interfaces.md` |
| 2026-08-04 | Claude Code | Repo structure cleanup | Relocated Sub-team B's T018 structures from `subteamB/` into `src/main/java/edu/ug/nexusb/linear`; fixed a `java.util.Comparator` violation and a duplicate `MyIterator`; added the CI banned-collections check, PR template, and a simplified `CODEOWNERS` |
| 2026-08-04 | Claude Code | Package placement fix | Moved `MyGraph`/`MyDisjointSet`/`PathResult`/`Edge` out of `core/` into `graphs/`, matching the "core is shared-only" rule already stated in `core/package-info.java` |
| 2026-08-04 | Claude Code | Branch model + CLAUDE.md | Consolidated the repo onto six branches (`main` + one per sub-team) per direction from Frederick; wrote `CLAUDE.md` so a Claude Code session in any teammate's clone starts from the same project context |
| 2026-08-07 | Claude Code | Week 1 structure/completeness audit | Audited `main` against the required package layout and the Week 1 exit criteria; reported the `interfaces/` package misplacement, the `locations.csv`/`roads.csv` facility-ID join gap, the empty `Sorter.java`, and unfilled doc templates |
| 2026-08-07 | Claude Code | T021 relocation, `Sorter.java`, `FacilityDisjointSet` | Moved `Sorter`/`Searcher`/`Benchmark` out of the non-standard `interfaces/` package into `algorithms/` and `bench/`; fixed a second `java.util.Comparator` violation and widened the CI check to catch it; wrote `Sorter.java` (previously a tracked but empty file) and `requiresSortedInput()`; added `FacilityDisjointSet` as a working `MyDisjointSet` implementation with 13 tests |
