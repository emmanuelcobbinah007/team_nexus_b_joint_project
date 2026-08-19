# Trace: Binary Search (and the unsorted-input counterexample)

Worked trace of `edu.ug.nexusb.algorithms.LinearBinarySearch.binarySearch` (T039)
on a small sorted array, then the exact invalid-precondition case
(`binarySearchOnUnsortedInputCanSilentlyMissAPresentElement` in
`LinearBinarySearchTest`) that shows what happens when the sortedness
precondition is violated.

## Successful search on sorted input

Array (already sorted, natural order): `[5, 12, 18, 27, 35, 42, 50, 61]`
(indices `0..7`). Target: `42`.

| Step | `low` | `high` | `mid` | `array[mid]` | `compare(array[mid], target)` | Action |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 0 | 7 | 3 | 27 | `27 < 42` → negative | `low = mid + 1 = 4` |
| 2 | 4 | 7 | 5 | 42 | `42 == 42` → zero | **found**, return `5` |

Two comparisons to find an element 8 deep into the array — the point of
`O(log n)` over `linearSearch`'s `O(n)` (which would need 6 comparisons,
indices `0..5`, to reach the same element).

## Unsuccessful search (target absent)

Same array, target `40` (would sit between `array[4]=35` and `array[5]=42`,
but isn't present):

| Step | `low` | `high` | `mid` | `array[mid]` | `compare(array[mid], target)` | Action |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 0 | 7 | 3 | 27 | `27 < 40` → negative | `low = mid + 1 = 4` |
| 2 | 4 | 7 | 5 | 42 | `42 > 40` → positive | `high = mid - 1 = 4` |
| 3 | 4 | 4 | 4 | 35 | `35 < 40` → negative | `low = mid + 1 = 5` |
| — | 5 | 4 | — | — | `low > high` | loop ends, return `-1` |

## The required counterexample: unsorted input silently misses a present element

`binarySearch` has no way to check its own precondition at runtime — verifying
sortedness would cost `O(n)` and defeat the entire point of running in
`O(log n)`, so violating it is a **silent correctness bug**, not an exception.
This is the exact case `LinearBinarySearchTest` uses to demonstrate it:

Array (deliberately **not** sorted): `[50, 10, 30, 20, 40]`. Target: `20`
(genuinely present, at index `3`).

| Step | `low` | `high` | `mid` | `array[mid]` | `compare(array[mid], target)` | Action |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 0 | 4 | 2 | 30 | `30 > 20` → positive | `high = mid - 1 = 1` |
| 2 | 0 | 1 | 0 | 50 | `50 > 20` → positive | `high = mid - 1 = -1` |
| — | 0 | -1 | — | — | `low > high` | loop ends, return `-1` |

Binary search reports `20` as **absent** (`-1`), despite it genuinely sitting
at index `3`. Halving the search space on the very first comparison
(`30 > 20` sends the search left, away from index `3`, which is on the
*right*) is only a valid move when the array is sorted — on this unsorted
input it throws away the half that actually contains the target. Meanwhile
`linearSearch` on the identical array correctly returns `3`, since it makes
no assumption about order at all.

This isn't asserted only in prose — it's exactly what
`binarySearchOnUnsortedInputCanSilentlyMissAPresentElement` checks: both
searches run on the same unsorted array, `linearSearch` finds the target,
`binarySearch` doesn't, and the test passes specifically *because* they
disagree.
