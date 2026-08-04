# Index-Number-Derived Parameters

**T005** — owner: Frederick Kankam. Satisfies the brief's requirement (Section 2,
item iii) that at least three algorithm parameters be derived from member index
numbers, so results are provably specific to this team's roster and cannot be
copied from a generic example.

Every formula below is defined only in terms of the 15 index numbers, so any
examiner can recompute it from the roster and get the same result.

## Roster (Group 29 — Team Nexus B)

| # | Name | Index number |
|---|---|---|
| 1 | Irene Tetteh (Group Leader) | 22013982 |
| 2 | Frederick Kankam | 22015587 |
| 3 | Johnson Kuzagbr | 22103951 |
| 4 | Cobbinah Emmanuel Kwaku Dua | 22169110 |
| 5 | Barnieh Owusu Victor | 22134010 |
| 6 | Obeng Jessica | 22051539 |
| 7 | Arthur, Philip Kofi | 22308160 |
| 8 | El Masri El-Chaarani, Bilal | 22369794 |
| 9 | Ajilogba, Abdulmalik Olamilekan | 22262429 |
| 10 | Dakwa, Nana Kwabena Addo | 22399501 |
| 11 | Kwetey, Sylvester Selasie Nicholas | 22369473 |
| 12 | Arhin, Franca | 22310503 |
| 13 | Mensah-Dogbevi, Princess Yvette Akorfa | 22376287 |
| 14 | Salami, Oluwanifemi Abdul-Rahman | 22260038 |
| 15 | Baah, John Excellence | 22325367 |

## Parameter A — hash table initial size

Used by `MyHashTable` (Sub-team C, T019/indexing engine) as the starting
bucket-array length, so the table's initial size is not a magic literal.

```
last3(id)   = id mod 1000                         (last 3 digits of each index number)
sum_last3   = sum of last3(id) over all 15 members = 6731
base        = sum_last3 mod 100                    = 31
pre_size    = base + 17                             = 48   (17 is a floor so the table never starts tiny)
table_size  = next prime >= pre_size                = 53
```

**`INITIAL_TABLE_SIZE = 53`**

Consumed at: `MyHashTable`'s no-arg / default constructor (Sub-team C, not yet
implemented — `src/main/java/edu/ug/nexusb/trees/`). Referenced in
[interfaces.md:80](interfaces.md#L80) as the first index-derived parameter.

## Parameter B — dataset generation seed

Used to seed the pseudo-random generator behind the synthetic case and
resource CSVs (T014, T015), so the "random" dataset is reproducible and
traceable to this roster rather than to an arbitrary run.

```
last4(id)  = id mod 10000                          (last 4 digits of each index number)
seed       = sum of last4(id) over all 15 members
           = 79731
```

**`GENERATION_SEED = 79731`**

Consumed at: the case/resource generator behind `data/cases.csv` and
`data/resources.csv` (Sub-team A — Arhin Franca / Baah John Excellence, T014 /
T015, not yet written). Pass `new Random(79731)` (or the custom PRNG's
equivalent seed argument) once that generator exists.

## Parameter C — global condition/priority weight factor

A small multiplier folded into the effective edge-weight formula
(`base_time × traffic_weight × condition_factor`, see the `v_weighted_edge`
view in `data/schema.sql`) and available to the triage/greedy scoring logic as
a tie-breaking nudge — again so the number is this team's, not a round default
like `1.0` or `1.5`.

```
digitsum(id) = sum of the individual digits of id
sum_digits   = sum of digitsum(id) over all 15 members = 408
w_base       = sum_digits mod 50                        = 8
weight       = 1.00 + w_base / 100                       = 1.08
```

**`CONDITION_WEIGHT_FACTOR = 1.08`**

Consumed at: the effective edge-weight computation used by Dijkstra/Prim/
Kruskal (Sub-team D, `src/main/java/edu/ug/nexusb/graphs/`, not yet
implemented) and optionally the greedy nearest-ambulance tie-break (Sub-team
E). Multiply the per-edge `road_condition_weight` by this constant when
computing effective travel time.

## Reproducing these numbers

```python
ids = [22013982, 22015587, 22103951, 22169110, 22134010, 22051539, 22308160,
       22369794, 22262429, 22399501, 22369473, 22310503, 22376287, 22260038,
       22325367]

sum_last3  = sum(i % 1000 for i in ids)                 # 6731
sum_last4  = sum(i % 10000 for i in ids)                 # 79731
sum_digits = sum(sum(int(d) for d in str(i)) for i in ids)  # 408

table_size = 53          # next prime >= (sum_last3 % 100) + 17
seed       = sum_last4   # 79731
weight     = 1.00 + (sum_digits % 50) / 100  # 1.08
```

Any team member (or the examiner) can re-run this against the roster above and
get identical values.
