# Proof: Insertion Sort Correctness (Loop Invariant)

A loop-invariant argument for `edu.ug.nexusb.algorithms.InsertionSort.sort`
— the classic proof technique for an iterative search/sort algorithm,
applied to this project's own implementation rather than pseudocode.

## The code being proven correct

```java
public void sort(T[] array, MyComparator<T> comparator) {
    if (array == null || array.length <= 1) return;

    for (int i = 1; i < array.length; i++) {
        T key = array[i];
        int j = i - 1;

        while (j >= 0 && comparator.compare(array[j], key) > 0) {
            array[j + 1] = array[j];
            j = j - 1;
        }
        array[j + 1] = key;
    }
}
```

(`InsertionSort.java:24-37`; the null/length guard at the top is the
`array == null || array.length <= 1` boundary case, handled separately
below.)

## The invariant

**At the start of each iteration of the outer `for` loop (i.e. for the
current value of `i`), the subarray `array[0..i-1]` contains exactly the
elements that originally occupied `array[0..i-1]`, in sorted order
according to `comparator`.**

### Initialization

Before the first iteration, `i = 1`, so the subarray in question is
`array[0..0]` — a single element. A one-element subarray is trivially
sorted, and it trivially contains exactly the elements originally at that
position (there's been no mutation yet). The invariant holds before the
loop starts.

### Maintenance

Assume the invariant holds at the start of an iteration for some `i`:
`array[0..i-1]` is the original elements of that range, sorted. The loop
body must show it still holds at the start of iteration `i+1`, i.e. that
`array[0..i]` ends the iteration sorted and containing exactly its
original elements.

The `while` loop scans right-to-left through the already-sorted
`array[0..i-1]`, shifting each element strictly greater than `key`
(`comparator.compare(array[j], key) > 0`) one position to the right. This
does two things simultaneously:

1. **No element is lost or duplicated.** Every shift is `array[j+1] =
   array[j]`, moving one value one slot right; the loop terminates the
   moment `j < 0` or `array[j] <= key`, and the final line
   `array[j + 1] = key` places `key` into the exact slot the shifting
   just vacated. The subarray `array[0..i]` after the loop body is a
   permutation of `array[0..i]` before it — nothing outside this range is
   touched.
2. **The result is sorted.** Every element left of the final position of
   `key` is `<= key` (the loop only shifts elements `> key`, and stops as
   soon as it finds one that isn't), and every element shifted right of
   it was, by the invariant, part of the sorted `array[0..i-1]` and
   therefore already `>= key` in their relative order among themselves.
   `key` sits at the one position where everything to its left is `<=`
   it and everything to its right is `>` it — exactly the sorted-position
   requirement.

So after the loop body runs, `array[0..i]` is sorted and is exactly the
original elements of that range, reordered. The invariant holds for
`i + 1`.

### Termination

The outer loop's condition is `i < array.length`; it exits when
`i = array.length`. Substituting into the invariant: `array[0..array.length-1]`
— the whole array — contains exactly the original elements, sorted. That
is precisely the postcondition `sort` is required to establish.

## Boundary case handled outside the invariant

`array == null || array.length <= 1` returns immediately, before the loop
ever runs. A `null` array has no elements to reorder (nothing to prove);
a zero- or one-element array is sorted by definition regardless of the
loop, matching the invariant's own base case above. This is exactly the
case `InsertionSortTest.testSortNullArrayIsSafeNoOp` and
`testSortBoundaryEmptyAndSingle` assert directly, rather than leaving it
implicit.

## Cross-check against T041's actual test evidence

This proof's two maintenance claims are independently confirmed by
execution, not just argued on paper:

- **"No element is lost or duplicated"** — every `InsertionSortTest` case
  asserts the exact expected array via `assertArrayEquals`, which fails
  if the sort ever drops or duplicates a value.
- **"The result is sorted"** — the same assertions check final order
  directly; `testSortWithComparator` additionally confirms the invariant
  holds under a non-natural ordering (descending), not just the default
  case, since the proof above never assumed anything about `comparator`
  beyond it being a total order.

See [`trace_insertionsort.md`](../traces/trace_insertionsort.md) for a
concrete run showing the invariant's `array[0..i-1]` sorted-prefix
holding after every single outer-loop iteration on a real (unsorted)
array.
