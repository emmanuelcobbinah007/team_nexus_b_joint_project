package edu.ug.nexusb.core;

/**
 * Exposes cheap, machine-independent counters that stand in for wall-clock
 * timing during correctness and complexity discussions.
 *
 * <p>Wall-clock timings vary across the fifteen laptops this project is built
 * on; comparison and movement counts do not. Every custom structure and
 * algorithm implements this so the Week 4 efficiency graphs have a
 * cross-checkable second source of evidence, not just timing noise.
 *
 * <p>Implementations must increment counters as a simple {@code long++} with
 * no other side effects, so the counters cannot themselves distort the
 * measurement they exist to support.
 */
public interface Instrumented {

    /**
     * Returns the number of key/element comparisons performed since the
     * counters were last reset (or since construction, if never reset).
     *
     * @return the current comparison count, never negative
     */
    long comparisonCount();

    /**
     * Returns the number of element movements (assignments, swaps, pointer
     * relinks) performed since the counters were last reset.
     *
     * @return the current movement count, never negative
     */
    long movementCount();

    /**
     * Resets both {@link #comparisonCount()} and {@link #movementCount()} to
     * zero. Called by the benchmark harness immediately before each timed
     * run so that counts from warm-up iterations or prior experiments do not
     * leak into the next measurement.
     */
    void resetCounters();
}
