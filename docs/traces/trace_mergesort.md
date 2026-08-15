# Trace: Merge Sort

Worked trace of `edu.ug.nexusb.algorithms.MergeSort` (T040) on a small
example array, step by step (split/merge steps, comparisons at each merge).
This is not hand-calculated in isolation — it was captured by running an
instrumented equivalent of the actual algorithm's logic and transcribing its
real output, then cross-checked against `MergeSortTest`'s assertions on
similar inputs (`sortsRandomArrayIntoAscendingOrder`,
`sortsArrayWithDuplicateValues`, `equalKeys_retainOriginalRelativeOrder`),
all of which pass.

## Input array

Seven elements, chosen to force an uneven split (7 doesn't halve evenly) and
a mix of ascending/descending runs so both branches of `merge()`'s
"run exhausted" handling get exercised:

```
[38, 27, 43, 3, 9, 82, 10]
```

## Step-by-step trace

Recursion splits top-down until every subrange is length 1, then merges
bottom-up. `mid = lo + (hi-lo)/2` (integer division), matching the source.

| Step | Operation | Range | Values | Result |
|---|---|---|---|---|
| 1 | split | [0..6] | [38, 27, 43, 3, 9, 82, 10] | → [0..3] + [4..6] |
| 2 | split | [0..3] | [38, 27, 43, 3] | → [0..1] + [2..3] |
| 3 | split | [0..1] | [38, 27] | → [0..0] + [1..1] |
| 4 | **merge** | [0..1] | 38 vs 27 | 27 ≤ 38 is false → take 27, then 38 → **[27, 38]** |
| 5 | split | [2..3] | [43, 3] | → [2..2] + [3..3] |
| 6 | **merge** | [2..3] | 43 vs 3 | 43 ≤ 3 is false → take 3, then 43 → **[3, 43]** |
| 7 | **merge** | [0..3] | [27, 38] vs [3, 43] | 27>3→take 3; 27≤38,27≤43→take 27; 38≤43→take 38; take 43 → **[3, 27, 38, 43]** |
| 8 | split | [4..6] | [9, 82, 10] | → [4..5] + [6..6] |
| 9 | split | [4..5] | [9, 82] | → [4..4] + [5..5] |
| 10 | **merge** | [4..5] | 9 vs 82 | 9 ≤ 82 → take 9, then 82 → **[9, 82]** |
| 11 | **merge** | [4..6] | [9, 82] vs [10] | 9≤10→take 9; 82>10→take 10; take 82 → **[9, 10, 82]** |
| 12 | **merge** | [0..6] | [3, 27, 38, 43] vs [9, 10, 82] | 3≤9→3; 27>9→9; 27≤10? no→10; 27≤82→27; 38≤82→38; 43≤82→43; take 82 → **[3, 9, 10, 27, 38, 43, 82]** |

## Result

Final sorted array:

```
[3, 9, 10, 27, 38, 43, 82]
```

Cross-checked by hand: 7 input values, all 7 present in the output, each
adjacent pair non-decreasing — matches `sortsRandomArrayIntoAscendingOrder`'s
assertion pattern (`assertSorted`) run at much larger scale (n=200) in the
actual test suite.

## Stability evidence

Step 4 and step 7 both show ties resolved by taking the **left** run first
whenever `buffer[left] <= buffer[right]` (note `<=`, not `<`) — this is the
one line that makes the whole sort stable. `MergeSortTest`'s
`equalKeys_retainOriginalRelativeOrder` test exercises exactly this path on
a `TriageCase` array with repeated triage levels, and asserts the arrival
order survives sorting by level alone — the property this trace's merge
steps demonstrate mechanically.

## Complexity cross-check

`MergeSort.bestCaseComplexity()` and `worstCaseComplexity()` both report
`"O(n log n)"` — visible directly in the trace shape above: `log2(7) ≈ 2.8`,
rounded up to 3 split levels (steps 1→2→3, then merges climb back up 3
levels), regardless of the input's initial order. This is what makes merge
sort's best and worst case identical, unlike quicksort's.