package gh.ug.smartops.structures;

/**
 * A minimal custom iterator contract for our hand-built linked list.
 *
 * We are not allowed to just hand back java.util.Iterator internals without
 * having written our own traversal logic, so this interface exists to make
 * that traversal explicit and defensible in the oral defense.
 *
 * @param <T> the element type being iterated over
 */
public interface MyIterator<T> {

    /**
     * @return true if there is at least one more element to visit,
     *         false otherwise. Must not advance the cursor.
     */
    boolean hasNext();

    /**
     * Returns the next element and advances the internal cursor by one.
     *
     * @return the next element in traversal order
     * @throws java.util.NoSuchElementException if hasNext() would return false
     */
    T next();
}
