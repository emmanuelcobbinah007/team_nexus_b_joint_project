# Proof: Hash Table Complexity

Correctness and complexity argument for `edu.ug.nexusb.trees.ChainedHashTable`
(implements `MyHashTable`).

## Correctness

Separate chaining resolves every collision by appending to a linked chain at
the target bucket, so no key is ever displaced or lost regardless of how many
keys hash to the same index — unlike open addressing, there is no probe
sequence to get wrong and no risk of the table filling up mid-search. A
lookup, insert, or delete only ever needs to examine the one bucket
`bucketIndex(key)` computes; every other bucket is provably irrelevant to
that key; correctness reduces to: (1) `bucketIndex` is a pure function of
the key, so a key always maps to the same bucket at insert and lookup time,
and (2) the chain walk uses `Object.equals`, not reference identity, so two
distinct key instances that are logically equal are still found.

The one place this could go wrong is resizing: rehashing must move every
existing entry into the new bucket array using the *new* capacity, not
leave it addressed by the old one. `resize()` recomputes `bucketIndex` for
every node during the rehash for exactly this reason — `ChainedHashTableTest
.exceedingLoadFactorTriggersAResizeThatGrowsCapacity` confirms every key
still resolves correctly immediately after a resize, not just before one.

## Complexity

**Assumption (simple uniform hashing):** each key is equally likely to hash
to any of the `m` buckets, independent of every other key. This is an
assumption about the hash function's distribution, not something the table
itself can guarantee — `bucketIndex`'s bit-spreading step
(`h ^= (h >>> 16)`) exists specifically to make it hold better in practice
for keys whose raw `hashCode()` has structure (e.g. sequential integers,
which without spreading would cluster in low-order buckets whenever the
table size shares a factor with the sequence).

**Expected chain length.** For `n` keys in `m` buckets, define the load
factor `α = n / m`. By linearity of expectation, the expected number of
keys landing in any one bucket is `n · (1/m) = α`.

**Expected cost of a search.** An unsuccessful search must walk the entire
target chain to confirm absence: `Θ(1 + α)`. A successful search is
`Θ(1 + α)` on average too — averaged over all `n` keys in insertion order,
the expected chain length seen while inserting the `i`-th key is at most
`α`, so the average successful-search cost is `Θ(1 + α)` as well (CLRS
Theorem 11.2's argument, not reproduced in full here).

**Why this is O(1) *average* rather than O(1) always:** `resize()` triggers
whenever `loadFactor() > 0.75`, doubling capacity (rounded up to the next
prime) and rehashing every entry, which keeps `α = O(1)` at all times. That
bounds the *expected* chain length to a constant — it says nothing about
any single bucket. `collisionCount()` and `longestBucket()` exist precisely
to make that gap visible: on real (non-uniformly-random) keys, a table can
have `α ≤ 0.75` and still have one badly overloaded bucket if the hash
function distributes poorly. The Week 4 experiment compares the two
directly rather than assuming uniformity held.

**Worst case: `Θ(n)`.** If every key collides into one bucket (adversarial
input, or a degenerate hash function), every operation degrades to a linear
scan of a single chain — chaining never fails outright the way open
addressing can when the table is full, but it offers no better worst case.

**Amortized cost of resizing.** A resize costs `Θ(current size)` when it
happens, but only happens when crossing the 0.75 threshold after roughly
doubling since the last one. Summed over a sequence of `n` insertions, total
resize work is `n + n/2 + n/4 + ... < 2n` — the same geometric-series
argument that makes a doubling dynamic array's amortized append cost `O(1)`
applies here identically, so resizing does not change the average-case
`O(1)` bound above.
