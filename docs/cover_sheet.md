# DCIT 204/308 Project Submission Checklist and Cover Sheet

_Matches the official template (`Joint_DSA_Project_Checklist_Cover_Sheet.docx`) field for field._

| | |
|---|---|
| Team name | Team Nexus B (Group 29) |
| Selected Ghana context | Hospital / clinic operations (Greater Accra district health network) |
| Organisation/problem modelled | Ghana Smart Service Operations Optimizer — triage, ambulance dispatch, and routing across a real Greater Accra facility network |
| Database used | SQLite (`nexus.db`, built from `data/schema.sql`) |
| Programming language/version | Java 17 |
| Total records in dataset | 672 (117 facilities + 217 road links + 306 case requests + 32 resources) |
| Repository or submitted ZIP name | `team_nexus_b_joint_project` |

## Checklist

| Requirement | Tick | Evidence location |
|---|:---:|---|
| Local dataset with data dictionary | ✅ | [`docs/data_dictionary.md`](data_dictionary.md), [`data/locations.csv`](../data/locations.csv), [`data/roads.csv`](../data/roads.csv), [`data/request.csv`](../data/request.csv), [`data/resources.csv`](../data/resources.csv) |
| Database schema and seed data | ✅ | [`data/schema.sql`](../data/schema.sql); seeded via `DBLoader` (`mvn exec:java -Dexec.args="--init-db"`) |
| Custom data structures implemented | ✅ | `src/main/java/edu/ug/nexusb/{linear,trees,graphs}/` — see [05_data_structures.md](report/05_data_structures.md) |
| Searching and sorting algorithms | ✅ | `src/main/java/edu/ug/nexusb/algorithms/` (linear/binary search, selection/insertion/merge/quicksort) |
| Graph algorithms implemented | ✅ | `src/main/java/edu/ug/nexusb/graphs/` (BFS, DFS, Dijkstra, Prim, Kruskal) |
| Greedy and DP algorithms | ✅ | `src/main/java/edu/ug/nexusb/optimization/` (`GreedyDispatch`, `KnapsackDP`) |
| Correctness tests and trace tables | ✅ | 459+ tests (`mvn test`); [`docs/traces/`](traces/) (8 trace tables); [`docs/proofs/`](proofs/) (4 proof sketches); [`docs/counterexamples/`](counterexamples/) |
| Performance CSV and graphs | ✅ | [`results/csv/`](../results/csv/), [`results/graphs/`](../results/graphs/) — 6 experiments (search/sort comparison, hash table load factor, BST vs. balanced tree, heap insert/extract, triage-priority vs. FCFS, graph algorithms vs. size/density) |
| Technical report | ✅ | [`docs/report/report_master.md`](report/report_master.md) |
| Demo video / oral defense prepared | ☐ | Pending T076 (video) / T078 (mock defense, 21 Aug) |

---

## Additional detail (beyond the official template)

### Team roster

| # | Name | Index number | Role |
|---|---|---|---|
| 1 | Irene Tetteh | 22013982 | Group Leader (outward-facing) |
| 2 | Frederick Kankam | 22015587 | Planning & Delivery Lead |
| 3 | Johnson Kuzagbr | 22103951 | Sub-team E Lead |
| 4 | Cobbinah Emmanuel Kwaku Dua | 22169110 | Technical Lead / Integrator, Sub-team C Lead |
| 5 | Barnieh Owusu Victor | 22134010 | Sub-team A Lead |
| 6 | Obeng Jessica | 22051539 | Sub-team B Lead |
| 7 | Arthur, Philip Kofi | 22308160 | Sub-team B |
| 8 | El Masri El-Chaarani, Bilal | 22369794 | Sub-team B |
| 9 | Ajilogba, Abdulmalik Olamilekan | 22262429 | Sub-team E |
| 10 | Dakwa, Nana Kwabena Addo | 22399501 | Sub-team C |
| 11 | Kwetey, Sylvester Selasie Nicholas | 22369473 | Sub-team D |
| 12 | Arhin, Franca | 22310503 | Sub-team A |
| 13 | Mensah-Dogbevi, Princess Yvette Akorfa | 22376287 | Sub-team C |
| 14 | Salami, Oluwanifemi Abdul-Rahman | 22260038 | Sub-team E |
| 15 | Baah, John Excellence | 22325367 | Sub-team A |

Sub-team D (Graphs) also includes Frederick Kankam (lead) and Irene Tetteh,
per [`README.md`](../README.md)'s team table.

### Declaration

We confirm that:

1. Every data structure and algorithm in `src/main/java/edu/ug/nexusb`
   (outside `data/` and `bench/`) is implemented from scratch, without
   `java.util` collection classes — enforced by CI on every push.
2. All case and resource data in this submission is synthetic. No real
   patient data — names, contact details, national IDs, diagnoses, or
   clinical notes — appears anywhere in this repository or its database
   schema. See [`docs/evidence_note.md`](evidence_note.md).
3. Facility and road network data is real, compiled by team members from
   their own knowledge of Greater Accra, used here only as realistic
   structural input to the algorithms under test.
4. Every use of AI assistance is declared in
   [`docs/ai_usage_log.md`](ai_usage_log.md). Every team member can
   explain and modify any code submitted under their name.
5. The full test suite (`mvn clean test`) passes on a clean clone with no
   local-only state required.

### Signatures

| Name | Signature | Date |
|---|---|---|
| Irene Tetteh (Group Leader) | _____________________ | _________ |
| Frederick Kankam (Planning & Delivery Lead) | _____________________ | _________ |
| Cobbinah Emmanuel (Technical Lead) | _____________________ | _________ |

_Signatures are added by hand (or the team's actual sign-off process)
before submission, not filled in here._
