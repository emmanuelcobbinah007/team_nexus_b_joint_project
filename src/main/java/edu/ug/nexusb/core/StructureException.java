package edu.ug.nexusb.core;

/**
 * Base type for all failures raised by core data structures and algorithms.
 *
 * <p>Unchecked, deliberately: callers throughout the console application and
 * the algorithm layer are expected to catch specific subclasses (such as
 * {@link KeyNotFoundException}) at the point where they can produce a
 * meaningful console message, not to be forced into a checked-exception
 * signature on every core method.
 */
public class StructureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a structure exception with the given human-readable message.
     *
     * @param message explanation of what invariant or precondition failed
     */
    public StructureException(String message) {
        super(message);
    }

    /**
     * Creates a structure exception that wraps a lower-level cause.
     *
     * @param message explanation of what invariant or precondition failed
     * @param cause the underlying exception that triggered this failure
     */
    public StructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
