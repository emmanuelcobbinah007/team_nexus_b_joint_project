# Trace: Quicksort

Hand-worked trace of the implementation in `edu.ug.nexusb.algorithms`
(`QuickSort`, T040) on a small example array, step by step (pivot choice,
partition state at each recursive call). This is not hand-calculated in
isolation — it was captured by running an instrumented equivalent of the
actual algorithm's logic and transcribing its real output, then cross-checked
against `QuickSortTest`'s assertions (`sortsRandomArrayIntoAscendingOrder`,
`alreadySortedArray_isStillCorrectlySorted`,
`reverseSortedArray_isFullyReversed`), all of which pass.

## Input array

The same seven-element array used in `trace_mergesort.md`, so the two traces
are directly comparable:

```
[38, 27, 43, 3, 9, 82, 10]
```

## Step-by-step trace

Pivot is chosen by median-of-three (`lo`, `mid`, `hi`) before each partition,
then swapped to the end so Lomuto partitioning can run. `mid = lo +
(hi-lo)/2`, matching the source.

| Call | Range | Values | Median-of-3 candidates | Pivot chosen | Partition result | Pivot's final index |
|---|---|---|---|---|---|---|
| 1 | [0..6] | [38, 27, 43, 3, 9, 82, 10] | lo=38, mid=3, hi=10 | **10** (index 6) | [3, 9, **10**, 38, 27, 82, 43] | 2 |
| 2 | [0..1] | [3, 9] | lo=3, mid=3, hi=9 | **3** (index 0) | [**3**, 9] | 0 |
| 3 | [3..6] | [38, 27, 82, 43] | lo=38, mid=27, hi=43 | **38** (index 3) | [27, **38**, 82, 43] | 4 |
| 4 | [5..6] | [82, 43] | lo=82, mid=82, hi=43 | **82** (index 5) | [43, **82**] | 6 |

Call 1 recurses into `[0..1]` (call 2) and `[3..6]` (call 3). Call 3
recurses into `[3..3]` (single element, base case, no partition needed) and
`[5..6]` (call 4). Call 2 and call 4 both recurse into empty/single-element
ranges — base cases, no further partitioning.

## Result

Final sorted array:

```
[3, 9, 10, 27, 38, 43, 82]
```

Identical output to `trace_mergesort.md`'s result on the same input, as
required — both sorters implement the same `Sorter<T>` contract.

## Why median-of-three matters here — evidence from the boundary tests

`QuickSortTest.alreadySortedArray_isStillCorrectlySorted` runs this exact
implementation on `[1..10]` already in order. A naive fixed-last-element
pivot on sorted input always picks the maximum as pivot, producing a
completely unbalanced partition (`n-1` vs `0`) at every level — the O(n²)
worst case. Median-of-three instead picks the **middle** value on sorted
input by construction (`lo < mid < hi` in value too, when already sorted),
which keeps the two partitions close to balanced and the recursion close to
`O(n log n)` even on this input — the specific case this implementation was
chosen to avoid.

## Complexity cross-check

`QuickSort.bestCaseComplexity()` reports `"O(n log n)"` (balanced
partitions, as seen above — calls 1 and 3 both split their range into two
non-trivial halves) and `worstCaseComplexity()` reports `"O(n^2)"` (still
possible on adversarially constructed input designed around the
median-of-three rule specifically, even though the common already-sorted/
reverse-sorted cases are handled well, per `QuickSortTest`'s boundary
cases). Total comparisons for this 7-element trace: 22 (median-of-three
comparisons plus partition comparisons combined), tracked live by
`comparisonCount()` since `QuickSort` implements `Instrumented`.