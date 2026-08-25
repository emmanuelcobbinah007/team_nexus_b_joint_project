# 12. References & Appendices

## Course materials

- DCIT 204/308 Joint DSA Project brief, Department of Computer Science,
  University of Ghana (2026) — the source of every requirement cross-
  referenced throughout this report (12-section structure, minimum test/
  trace/proof/experiment counts, the six operational questions in
  [02_problem_statement.md](02_problem_statement.md)).
- `Joint_DSA_Project_Checklist_Cover_Sheet.docx` — official cover sheet
  template, reproduced field-for-field in [`../cover_sheet.md`](../cover_sheet.md).
- Team Nexus B Master Task Tracker (Group 29) — the task/owner/deliverable
  ledger this report's Task IDs (T0xx) refer back to.

## Algorithms and data structures — textbook sources

The algorithms implemented in this project follow standard formulations;
no code was copied from any of these, they are cited as the source of the
correctness arguments in [`docs/proofs/`](../proofs/) and the complexity
claims in [06_algorithm_implementation.md](06_algorithm_implementation.md):

- Cormen, T. H., Leiserson, C. E., Rivest, R. L., & Stein, C. (2022).
  *Introduction to Algorithms* (4th ed.). MIT Press. — Dijkstra's
  algorithm, Kruskal's and Prim's MST algorithms, red-black tree
  invariants and rebalancing, B-trees, dynamic programming (0/1 knapsack),
  the loop-invariant proof method used in
  [`proof_insertion_sort_loop_invariant.md`](../proofs/proof_insertion_sort_loop_invariant.md).
- Sedgewick, R., & Wayne, K. (2011). *Algorithms* (4th ed.).
  Addison-Wesley. — hash table separate chaining and amortized resize
  analysis (`proof_hashing.md`), disjoint-set union-by-rank with path
  compression.

## Software and libraries

Every third-party dependency actually used, with what it's for and why it
doesn't conflict with the "no `java.util` collections in core logic"
constraint (none of these are collection implementations):

| Dependency | Version | Purpose | Scope |
|---|---|---|---|
| SQLite (via `org.xerial:sqlite-jdbc`) | 3.46.1.3 | Embedded relational database, `nexus.db` | `data/` |
| JUnit Jupiter | 5.10.2 | Test framework | `src/test` only |
| OpenCSV | 5.9 | CSV parsing for `DBLoader`'s source files | `data/` (exempted package) |
| `com.sun.net.httpserver.HttpServer` (JDK built-in) | JDK 17 | HTTP API server backing the web console | `web/` |

No JSON, charting, or JavaScript UI library is used anywhere in this
project: `web/Json.java` is a hand-rolled JSON writer and
`bench/Charts.java` is a hand-rolled SVG line-chart renderer, consistent
with the project's build-it-yourself standard.

## Appendix A — repository layout

See the README's repository layout table for the authoritative package
ownership map; summarized in
[04_system_architecture.md](04_system_architecture.md)'s module layout
section.

## Appendix B — where every deliverable actually lives

| Deliverable | Location |
|---|---|
| Source code | `src/main/java/edu/ug/nexusb/` |
| Tests | `src/test/java/edu/ug/nexusb/` |
| Dataset (CSVs) | `data/*.csv` |
| Database schema | `data/schema.sql` |
| Trace tables | `docs/traces/` |
| Proof sketches | `docs/proofs/` |
| Counterexamples | `docs/counterexamples/` |
| Benchmark data (CSV) | `results/csv/` |
| Benchmark charts (SVG) | `results/graphs/` |
| This report | `docs/report/` (this folder), concatenated in [`report_master.md`](report_master.md) |
| Cover sheet | [`../cover_sheet.md`](../cover_sheet.md) |
| Interface contracts | [`../interfaces.md`](../interfaces.md) |
| Index-number-derived parameters | [`../parameters.md`](../parameters.md) |
| Submission checklist | [`../submission_checklist.md`](../submission_checklist.md) |
