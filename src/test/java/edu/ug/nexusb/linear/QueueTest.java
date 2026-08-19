package edu.ug.nexusb.linear;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QueueTest {

    @Test
    public void testArrayQueue() {
        ArrayCircularQueue<String> queue = new ArrayCircularQueue<>(2);
        assertTrue(queue.isEmpty());

        queue.enqueue("Apple");
        queue.enqueue("Banana");
        assertEquals(2, queue.size());

        assertEquals("Apple", queue.dequeue());
        assertEquals("Banana", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testCircularQueue() {
        CircularQueue<Integer> cq = new ArrayCircularQueue<>(2);
         cq.enqueue(1);
        cq.enqueue(2);
        assertTrue(cq.isFull());

        // Test that enqueuing when full throws an IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            cq.enqueue(3);
        });

        assertEquals(1, cq.dequeue());
        
        // Should succeed now via wrap-around
        cq.enqueue(3); 
        
        assertEquals(2, cq.dequeue());
        assertEquals(3, cq.dequeue());
        assertTrue(cq.isEmpty());
    }

    @Test
    public void testPeekFrontReturnsFrontWithoutRemoving() {
        ArrayCircularQueue<String> queue = new ArrayCircularQueue<>(2);
        queue.enqueue("Apple");
        queue.enqueue("Banana");

        assertEquals("Apple", queue.peekFront());
        assertEquals(2, queue.size(), "peekFront must not remove the element");
        assertEquals("Apple", queue.dequeue());
    }

    // ---- boundary case ----

    @Test
    public void testSingleCapacityQueueFillsAndDrainsCorrectly() {
        ArrayCircularQueue<Integer> queue = new ArrayCircularQueue<>(1);
        queue.enqueue(1);
        assertTrue(queue.isFull());
        assertEquals(1, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    // ---- invalid input ----

    @Test
    public void testDequeueOnEmptyQueueThrows() {
        ArrayCircularQueue<String> queue = new ArrayCircularQueue<>(2);
        assertThrows(NoSuchElementException.class, queue::dequeue);
    }

    @Test
    public void testPeekFrontOnEmptyQueueThrows() {
        ArrayCircularQueue<String> queue = new ArrayCircularQueue<>(2);
        assertThrows(NoSuchElementException.class, queue::peekFront);
    }

    @Test
    public void testZeroCapacityConstructorThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ArrayCircularQueue<String>(0));
    }

    @Test
    public void testNegativeCapacityConstructorThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ArrayCircularQueue<String>(-1));
    }
}