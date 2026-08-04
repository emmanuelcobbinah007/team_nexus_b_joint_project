package edu.ug.nexusb.core;

/**
 * A fail-fast cursor over the elements of a {@link MyIterable} structure.
 *
 * <p>Declared here rather than reusing {@code java.util.Iterator} so that no
 * core traversal logic depends on the Collections Framework. "Fail-fast"
 * means an implementation backed by a structure that is structurally
 * modified during iteration must throw rather than silently returning stale
 * or inconsistent data.
 *
 * @param <T> the type of element produced by this iterator
 */
public interface MyIterator<T> {

    /**
     * Reports whether {@link #next()} would return another element.
     *
     * @return {@code true} if there is at least one more element to visit
     */
    boolean hasNext();

    /**
     * Returns the next element in the traversal and advances the cursor.
     *
     * @return the next element
     * @throws StructureException if {@link #hasNext()} is {@code false}, or
     *     if the underlying structure was structurally modified since this
     *     iterator was created
     */
    T next();
}
