# Submission Checklist (T079)

Owner: Frederick Kankam. Checked items were verified directly against the
repo (compiled, tested, or read) — not marked done from the tracker alone.
Run through this again right before T080 (tag + export) and T081 (submit).

## Code

- [x] Full suite passes: `mvn clean test` — 459/459 tests, zero failures
- [x] Clean build from a fresh clone (no local-only state required) —
      `nexus.db` regenerates via `mvn exec:java -Dexec.args="--init-db"`
      (re-verified this pass: fresh `App --init-db` run loaded
      117/217/32/306 rows, exact match against source CSVs — see
      [09_database_integration_evidence.md](report/09_database_integration_evidence.md))
- [x] No `java.util` collections outside `data/`/`bench/` — CI-enforced
      (`.github/workflows/build.yml`), spot-checked manually this session
- [x] Every structure/algorithm has normal + boundary + invalid-input tests
      (audited directly, T066 — see [07_correctness_evidence.md](report/07_correctness_evidence.md))
- [x] Live web console (`web.ApiServer` + browser frontend) exercised
      end-to-end via Playwright against a real Chromium instance, covering
      every capability (routing, MST, dispatch, triage, indexing,
      knapsack, sort/search)
- [ ] All Week 4 branches merged to `main` with no open PRs left unresolved
      — check `gh pr list --state open` right before tagging

## Documentation

- [x] `docs/interfaces.md` — frozen contracts, complete
- [x] `docs/data_dictionary.md`, `docs/evidence_note.md`, `docs/parameters.md` — complete
- [x] `docs/traces/` — **8 trace tables** present and verified against real
      runs (binary search, insertion sort, merge sort, quicksort, Dijkstra,
      Kruskal, Prim, knapsack) — exceeds the brief's 6-trace minimum
- [x] `docs/proofs/` — **4 proof sketches** present, covering all 3 brief
      categories (loop invariant — insertion sort; induction/recursion —
      BST/RB height; greedy/DP correctness — Kruskal cut property) plus a
      bonus hash-table complexity argument
- [x] `docs/counterexamples/` — **2 counterexamples**, matching the brief's
      minimum exactly (greedy-dispatch failure; unsorted-input binary
      search)
- [x] `docs/cover_sheet.md` — rewritten to match the official
      `Joint_DSA_Project_Checklist_Cover_Sheet.docx` template field-for-field
      (header table + 10-row requirement checklist), with the previously-built
      roster/declaration/signatures kept as an appendix
- [x] `docs/report/` — restructured to the brief's own **12-section**
      structure (verified against the actual course brief PDF, not just
      the tracker's interpretation); all 12 sections have real content —
      see below
- [ ] `docs/dev_log.md` — dev log entry #3 (T067, owner Irene Tetteh)
- [ ] `docs/ai_usage_log.md` — up to date with every AI-assisted session, including this one

## Report assembly (T075)

- [x] `docs/report/report_master.md` regenerated from all 12 numbered
      section files (`00_index.md` through `12_references_and_appendices.md`)
- [x] Section 8 (performance analysis): all **6** brief-required experiments
      done at or beyond the brief's minimum input ranges — search/sorting
      comparison (T064/`EarlyExperiments`, 100-10,000), hash table load
      factor (T070, extended this pass to **100-20,000**, matching the
      brief exactly), BST vs. balanced tree (T071), heap priority dispatch
      insert/extract (**new this pass**, `HeapExperiments`, 100-20,000),
      graph algorithms vs. size/density (T073, 50-800) — real CSVs +
      charts, interpreted, not placeholders
- [x] Section 9: database integration evidence — real load/read/write
      counts per table, `assignment`'s not-yet-written status named
      explicitly rather than hidden
- [x] Section 10: responsible algorithm selection, limitations, future
      work, and conclusion — merges the former discussion/limitations and
      conclusion sections
- [ ] Section 11: each of the 15 members' individual contribution
      statement and oral-defense notes (T077) — table structure and
      defense-question framework are in place, rows need filling in
- [x] Section 12: references and appendices — textbook/brief citations,
      dependency table, deliverable location index
- [ ] Export `report_master.md` → `report_final.docx` and `.pdf`
      (`pandoc docs/report/report_master.md -o report_final.docx`)

## Video & defense

- [ ] `demo_video.mp4` — 5-8 minute assembled demo (T076, video volunteer)
- [ ] Each module's 60-90 second demo clip in the shared folder (T065, all sub-teams)
- [ ] Mock oral defense completed, hard questions logged (T078, Fri 21 Aug)

## Final release (T080, owner Cobbinah Emmanuel)

- [ ] Tag the final release commit
- [ ] Export a clean repo ZIP
- [ ] Verify the ZIP builds from a clean clone with no local state
      (`git clone <zip-source> fresh && cd fresh && mvn clean test`)

## Submission (T081, owner Irene Tetteh)

- [ ] Every item above checked
- [ ] Signed cover sheet attached (see [cover_sheet.md](cover_sheet.md))
- [ ] Submitted **Sat 22 Aug, by the afternoon** — not midnight
