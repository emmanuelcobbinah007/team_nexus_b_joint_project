package edu.ug.nexusb.data;

import edu.ug.nexusb.linear.ArrayStack;
import edu.ug.nexusb.linear.MyStack;

import java.util.EmptyStackException;


public final class AuditTrail {

    private final MyStack<AuditEvent> history;

    public AuditTrail() {
        this(new ArrayStack<>());
    }

    
    AuditTrail(MyStack<AuditEvent> history) {
        this.history = history;
    }

    public void record(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        history.push(event);
    }

    /**
     * Reverses the most recent decision.
     *
     * @return the event that was undone
     * @throws EmptyStackException if there is nothing left to undo
     */
    public AuditEvent undoLast() {
        return history.pop();
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public int size() {
        return history.size();
    }
}