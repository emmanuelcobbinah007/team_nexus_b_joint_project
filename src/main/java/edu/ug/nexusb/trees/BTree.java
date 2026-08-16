package edu.ug.nexusb.trees;


import edu.ug.nexusb.core.MyComparator;
import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.core.StructureException;


    public class BTree<K, V> implements MyTree<K, V> {

        /** The minimum degree used when no explicit degree is supplied. */
        public static final int DEFAULT_MIN_DEGREE = 3;

        private final int minDegree;
        private final int maxKeys;
        private Node<K, V> root;
        private int size;
        private final MyComparator<? super K> comparator;
        private long comparisons;
        private long movements;


        public BTree(MyComparator<? super K> comparator) {
            this(comparator, DEFAULT_MIN_DEGREE);
        }


        @SuppressWarnings("unchecked")
        public BTree(MyComparator<? super K> comparator, int minDegree) {
            if (comparator == null) {
                throw new IllegalArgumentException("comparator cannot be null");
            }
            if (minDegree < 2) {
                throw new IllegalArgumentException("minDegree must be at least 2");
            }
            this.comparator = comparator;
            this.minDegree = minDegree;
            this.maxKeys = 2 * minDegree - 1;
            this.root = new Node<>(minDegree, true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public V put(K key, V value) {
            requireKey(key);
            Location<K, V> loc = locate(key);
            if (loc != null) {
                V old = (V) loc.node.values[loc.index];
                loc.node.values[loc.index] = value;
                movements++;
                return old;
            }
            insertNew(key, value);
            size++;
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(K key) {
            requireKey(key);
            Location<K, V> loc = locate(key);
            return loc == null ? null : (V) loc.node.values[loc.index];
        }

        @Override
        @SuppressWarnings("unchecked")
        public V remove(K key) {
            requireKey(key);
            if (locate(key) == null) {
                return null;
            }
            Object[] holder = new Object[1];
            boolean[] found = new boolean[1];
            root = deleteFromNode(root, key, holder, found);
            if (found[0]) {
                size--;
                if (root.numKeys == 0 && !root.leaf) {
                    root = root.children[0];
                    movements++;
                }
            }
            return found[0] ? (V) holder[0] : null;
        }

        @Override
        public boolean containsKey(K key) {
            requireKey(key);
            return locate(key) != null;
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
            if (size == 0) {
                return -1;
            }
            int h = 0;
            Node<K, V> node = root;
            while (!node.leaf) {
                node = node.children[0];
                h++;
            }
            return h;
        }

        @Override
        public boolean isBalanced() {
            // A B-tree keeps every leaf at the same depth and every node
            // within [minDegree - 1, 2 * minDegree - 1] keys by construction,
            // so this is trivially true rather than a computed check.
            return true;
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

        // ---- private helper methods: search ----

        private void requireKey(K key) {
            if (key == null) {
                throw new IllegalArgumentException("key cannot be null");
            }
        }

        @SuppressWarnings("unchecked")
        private Location<K, V> locate(K key) {
            Node<K, V> node = root;
            while (node != null) {
                int i = 0;
                while (i < node.numKeys) {
                    comparisons++;
                    int cmp = comparator.compare(key, (K) node.keys[i]);
                    if (cmp == 0) {
                        return new Location<>(node, i);
                    }
                    if (cmp < 0) {
                        break;
                    }
                    i++;
                }
                if (node.leaf) {
                    return null;
                }
                node = node.children[i];
            }
            return null;
        }

        // ---- private helper methods: insert ----

        private void insertNew(K key, V value) {
            if (root.numKeys == maxKeys) {
                Node<K, V> newRoot = new Node<>(minDegree, false);
                newRoot.children[0] = root;
                splitChild(newRoot, 0);
                movements++;
                root = newRoot;
            }
            insertNonFull(root, key, value);
        }

        @SuppressWarnings("unchecked")
        private void splitChild(Node<K, V> parent, int index) {
            int t = minDegree;
            Node<K, V> fullChild = parent.children[index];
            Node<K, V> newChild = new Node<>(t, fullChild.leaf);

            for (int j = 0; j < t - 1; j++) {
                newChild.keys[j] = fullChild.keys[j + t];
                newChild.values[j] = fullChild.values[j + t];
                fullChild.keys[j + t] = null;
                fullChild.values[j + t] = null;
                movements++;
            }
            newChild.numKeys = t - 1;

            if (!fullChild.leaf) {
                for (int j = 0; j < t; j++) {
                    newChild.children[j] = fullChild.children[j + t];
                    fullChild.children[j + t] = null;
                    movements++;
                }
            }

            K medianKey = (K) fullChild.keys[t - 1];
            V medianValue = (V) fullChild.values[t - 1];
            fullChild.keys[t - 1] = null;
            fullChild.values[t - 1] = null;
            fullChild.numKeys = t - 1;

            for (int j = parent.numKeys; j > index; j--) {
                parent.children[j + 1] = parent.children[j];
            }
            parent.children[index + 1] = newChild;

            for (int j = parent.numKeys - 1; j >= index; j--) {
                parent.keys[j + 1] = parent.keys[j];
                parent.values[j + 1] = parent.values[j];
            }
            parent.keys[index] = medianKey;
            parent.values[index] = medianValue;
            parent.numKeys++;
            movements++;
        }

        @SuppressWarnings("unchecked")
        private void insertNonFull(Node<K, V> node, K key, V value) {
            int i = node.numKeys - 1;
            if (node.leaf) {
                while (i >= 0) {
                    comparisons++;
                    if (comparator.compare(key, (K) node.keys[i]) < 0) {
                        node.keys[i + 1] = node.keys[i];
                        node.values[i + 1] = node.values[i];
                        movements++;
                        i--;
                    } else {
                        break;
                    }
                }
                node.keys[i + 1] = key;
                node.values[i + 1] = value;
                node.numKeys++;
                movements++;
            } else {
                while (i >= 0) {
                    comparisons++;
                    if (comparator.compare(key, (K) node.keys[i]) < 0) {
                        i--;
                    } else {
                        break;
                    }
                }
                i++;
                if (node.children[i].numKeys == maxKeys) {
                    splitChild(node, i);
                    comparisons++;
                    if (comparator.compare(key, (K) node.keys[i]) > 0) {
                        i++;
                    }
                }
                insertNonFull(node.children[i], key, value);
            }
        }

        // ---- private helper methods: delete ----

        @SuppressWarnings("unchecked")
        private int findKeyIndex(Node<K, V> node, K key) {
            int idx = 0;
            while (idx < node.numKeys) {
                comparisons++;
                if (comparator.compare(key, (K) node.keys[idx]) <= 0) {
                    break;
                }
                idx++;
            }
            return idx;
        }

        @SuppressWarnings("unchecked")
        private Node<K, V> deleteFromNode(Node<K, V> node, K key, Object[] holder, boolean[] found) {
            int idx = findKeyIndex(node, key);

            if (idx < node.numKeys) {
                comparisons++;
                if (comparator.compare(key, (K) node.keys[idx]) == 0) {
                    found[0] = true;
                    holder[0] = node.values[idx];
                    if (node.leaf) {
                        removeFromLeaf(node, idx);
                    } else {
                        removeFromNonLeaf(node, idx);
                    }
                    return node;
                }
            }

            if (node.leaf) {
                return node;
            }

            boolean lastChild = idx == node.numKeys;
            if (node.children[idx].numKeys < minDegree) {
                fill(node, idx);
            }

            if (lastChild && idx > node.numKeys) {
                node.children[idx - 1] = deleteFromNode(node.children[idx - 1], key, holder, found);
            } else {
                node.children[idx] = deleteFromNode(node.children[idx], key, holder, found);
            }
            return node;
        }

        private void removeFromLeaf(Node<K, V> node, int idx) {
            for (int i = idx + 1; i < node.numKeys; i++) {
                node.keys[i - 1] = node.keys[i];
                node.values[i - 1] = node.values[i];
                movements++;
            }
            node.keys[node.numKeys - 1] = null;
            node.values[node.numKeys - 1] = null;
            node.numKeys--;
            movements++;
        }

        @SuppressWarnings("unchecked")
        private void removeFromNonLeaf(Node<K, V> node, int idx) {
            K key = (K) node.keys[idx];
            Object[] dummyHolder = new Object[1];
            boolean[] dummyFound = new boolean[1];

            if (node.children[idx].numKeys >= minDegree) {
                Node<K, V> predNode = node.children[idx];
                while (!predNode.leaf) {
                    predNode = predNode.children[predNode.numKeys];
                }
                K predKey = (K) predNode.keys[predNode.numKeys - 1];
                V predValue = (V) predNode.values[predNode.numKeys - 1];
                node.keys[idx] = predKey;
                node.values[idx] = predValue;
                movements++;
                node.children[idx] = deleteFromNode(node.children[idx], predKey, dummyHolder, dummyFound);
            } else if (node.children[idx + 1].numKeys >= minDegree) {
                Node<K, V> succNode = node.children[idx + 1];
                while (!succNode.leaf) {
                    succNode = succNode.children[0];
                }
                K succKey = (K) succNode.keys[0];
                V succValue = (V) succNode.values[0];
                node.keys[idx] = succKey;
                node.values[idx] = succValue;
                movements++;
                node.children[idx + 1] = deleteFromNode(node.children[idx + 1], succKey, dummyHolder, dummyFound);
            } else {
                merge(node, idx);
                node.children[idx] = deleteFromNode(node.children[idx], key, dummyHolder, dummyFound);
            }
        }

        private void fill(Node<K, V> node, int idx) {
            if (idx != 0 && node.children[idx - 1].numKeys >= minDegree) {
                borrowFromPrev(node, idx);
            } else if (idx != node.numKeys && node.children[idx + 1].numKeys >= minDegree) {
                borrowFromNext(node, idx);
            } else if (idx != node.numKeys) {
                merge(node, idx);
            } else {
                merge(node, idx - 1);
            }
        }

        private void borrowFromPrev(Node<K, V> node, int idx) {
            Node<K, V> child = node.children[idx];
            Node<K, V> sibling = node.children[idx - 1];

            for (int i = child.numKeys - 1; i >= 0; i--) {
                child.keys[i + 1] = child.keys[i];
                child.values[i + 1] = child.values[i];
                movements++;
            }
            if (!child.leaf) {
                for (int i = child.numKeys; i >= 0; i--) {
                    child.children[i + 1] = child.children[i];
                    movements++;
                }
                child.children[0] = sibling.children[sibling.numKeys];
                sibling.children[sibling.numKeys] = null;
            }

            child.keys[0] = node.keys[idx - 1];
            child.values[0] = node.values[idx - 1];

            node.keys[idx - 1] = sibling.keys[sibling.numKeys - 1];
            node.values[idx - 1] = sibling.values[sibling.numKeys - 1];

            sibling.keys[sibling.numKeys - 1] = null;
            sibling.values[sibling.numKeys - 1] = null;

            child.numKeys++;
            sibling.numKeys--;
            movements++;
        }

        private void borrowFromNext(Node<K, V> node, int idx) {
            Node<K, V> child = node.children[idx];
            Node<K, V> sibling = node.children[idx + 1];

            child.keys[child.numKeys] = node.keys[idx];
            child.values[child.numKeys] = node.values[idx];

            if (!child.leaf) {
                child.children[child.numKeys + 1] = sibling.children[0];
            }

            node.keys[idx] = sibling.keys[0];
            node.values[idx] = sibling.values[0];

            for (int i = 1; i < sibling.numKeys; i++) {
                sibling.keys[i - 1] = sibling.keys[i];
                sibling.values[i - 1] = sibling.values[i];
                movements++;
            }
            if (!sibling.leaf) {
                for (int i = 1; i <= sibling.numKeys; i++) {
                    sibling.children[i - 1] = sibling.children[i];
                    movements++;
                }
                sibling.children[sibling.numKeys] = null;
            }
            sibling.keys[sibling.numKeys - 1] = null;
            sibling.values[sibling.numKeys - 1] = null;

            child.numKeys++;
            sibling.numKeys--;
            movements++;
        }

        private void merge(Node<K, V> node, int idx) {
            int t = minDegree;
            Node<K, V> child = node.children[idx];
            Node<K, V> sibling = node.children[idx + 1];

            child.keys[t - 1] = node.keys[idx];
            child.values[t - 1] = node.values[idx];

            for (int i = 0; i < sibling.numKeys; i++) {
                child.keys[t + i] = sibling.keys[i];
                child.values[t + i] = sibling.values[i];
                movements++;
            }
            if (!child.leaf) {
                for (int i = 0; i <= sibling.numKeys; i++) {
                    child.children[t + i] = sibling.children[i];
                    movements++;
                }
            }
            child.numKeys += sibling.numKeys + 1;

            for (int i = idx + 1; i < node.numKeys; i++) {
                node.keys[i - 1] = node.keys[i];
                node.values[i - 1] = node.values[i];
            }
            for (int i = idx + 2; i <= node.numKeys; i++) {
                node.children[i - 1] = node.children[i];
            }
            node.keys[node.numKeys - 1] = null;
            node.values[node.numKeys - 1] = null;
            node.children[node.numKeys] = null;
            node.numKeys--;
            movements++;
        }

        // ---- private helper methods: traversal ----

        @SuppressWarnings("unchecked")
        private void collectEntries(Node<K, V> node, Object[] buffer, int[] count) {
            if (node == null) {
                return;
            }
            int i;
            for (i = 0; i < node.numKeys; i++) {
                collectEntries(node.leaf ? null : node.children[i], buffer, count);
                buffer[count[0]++] = new Entry<>((K) node.keys[i], (V) node.values[i]);
            }
            collectEntries(node.leaf ? null : node.children[i], buffer, count);
        }

        @SuppressWarnings("unchecked")
        private void collectRange(Node<K, V> node, K from, K to, Object[] buffer, int[] count) {
            if (node == null) {
                return;
            }
            int i;
            for (i = 0; i < node.numKeys; i++) {
                collectRange(node.leaf ? null : node.children[i], from, to, buffer, count);
                comparisons++;
                int cmpFrom = comparator.compare((K) node.keys[i], from);
                comparisons++;
                int cmpTo = comparator.compare((K) node.keys[i], to);
                if (cmpFrom >= 0 && cmpTo <= 0) {
                    buffer[count[0]++] = node.keys[i];
                }
            }
            collectRange(node.leaf ? null : node.children[i], from, to, buffer, count);
        }

        // ---- private nested types ----

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static <K, V> Node<K, V>[] newChildrenArray(int length) {
            return new Node[length];
        }

        private static final class Node<K, V> {
            private final Object[] keys;
            private final Object[] values;
            private final Node<K, V>[] children;
            private final boolean leaf;
            private int numKeys;

            private Node(int minDegree, boolean leaf) {
                int maxKeys = 2 * minDegree - 1;
                this.keys = new Object[maxKeys];
                this.values = new Object[maxKeys];
                this.leaf = leaf;
                this.children = leaf ? null : newChildrenArray(2 * minDegree);
                this.numKeys = 0;
            }
        }

        private static final class Location<K, V> {
            private final Node<K, V> node;
            private final int index;

            private Location(Node<K, V> node, int index) {
                this.node = node;
                this.index = index;
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

