# Trace: 0/1 Knapsack Dynamic Programming

Worked trace of `edu.ug.nexusb.optimization.KnapsackDP.solve` (T052): the
tabulation build, then the backtracking reconstruction that recovers which
items were actually chosen. Uses a small hand-traceable instance rather than
`KnapsackDPTest`'s normal-case numbers (capacity 50 makes a 51-column table
impractical to print) — this instance was run through the real
implementation to confirm every table cell below.

## Input

Three items, capacity `5`:

| Item (index) | Weight | Value |
|---:|---:|---:|
| 0 | 2 | 3 |
| 1 | 3 | 4 |
| 2 | 4 | 5 |

## Building the DP table

`dp[i][w]` = the best value achievable using only the first `i` items with
capacity `w`. Row `0` (no items available) is all zeros; every other cell is
either "skip item `i-1`" (`dp[i-1][w]`) or, if it fits (`weight[i-1] <= w`),
"take it" (`value[i-1] + dp[i-1][w - weight[i-1]]`) — whichever is larger.

| `i` \ `w` | 0 | 1 | 2 | 3 | 4 | 5 |
|---:|---:|---:|---:|---:|---:|---:|
| 0 (no items) | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 (item 0: w2,v3) | 0 | 0 | 3 | 3 | 3 | 3 |
| 2 (item 1: w3,v4) | 0 | 0 | 3 | 4 | 4 | **7** |
| 3 (item 2: w4,v5) | 0 | 0 | 3 | 4 | 5 | **7** |

Worked example, `dp[2][5]` (items 0-1, capacity 5): item 1 fits
(`weight=3 <= 5`), so it's `max(take, skip)` =
`max(value[1] + dp[1][5-3], dp[1][5])` = `max(4 + dp[1][2], 3)` =
`max(4 + 3, 3)` = **7**. `dp[3][5]` stays `7`: item 2 also fits
(`weight=4 <= 5`), but taking it gives `max(5 + dp[2][1], dp[2][5])` =
`max(5 + 0, 7)` = `7` — no better than not taking it, so the "skip" branch
wins and item 2 is never selected. Final answer: `dp[3][5] = 7`.

## Reconstructing which items were chosen

Backtracking starts at `res = dp[n][capacity] = 7`, `w = capacity = 5`, and
walks `i` from `n` down to `1`: at each step, if `res != dp[i-1][w]`, item
`i-1` was taken — record it, then subtract its value from `res` and its
weight from `w`.

| Step | `i` | `w` | `res` | `dp[i-1][w]` | `res != dp[i-1][w]`? | Decision | New `res`, `w` |
|---:|---:|---:|---:|---:|---|---|---|
| 1 | 3 | 5 | 7 | `dp[2][5] = 7` | No (equal) | item 2 **not** selected | unchanged |
| 2 | 2 | 5 | 7 | `dp[1][5] = 3` | Yes | item 1 **selected** | `res = 7-4 = 3`, `w = 5-3 = 2` |
| 3 | 1 | 2 | 3 | `dp[0][2] = 0` | Yes | item 0 **selected** | `res = 3-3 = 0`, `w = 2-2 = 0` |

Loop stops (`res == 0`). `selectedIndices = [1, 0]` in that order (the loop
records item 1 before item 0, since it walks `i` downward from `n`) — items
1 and 0, total weight `3 + 2 = 5` (exactly the capacity), total value
`4 + 3 = 7`, matching `dp[3][5]`.

## Result

```
maxValue = 7
selectedIndices = {0, 1}   (item 2 correctly excluded — it never improves on skipping it)
```

This mirrors exactly what `KnapsackDPTest.testKnapsackOptimization` checks
on its own (larger) instance: the returned `maxValue` matches `dp[n][capacity]`,
and `selectedIndices` names the items that actually make up that value, not
just its size.
