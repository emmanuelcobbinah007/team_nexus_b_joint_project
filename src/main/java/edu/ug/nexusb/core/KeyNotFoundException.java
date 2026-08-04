package edu.ug.nexusb.core;

/**
 * Signals that a lookup key, vertex ID, or other identifier was not present
 * in the structure being queried.
 *
 * <p>Used across the trees/hashing module (a map lookup miss) and the graph
 * module (querying a vertex or edge that was never added), so a console menu
 * catching this one type can report "not found" uniformly regardless of
 * which structure raised it.
 */
public class KeyNotFoundException extends StructureException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message that should name the missing key
     * so the console menu can surface it directly to the examiner.
     *
     * @param message explanation naming the key or identifier that was not found
     */
    public KeyNotFoundException(String message) {
        super(message);
    }
}
