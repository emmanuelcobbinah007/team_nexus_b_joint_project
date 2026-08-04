package edu.ug.nexusb.core;

/**
 * Contract for a structure that can hand out a {@link MyIterator} over its
 * elements. Declared here rather than reusing {@code java.lang.Iterable} so
 * that returning one from a core type never implicitly pulls in the
 * {@code java.util} iteration machinery (e.g. for-each sugar over
 * {@code java.util.Iterator}).
 *
 * @param <T> the type of element produced by the returned iterator
 */
public interface MyIterable<T> {

    /**
     * Returns a new, independent cursor positioned before the first element.
     * Each call returns a fresh iterator; iterators are not required to be
     * reusable once exhausted.
     *
     * @return a new {@link MyIterator} over this structure's elements
     */
    MyIterator<T> iterator();
}
