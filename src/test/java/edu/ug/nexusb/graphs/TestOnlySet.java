package edu.ug.nexusb.graphs;

import edu.ug.nexusb.core.MyIterable;
import edu.ug.nexusb.core.MyIterator;
import edu.ug.nexusb.trees.MySet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A minimal {@link MySet} implementation used only in tests, so BFS/
 * reachability logic can be verified before Sub-team C's real hash-table-
 * backed MySet implementation is merged. Uses java.util.HashSet internally
 * — allowed here since the "no java.util collections" rule applies only to
 * src/main, not src/test.
 *
 * <p>TODO: once Sub-team C merges their real MySet implementation, replace
 * this with that class in ReachabilityTest — this class exists purely to
 * unblock T047 testing in the meantime.
 */
public class TestOnlySet<T> implements MySet<T> {

    private final Set<T> backing = new HashSet<>();
    private long comparisonCount;
    private long movementCount;

    @Override
    public boolean add(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        movementCount++;
        return backing.add(value);
    }

    @Override
    public boolean remove(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        movementCount++;
        return backing.remove(value);
    }

    @Override
    public boolean contains(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        comparisonCount++;
        return backing.contains(value);
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public long comparisonCount() {
        return comparisonCount;
    }

    @Override
    public long movementCount() {
        return movementCount;
    }

    @Override
    public void resetCounters() {
        comparisonCount = 0;
        movementCount = 0;
    }

    @Override
    public MyIterator<T> iterator() {
        Iterator<T> it = backing.iterator();
        return new MyIterator<T>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next();
            }
        };
    }
}