# 11. Individual Contributions & Oral-Defense Notes

**Status: statements not yet written (T077, "write individual contribution
statement") — each of the 15 members fills in their own row and defense
notes below before submission.** The table structure and defense-question
framework are in place so that filling this in is a matter of each person
writing their own row, not designing the format under deadline pressure.

## Individual contribution statements

Each member should record: task IDs actually owned (noting any
reassignment away from the original tracker row — several tasks in this
project were reassigned mid-stream, and a statement should reflect what
was actually built, not just the original tracker assignment), the PRs
that shipped that work, and one sentence on the most technically
significant thing they personally debugged or decided — not just "wrote
the code," but the specific judgment call.

| Member | Sub-team | Task IDs owned | PRs | Most significant contribution (1 sentence) |
|---|---|---|---|---|
| Irene Tetteh | D (+ Group Leader) | | | |
| Frederick Kankam | D (+ Planning & Delivery Lead) | | | |
| Cobbinah Emmanuel | C (+ Technical Lead/Integrator) | | | |
| Victor Barnieh | A (lead) | | | |
| Arhin Franca | A | | | |
| Baah John Excellence | A | | | |
| Obeng Jessica | B (lead) | | | |
| Arthur Philip Kofi | B | | | |
| El Masri Bilal | B | | | |
| Mensah-Dogbevi Princess | C | | | |
| Dakwa Nana Kwabena | C | | | |
| Kwetey Sylvester | D | | | |
| Johnson Kuzagbr | E (lead) | | | |
| Ajilogba Abdulmalik | E | | | |
| Salami Oluwanifemi | E | | | |

Roster cross-reference (index numbers, sub-team assignments):
[`../cover_sheet.md`](../cover_sheet.md).

## Oral-defense preparation notes

One likely question per major decision this report documents, so the
team walks in having already answered them once:

| Likely question | Where the answer already lives |
|---|---|
| "Why not just use `java.util.HashMap`/`PriorityQueue`?" | [02_problem_statement.md](02_problem_statement.md)'s core structural requirement, [`proof_hashing.md`](../proofs/proof_hashing.md) |
| "How do you know Dijkstra/Kruskal/Prim are actually correct, not just that they run?" | [06_algorithm_implementation.md](06_algorithm_implementation.md), [`trace_dijkstra.md`](../traces/trace_dijkstra.md), [`trace_kruskal.md`](../traces/trace_kruskal.md), [`trace_prim.md`](../traces/trace_prim.md), `KruskalTest`/`PrimTest`'s cross-check of MST weight |
| "Why does greedy dispatch exist if it's not the recommended answer?" | [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md), [`counterexample_greedy_dispatch.md`](../counterexamples/counterexample_greedy_dispatch.md) |
| "What happens with an empty/disconnected/single-node graph?" | [07_correctness_evidence.md](07_correctness_evidence.md)'s edge-case table |
| "What's the biggest bug you found, and how?" | [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md)'s known-limitations section — Prim's `O(V)` bottleneck, found by benchmark comparison, not code review |
| "What doesn't work yet / what would you do differently?" | [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md)'s limitations and future-work sections — the `assignment` table gap, the unweighted T072 metric |
| "How is the dataset real, and how much of it is synthetic?" | [03_dataset_description.md](03_dataset_description.md) |
| "How did 15 people work on one codebase without constant merge conflicts?" | [04_system_architecture.md](04_system_architecture.md)'s module layout, the interfaces-first architecture |
