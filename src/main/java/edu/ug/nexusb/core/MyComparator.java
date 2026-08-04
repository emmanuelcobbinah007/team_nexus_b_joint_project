package edu.ug.nexusb.core;

/**
 * An ordering strategy over elements of type {@code T}.
 *
 * <p>Declared here rather than reusing {@code java.util.Comparator} so that
 * no core logic depends on the Collections Framework — the same reasoning
 * behind {@link MyIterator}. Defining this costs about fifteen lines and
 * removes the question entirely at the oral defense.
 *
 * @param <T> the type of element being compared
 */
public interface MyComparator<T> {

    /**
     * Compares two elements for ordering.
     *
     * @param a the first element
     * @param b the second element
     * @return a negative number if {@code a} sorts before {@code b}, a
     *     positive number if {@code a} sorts after {@code b}, or zero if
     *     they are equivalent for ordering purposes
     */
    int compare(T a, T b);
}
