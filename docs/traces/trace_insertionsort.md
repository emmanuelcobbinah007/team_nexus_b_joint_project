# Trace: Insertion Sort

Worked trace of `edu.ug.nexusb.algorithms.InsertionSort.sort` (T041) on a
small example, step by step — verified against the real implementation
(`InsertionSortTest`'s `testSortNormalCase` uses the same shape of input;
this specific array was run through the actual code and its output
transcribed here).

## Input array

```
[5, 2, 4, 6, 1, 3]
```

## How it works

For each index `i` from `1` to `n-1`, `array[i]` is the "key" being
inserted into the already-sorted region `array[0..i-1]`: shift every
element in that region greater than the key one position right, then drop
the key into the gap that opens up.

## Step-by-step trace

| `i` | key | Shifts performed | Array after this step |
|---:|---:|---|---|
| 1 | 2 | `5` shifts right (`5>2`) | `[2, 5, 4, 6, 1, 3]` |
| 2 | 4 | `5` shifts right (`5>4`); `2` stops it (`2<4`) | `[2, 4, 5, 6, 1, 3]` |
| 3 | 6 | none — `5 < 6`, key is already in place | `[2, 4, 5, 6, 1, 3]` |
| 4 | 1 | `6, 5, 4, 2` all shift right (each `> 1`) | `[1, 2, 4, 5, 6, 3]` |
| 5 | 3 | `6, 5, 4` shift right; `2` stops it (`2<3`) | `[1, 2, 3, 4, 5, 6]` |

## Result

```
[1, 2, 3, 4, 5, 6]
```

Step 3 is worth calling out specifically: when the key is already
`>=` everything to its left, the inner `while` loop's condition fails on
the very first check, so **zero shifts happen** — this is exactly why
insertion sort's best case is `O(n)` (already-sorted input) rather than
`O(n²)`: the outer loop still runs `n-1` times, but each inner loop does
`O(1)` work instead of `O(i)`. `InsertionSortTest.testSortBoundaryEmptyAndSingle`
and `testSortReverseSorted` cover the two extremes this trace sits between.

This trace also demonstrates why the algorithm is
[stable](../../src/main/java/edu/ug/nexusb/algorithms/InsertionSort.java)
(`isStable()` returns `true`): the inner loop's shift condition is a
*strict* `>`, not `>=`, so an element equal to the key already in the
sorted region is never shifted past — two equal keys always keep their
original relative order.
