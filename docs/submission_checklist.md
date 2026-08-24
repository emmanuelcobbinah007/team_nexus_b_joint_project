# Submission Checklist (T079)

Owner: Frederick Kankam. Checked items were verified directly against the
repo (compiled, tested, or read) — not marked done from the tracker alone.
Run through this again right before T080 (tag + export) and T081 (submit).

## Code

- [x] Full suite passes: `mvn clean test` — 457/457 tests, zero failures
- [x] Clean build from a fresh clone (no local-only state required) —
      `nexus.db` regenerates via `mvn exec:java -Dexec.args="--init-db"`
- [x] No `java.util` collections outside `data/`/`bench/` — CI-enforced
      (`.github/workflows/build.yml`), spot-checked manually this session
- [x] Every structure/algorithm has normal + boundary + invalid-input tests
      (audited directly, T066 — see [07_testing_and_verification.md](report/07_testing_and_verification.md))
- [ ] All Week 4 branches merged to `main` with no open PRs left unresolved
      — check `gh pr list --state open` right before tagging

## Documentation

- [x] `docs/interfaces.md` — frozen contracts, complete
- [x] `docs/data_dictionary.md`, `docs/evidence_note.md`, `docs/parameters.md` — complete
- [x] `docs/traces/` — all 7 required trace tables present and verified against real runs (Dijkstra, Kruskal, Prim, merge sort, quicksort, binary search, knapsack)
- [x] `docs/proofs/` — all 3 proof sketches present (BST/RB height, hash table average cost, Kruskal cut property)
- [x] `docs/counterexamples/counterexample_greedy_dispatch.md` — engineered counterexample, verified by execution
- [x] `docs/report/` — sections 1-9 have real content, including all four Week 4 experiments; **10 (individual contributions) and 11 (conclusion) still need to be written** — see below
- [ ] `docs/dev_log.md` — dev log entry #3 (T067, owner Irene Tetteh)
- [ ] `docs/ai_usage_log.md` — up to date with every AI-assisted session, including this one

## Report assembly (T075)

- [x] `docs/report/report_master.md` assembled from sections 1-9
- [x] Section 8 (benchmarks): all four Week 4 experiments done — T070 (hash
      table load factor vs. collisions), T071 (BST vs. balanced tree),
      T072 (triage-priority vs. FCFS), T073 (graph algorithms vs.
      size/density) — real CSVs + charts, interpreted, not placeholders
- [ ] Section 10: each of the 15 members' individual contribution statement (T077)
- [ ] Section 11: conclusion — write last, once 1-10 are final
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
