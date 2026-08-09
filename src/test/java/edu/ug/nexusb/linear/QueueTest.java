package edu.ug.nexusb.linear;

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
}