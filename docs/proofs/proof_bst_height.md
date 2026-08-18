# Proof: BST vs. Red-Black Tree Height

Why a plain BST's height is unbounded in the worst case, and why
`edu.ug.nexusb.trees.RedBlackTree`'s height is provably `O(log n)`
regardless of insertion order. Height is measured the same way both
implementations define it: edges on the longest root-to-leaf path, `-1` for
an empty tree, `0` for a single node.

## Plain BST: no bound

A BST has no shape invariant at all — every insert just walks down by
comparison and attaches a new leaf where the search fell off the tree.
Nothing about that process depends on *how* the keys arrived, only their
relative order at each comparison.

**Worst case: `Θ(n)`.** Inserting `n` keys already in sorted (or
reverse-sorted) order produces a tree where every node has exactly one
child — a linked list wearing a tree's interface. Height is `n - 1`. This
is not a rare adversarial input for this project's data: `requested_at`
timestamps loaded from `request.csv` arrive in roughly chronological order,
which is close to sorted order for whichever key a tree index happens to
use.

**Average case: `Θ(log n)`.** For `n` keys inserted in a *uniformly random*
order, the expected height is `Θ(log n)` — the same recursive structure as
quicksort's average case (each insertion partitions the remaining keys
around the current one, exactly as a partition step does), which is why
random-order BSTs perform well in practice despite having no formal
guarantee. The gap between this and the worst case above is entirely a
property of the *input order*, not the algorithm — nothing in a plain BST
detects or corrects a bad insertion sequence.

## Red-Black tree: `height ≤ 2·log₂(n+1)`, always

This is the guarantee the balanced-tree comparison actually depends on: RB
height is bounded *regardless* of insertion order, because the four
invariants are restored after every single insert and delete, not just
checked at the end.

**Lemma.** Any node `x` with black-height `bh(x)` roots a subtree containing
at least `2^bh(x) − 1` internal (non-nil) nodes.

*Proof, by induction on the height of the subtree rooted at `x`.*
- **Base case:** `x` is a nil leaf. `bh(x) = 0`, and it contains `0 = 2⁰ − 1`
  internal nodes.
- **Inductive step:** `x` is internal, with two children. Each child's
  black-height is either `bh(x)` (if that child is red — red nodes don't
  count toward black-height) or `bh(x) − 1` (if black). Either way it is at
  least `bh(x) − 1`, so by the inductive hypothesis each child's subtree has
  at least `2^(bh(x)−1) − 1` internal nodes. Summing both children and
  counting `x` itself:
  `2·(2^(bh(x)−1) − 1) + 1 = 2^bh(x) − 1`. ∎

**From black-height to height.** No red node may have a red child (one of
the four invariants `RedBlackTree.isBalanced()` checks via
`noRedRedViolation`), so on any root-to-leaf path, no two consecutive nodes
are both red — at least half the nodes on the path are black. That gives
`height(root) ≤ 2 · bh(root)`.

**Putting it together.** Let `n` be the number of internal nodes (i.e.
`size()`). By the lemma, `n ≥ 2^bh(root) − 1`, so `bh(root) ≤ log₂(n+1)`.
Combined with the height bound above:

```
height(root) ≤ 2 · bh(root) ≤ 2 · log₂(n+1)
```

This is not just asserted — `RedBlackTreeTest.staysBalancedUnderSortedInsertion`
inserts 1000 keys in sorted order (the exact input that degenerates a plain
BST to height 999) and checks the tree's actual height against this bound
after every single insert, confirming the invariants hold throughout
construction, not just at the end.

## Why this comparison is the point of T019

Both trees satisfy the identical `MyTree` contract and run through the same
harness, so `height()` after identical input is a direct, apples-to-apples
measurement of what the invariants buy: on sorted input, the plain BST's
height grows linearly while the Red-Black tree's stays logarithmic — the
whole reason `MyTree.isBalanced()` exists is to make that difference
checkable in code, not just arguable on paper.
