package edu.ug.nexusb.trees;


import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;


    public class BinarySearchTree<K, V> implements MyTree<K, V> {

        private Node<K, V> root;
        private int size;
        private final MyComparator<? super K> comparator;
        private long comparisons;
        private long movements;


        public BinarySearchTree(MyComparator<? super K> comparator) {
            if (comparator == null) {
                throw new IllegalArgumentException("comparator cannot be null");
            }
            this.comparator = comparator;
        }

        @Override
        public V put(K key, V value) {
            requireKey(key);
            if (root == null) {
                root = new Node<>(key, value);
                size++;
                movements++;
                return null;
            }
            Node<K, V> current = root;
            while (true) {
                comparisons++;
                int cmp = comparator.compare(key, current.key);
                if (cmp == 0) {
                    V old = current.value;
                    current.value = value;
                    movements++;
                    return old;
                } else if (cmp < 0) {
                    if (current.left == null) {
                        current.left = new Node<>(key, value);
                        size++;
                        movements++;
                        return null;
                    }
                    current = current.left;
                } else {
                    if (current.right == null) {
                        current.right = new Node<>(key, value);
                        size++;
                        movements++;
                        return null;
                    }
                    current = current.right;
                }
            }
        }

        @Override
        public V get(K key) {
            requireKey(key);
            Node<K, V> node = findNode(key);
            return node == null ? null : node.value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V remove(K key) {
            requireKey(key);
            Object[] holder = new Object[1];
            root = removeNode(root, key, holder);
            return (V) holder[0];
        }

        @Override
        public boolean containsKey(K key) {
            requireKey(key);
            return findNode(key) != null;
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
        public MyIterable<MyMap.MapEntry<K, V>> entries() {
            Object[] buffer = new Object[size];
            int[] count = {0};
            collectEntries(root, buffer, count);
            return new SnapshotIterable<>(buffer, count[0]);
        }

        @Override
        public MyComparator<? super K> comparator() {
            return comparator;
        }

        @Override
        public int height() {
            return heightOf(root);
        }

        @Override
        public boolean isBalanced() {
            return checkHeight(root) != Integer.MIN_VALUE;
        }

        @Override
        public MyIterable<K> rangeKeys(K from, K to) {
            if (from == null || to == null) {
                throw new IllegalArgumentException("from and to cannot be null");
            }
            Object[] buffer = new Object[size];
            int[] count = {0};
            comparisons++;
            if (comparator.compare(from, to) <= 0) {
                collectRange(root, from, to, buffer, count);
            }
            return new SnapshotIterable<>(buffer, count[0]);
        }

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

        // ---- private helper methods ----

        private void requireKey(K key) {
            if (key == null) {
                throw new IllegalArgumentException("key cannot be null");
            }
        }

        private Node<K, V> findNode(K key) {
            Node<K, V> current = root;
            while (current != null) {
                comparisons++;
                int cmp = comparator.compare(key, current.key);
                if (cmp == 0) {
                    return current;
                }
                current = cmp < 0 ? current.left : current.right;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private Node<K, V> removeNode(Node<K, V> node, K key, Object[] holder) {
            if (node == null) {
                return null;
            }
            comparisons++;
            int cmp = comparator.compare(key, node.key);
            if (cmp < 0) {
                node.left = removeNode(node.left, key, holder);
                return node;
            } else if (cmp > 0) {
                node.right = removeNode(node.right, key, holder);
                return node;
            }

            holder[0] = node.value;
            size--;
            if (node.left == null && node.right == null) {
                movements++;
                return null;
            } else if (node.left == null) {
                movements++;
                return node.right;
            } else if (node.right == null) {
                movements++;
                return node.left;
            } else {
                Node<K, V> successor = minNode(node.right);
                node.key = successor.key;
                node.value = successor.value;
                movements++;
                node.right = removeMin(node.right);
                return node;
            }
        }

        private Node<K, V> minNode(Node<K, V> node) {
            while (node.left != null) {
                node = node.left;
            }
            return node;
        }

        private Node<K, V> removeMin(Node<K, V> node) {
            if (node.left == null) {
                movements++;
                return node.right;
            }
            node.left = removeMin(node.left);
            return node;
        }

        private int heightOf(Node<K, V> node) {
            if (node == null) {
                return -1;
            }
            return 1 + Math.max(heightOf(node.left), heightOf(node.right));
        }

        // Returns the subtree's height, or Integer.MIN_VALUE as a sentinel the
        // instant any node's left/right height difference exceeds 1.
        private int checkHeight(Node<K, V> node) {
            if (node == null) {
                return -1;
            }
            int leftHeight = checkHeight(node.left);
            if (leftHeight == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            int rightHeight = checkHeight(node.right);
            if (rightHeight == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            if (Math.abs(leftHeight - rightHeight) > 1) {
                return Integer.MIN_VALUE;
            }
            return 1 + Math.max(leftHeight, rightHeight);
        }

        private void collectEntries(Node<K, V> node, Object[] buffer, int[] count) {
            if (node == null) {
                return;
            }
            collectEntries(node.left, buffer, count);
            buffer[count[0]++] = new Entry<>(node.key, node.value);
            collectEntries(node.right, buffer, count);
        }

        private void collectRange(Node<K, V> node, K from, K to, Object[] buffer, int[] count) {
            if (node == null) {
                return;
            }
            comparisons++;
            int cmpFrom = comparator.compare(node.key, from);
            if (cmpFrom > 0) {
                collectRange(node.left, from, to, buffer, count);
            }
            comparisons++;
            int cmpTo = comparator.compare(node.key, to);
            if (cmpFrom >= 0 && cmpTo <= 0) {
                buffer[count[0]++] = node.key;
            }
            if (cmpTo < 0) {
                collectRange(node.right, from, to, buffer, count);
            }
        }

        // ---- private nested types ----

        private static final class Node<K, V> {
            private K key;
            private V value;
            private Node<K, V> left;
            private Node<K, V> right;

            private Node(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }

        private static final class Entry<K, V> implements MyMap.MapEntry<K, V> {
            private final K key;
            private final V value;

            private Entry(K key, V value) {
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

        private static final class SnapshotIterable<T> implements MyIterable<T> {
            private final Object[] items;
            private final int count;

            private SnapshotIterable(Object[] items, int count) {
                this.items = items;
                this.count = count;
            }

            @Override
            public MyIterator<T> iterator() {
                return new ArrayIterator<>(items, count);
            }
        }

        private static final class ArrayIterator<T> implements MyIterator<T> {
            private final Object[] items;
            private final int count;
            private int cursor;

            private ArrayIterator(Object[] items, int count) {
                this.items = items;
                this.count = count;
            }

            @Override
            public boolean hasNext() {
                return cursor < count;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasNext()) {
                    throw new StructureException("no more elements in iterator");
                }
                return (T) items[cursor++];
            }
        }
    }






















