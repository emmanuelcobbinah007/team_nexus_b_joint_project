# Prim Trace — Minimum Facility Connection Network

Prim's algorithm starts from one facility and repeatedly selects the
lowest-weight edge connecting the current tree to an unvisited facility.

## Example graph

The graph contains these undirected weighted edges:

- `A-B = 1`
- `A-C = 4`
- `B-C = 2`
- `B-D = 5`
- `C-D = 1`

Starting vertex: `A`

## Step-by-step trace

| Step | Vertices in tree | Eligible frontier edges | Selected edge | Running total |
|---:|---|---|---|---:|
| 0 | `{A}` | `A-B(1), A-C(4)` | — | 0 |
| 1 | `{A, B}` | `B-C(2), A-C(4), B-D(5)` | `A-B(1)` | 1 |
| 2 | `{A, B, C}` | `C-D(1), A-C(4), B-D(5)` | `B-C(2)` | 3 |
| 3 | `{A, B, C, D}` | — | `C-D(1)` | 4 |

## MST result

Selected edges:

1. `A-B` with weight `1`
2. `B-C` with weight `2`
3. `C-D` with weight `1`

Total MST weight:

```text
1 + 2 + 1 = 4