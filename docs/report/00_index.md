# Report

Each report section lives in its own file in this folder, numbered in
reading order. This 12-section structure matches the DCIT 204/308 course
brief's own required report structure exactly — checked directly against
the brief, not just the team's own tracker interpretation.

- [01_cover_page.md](01_cover_page.md) — team, context, problem modelled
- [02_problem_statement.md](02_problem_statement.md) — problem statement, assumptions, input/output definitions, system boundaries
- [03_dataset_description.md](03_dataset_description.md) — dataset, data dictionary, database schema; links [../data_dictionary.md](../data_dictionary.md), [../evidence_note.md](../evidence_note.md)
- [04_system_architecture.md](04_system_architecture.md) — system architecture and module design; links [../interfaces.md](../interfaces.md), [../parameters.md](../parameters.md)
- [05_data_structures.md](05_data_structures.md) — one subsection per sub-team's structures (linear/trees/graphs)
- [06_algorithm_implementation.md](06_algorithm_implementation.md) — algorithms, pseudocode, Java snippets, complexity; links [../traces/](../traces/) and [../proofs/](../proofs/)
- [07_correctness_evidence.md](07_correctness_evidence.md) — test counts/coverage, trace/proof/counterexample index, edge-case checklist
- [08_performance_analysis.md](08_performance_analysis.md) — all six benchmark experiments against the brief's minimum ranges; links [../../results/](../../results/)
- [09_database_integration_evidence.md](09_database_integration_evidence.md) — real load/read/write evidence for every schema table
- [10_responsible_algorithm_selection.md](10_responsible_algorithm_selection.md) — why these algorithms for this problem, trade-offs, known limitations, future work, conclusion
- [11_individual_contributions_and_defense_notes.md](11_individual_contributions_and_defense_notes.md) — per-member contribution statements, oral-defense preparation notes
- [12_references_and_appendices.md](12_references_and_appendices.md) — citations, dependencies, deliverable location index

[`report_master.md`](report_master.md) concatenates all twelve into a
single document for submission.
