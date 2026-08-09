package edu.ug.nexusb.trees;

import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;

/**
 * A left-leaning-free, classic (CLRS-style) Red-Black tree implementation of
 * {@link MyTree}.
 *
 * <p>Every node is red or black; the root and every null leaf are black; a
 * red node never has a red child; and every root-to-leaf
 * path passes through the same number of black nodes (its "black-height").
 * Insertion and deletion restore these invariants with a bounded number of
 * recolorings plus at most a small constant number of rotations, which is
 * what keeps {@link #height()} at O(log n) regardless of insertion order -
 * the property the BST-vs-balanced-tree experiment in {@code docs/interfaces.md}
 * depends on {@link #isBalanced()} to confirm.
 *
 * <p>A single shared sentinel node ({@code nil}) stands in for every null
 * leaf and is always black. This is the standard CLRS trick: it lets every
 * fixup case dereference {@code node.parent}/{@code node.left}/
 * {@code node.right} without a null check, since {@code nil} is a real
 * object with real (if meaningless) pointers back to itself.
 *
 * <p>{@link #entries()} and {@link #rangeKeys} return a snapshot taken at
 * call time, not a live view - walking the tree while it is being iterated
 * would risk observing a half-completed rotation. The returned
 * {@link MyIterator} still fails fast against structural changes made
 * <em>after</em> the snapshot was taken, per the {@code MyIterable}/
 * {@code MyIterator} contract, by comparing a captured modification count
 * against the tree's live one on every {@link MyIterator#next()} call.
 *
 * <p>{@link #movementCount()} counts pointer relinks and color
 * changes (rotations, recolorings, transplants) - the operations whose
 * frequency is exactly what makes a Red-Black tree cheaper to rebalance
 * than, say, a naively rebuilt tree, and what the Week 4 report compares
 * against the plain BST's movement count.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class RedBlackTree<K, V> implements MyTree<K, V> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    /**
     * A tree node. Package-private fields are fine here - this class is
     * never exposed outside {@link RedBlackTree}, so there is no encapsulation
     * to protect.
     */
    private static final class Node<K, V> {
        K key;
        V value;
        boolean color;
        Node<K, V> left;
        Node<K, V> right;
        Node<K, V> parent;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class SimpleEntry<K, V> implements MapEntry<K, V> {
        private final K key;
        private final V value;

        SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }
    }

    /**
     * A minimal growable {@code Object[]} buffer - the collections-free
     * substitute for {@code java.util.ArrayList} used to gather traversal
     * results before handing back a snapshot {@link MyIterable}.
     */
    private static final class ObjBuffer {
        private Object[] data = new Object[8];
        private int count = 0;

        void add(Object o) {
            if (count == data.length) {
                Object[] grown = new Object[data.length * 2];
                System.arraycopy(data, 0, grown, 0, count);
                data = grown;
            }
            data[count++] = o;
        }
    }

    private final class SnapshotIterable<T> implements MyIterable<T> {
        private final Object[] data;
        private final int count;
        private final int snapshotModCount;

        SnapshotIterable(ObjBuffer buffer, int snapshotModCount) {
            this.data = buffer.data;
            this.count = buffer.count;
            this.snapshotModCount = snapshotModCount;
        }

        @Override
        public MyIterator<T> iterator() {
            return new MyIterator<T>() {
                private int position = 0;

                @Override
                public boolean hasNext() {
                    return position < count;
                }

                @Override
                @SuppressWarnings("unchecked")
                public T next() {
                    if (modCount != snapshotModCount) {
                        throw new StructureException(
                                "tree was structurally modified since this iterator was created");
                    }
                    if (position >= count) {
                        throw new StructureException("no more elements");
                    }
                    return (T) data[position++];
                }
            };
        }
    }

    private final MyComparator<? super K> comparator;
    private final Node<K, V> nil;
    private Node<K, V> root;
    private int size;
    private long comparisons;
    private long movements;
    private int modCount;

    /**
     * Creates an empty Red-Black tree ordered by {@code comparator}.
     *
     * @param comparator the ordering strategy for this tree's keys
     * @throws IllegalArgumentException if {@code comparator} is {@code null}
     */
    public RedBlackTree(MyComparator<? super K> comparator) {
        if (comparator == null) {
            throw new IllegalArgumentException("comparator must not be null");
        }
        this.comparator = comparator;
        this.nil = new Node<>(null, null);
        this.nil.color = BLACK;
        this.nil.left = this.nil;
        this.nil.right = this.nil;
        this.nil.parent = this.nil;
        this.root = this.nil;
    }

    // ------------------------------------------------------------------
    // Instrumented
    // ------------------------------------------------------------------

    @Override
    public long comparisonCount() {
        return comparisons;
    }

    @Override
    public long movementCount() {
        return movements;
    }

    @Override
    public void resetCounters() {
        comparisons = 0;
        movements = 0;
    }

    private int compareKeys(K a, K b) {
        comparisons++;
        return comparator.compare(a, b);
    }

    // ------------------------------------------------------------------
    // MyTree
    // ------------------------------------------------------------------

    @Override
    public MyComparator<? super K> comparator() {
        return comparator;
    }

    @Override
    public int height() {
        return height(root);
    }

    private int height(Node<K, V> node) {
        if (node == nil) {
            return -1;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    @Override
    public boolean isBalanced() {
        if (root.color != BLACK) {
            return false;
        }
        return blackHeight(root) >= 0 && noRedRedViolation(root);
    }

    private boolean noRedRedViolation(Node<K, V> node) {
        if (node == nil) {
            return true;
        }
        if (node.color == RED && (node.left.color == RED || node.right.color == RED)) {
            return false;
        }
        return noRedRedViolation(node.left) && noRedRedViolation(node.right);
    }

    /** Returns the black-height of {@code node} if consistent on every path beneath it, else -1. */
    private int blackHeight(Node<K, V> node) {
        if (node == nil) {
            return 0;
        }
        int left = blackHeight(node.left);
        if (left < 0) {
            return -1;
        }
        int right = blackHeight(node.right);
        if (right < 0 || left != right) {
            return -1;
        }
        return left + (node.color == BLACK ? 1 : 0);
    }

    @Override
    public MyIterable<K> rangeKeys(K from, K to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to must not be null");
        }
        ObjBuffer buffer = new ObjBuffer();
        if (compareKeys(from, to) <= 0) {
            fillRange(root, from, to, buffer);
        }
        return new SnapshotIterable<>(buffer, modCount);
    }

    private void fillRange(Node<K, V> node, K from, K to, ObjBuffer buffer) {
        if (node == nil) {
            return;
        }
        if (compareKeys(node.key, from) >= 0) {
            fillRange(node.left, from, to, buffer);
        }
        if (compareKeys(node.key, from) >= 0 && compareKeys(node.key, to) <= 0) {
            buffer.add(node.key);
        }
        if (compareKeys(node.key, to) <= 0) {
            fillRange(node.right, from, to, buffer);
        }
    }

    // ------------------------------------------------------------------
    // MyMap
    // ------------------------------------------------------------------

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        Node<K, V> parent = nil;
        Node<K, V> cursor = root;
        while (cursor != nil) {
            parent = cursor;
            int cmp = compareKeys(key, cursor.key);
            if (cmp == 0) {
                V old = cursor.value;
                cursor.value = value;
                movements++;
                return old;
            } else if (cmp < 0) {
                cursor = cursor.left;
            } else {
                cursor = cursor.right;
            }
        }
        Node<K, V> z = new Node<>(key, value);
        z.left = nil;
        z.right = nil;
        z.parent = parent;
        z.color = RED;
        movements++;
        if (parent == nil) {
            root = z;
        } else if (compareKeys(key, parent.key) < 0) {
            parent.left = z;
        } else {
            parent.right = z;
        }
        size++;
        modCount++;
        insertFixup(z);
        return null;
    }

    /** CLRS RB-INSERT-FIXUP: restores the red-red and black-height invariants after an insert. */
    private void insertFixup(Node<K, V> z) {
        while (z.parent.color == RED) {
            if (z.parent == z.parent.parent.left) {
                Node<K, V> uncle = z.parent.parent.right;
                if (uncle.color == RED) {
                    z.parent.color = BLACK;
                    uncle.color = BLACK;
                    z.parent.parent.color = RED;
                    movements += 3;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        z = z.parent;
                        leftRotate(z);
                    }
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    movements += 2;
                    rightRotate(z.parent.parent);
                }
            } else {
                Node<K, V> uncle = z.parent.parent.left;
                if (uncle.color == RED) {
                    z.parent.color = BLACK;
                    uncle.color = BLACK;
                    z.parent.parent.color = RED;
                    movements += 3;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        z = z.parent;
                        rightRotate(z);
                    }
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    movements += 2;
                    leftRotate(z.parent.parent);
                }
            }
        }
        root.color = BLACK;
    }

    private void leftRotate(Node<K, V> x) {
        Node<K, V> y = x.right;
        x.right = y.left;
        if (y.left != nil) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == nil) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left = x;
        x.parent = y;
        movements++;
    }

    private void rightRotate(Node<K, V> x) {
        Node<K, V> y = x.left;
        x.left = y.right;
        if (y.right != nil) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == nil) {
            root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right = x;
        x.parent = y;
        movements++;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        Node<K, V> node = findNode(key);
        return node == nil ? null : node.value;
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return findNode(key) != nil;
    }

    private Node<K, V> findNode(K key) {
        Node<K, V> cursor = root;
        while (cursor != nil) {
            int cmp = compareKeys(key, cursor.key);
            if (cmp == 0) {
                return cursor;
            }
            cursor = cmp < 0 ? cursor.left : cursor.right;
        }
        return nil;
    }

    @Override
    public V remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        Node<K, V> z = findNode(key);
        if (z == nil) {
            return null;
        }
        V oldValue = z.value;
        deleteNode(z);
        size--;
        modCount++;
        return oldValue;
    }

    private void transplant(Node<K, V> u, Node<K, V> v) {
        if (u.parent == nil) {
            root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        v.parent = u.parent;
        movements++;
    }

    private Node<K, V> minimum(Node<K, V> x) {
        while (x.left != nil) {
            x = x.left;
        }
        return x;
    }

    /** CLRS RB-DELETE: splices {@code z} out, then calls the fixup if a black node was removed. */
    private void deleteNode(Node<K, V> z) {
        Node<K, V> y = z;
        boolean yOriginalColor = y.color;
        Node<K, V> x;
        if (z.left == nil) {
            x = z.right;
            transplant(z, z.right);
        } else if (z.right == nil) {
            x = z.left;
            transplant(z, z.left);
        } else {
            y = minimum(z.right);
            yOriginalColor = y.color;
            x = y.right;
            if (y.parent == z) {
                x.parent = y;
            } else {
                transplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }
            transplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
            movements += 2;
        }
        if (yOriginalColor == BLACK) {
            deleteFixup(x);
        }
    }

    /** CLRS RB-DELETE-FIXUP: restores the black-height invariant after a black node is removed. */
    private void deleteFixup(Node<K, V> x) {
        while (x != root && x.color == BLACK) {
            if (x == x.parent.left) {
                Node<K, V> sibling = x.parent.right;
                if (sibling.color == RED) {
                    sibling.color = BLACK;
                    x.parent.color = RED;
                    movements += 2;
                    leftRotate(x.parent);
                    sibling = x.parent.right;
                }
                if (sibling.left.color == BLACK && sibling.right.color == BLACK) {
                    sibling.color = RED;
                    movements++;
                    x = x.parent;
                } else {
                    if (sibling.right.color == BLACK) {
                        sibling.left.color = BLACK;
                        sibling.color = RED;
                        movements += 2;
                        rightRotate(sibling);
                        sibling = x.parent.right;
                    }
                    sibling.color = x.parent.color;
                    x.parent.color = BLACK;
                    sibling.right.color = BLACK;
                    movements += 3;
                    leftRotate(x.parent);
                    x = root;
                }
            } else {
                Node<K, V> sibling = x.parent.left;
                if (sibling.color == RED) {
                    sibling.color = BLACK;
                    x.parent.color = RED;
                    movements += 2;
                    rightRotate(x.parent);
                    sibling = x.parent.left;
                }
                if (sibling.right.color == BLACK && sibling.left.color == BLACK) {
                    sibling.color = RED;
                    movements++;
                    x = x.parent;
                } else {
                    if (sibling.left.color == BLACK) {
                        sibling.right.color = BLACK;
                        sibling.color = RED;
                        movements += 2;
                        leftRotate(sibling);
                        sibling = x.parent.left;
                    }
                    sibling.color = x.parent.color;
                    x.parent.color = BLACK;
                    sibling.left.color = BLACK;
                    movements += 3;
                    rightRotate(x.parent);
                    x = root;
                }
            }
        }
        x.color = BLACK;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public MyIterable<MapEntry<K, V>> entries() {
        ObjBuffer buffer = new ObjBuffer();
        fillEntries(root, buffer);
        return new SnapshotIterable<>(buffer, modCount);
    }

    private void fillEntries(Node<K, V> node, ObjBuffer buffer) {
        if (node == nil) {
            return;
        }
        fillEntries(node.left, buffer);
        buffer.add(new SimpleEntry<>(node.key, node.value));
        fillEntries(node.right, buffer);
    }
}
