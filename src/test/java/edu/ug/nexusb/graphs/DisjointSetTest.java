package edu.ug.nexusb.graphs;

public class DisjointSetTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        run("testInitialState", DisjointSetTest::testInitialState);
        run("testUnionBasic", DisjointSetTest::testUnionBasic);
        run("testUnionAlreadyConnected", DisjointSetTest::testUnionAlreadyConnected);
        run("testUnionChain", DisjointSetTest::testUnionChain);
        run("testDisjointGroups", DisjointSetTest::testDisjointGroups);
        run("testUnionAll", DisjointSetTest::testUnionAll);
        run("testPathCompressionFlattensTree", DisjointSetTest::testPathCompressionFlattensTree);
        run("testUnionByRankKeepsTreeShallow", DisjointSetTest::testUnionByRankKeepsTreeShallow);
        run("testOutOfRangeThrows", DisjointSetTest::testOutOfRangeThrows);
        run("testNegativeNThrows", DisjointSetTest::testNegativeNThrows);
        run("testSingleElement", DisjointSetTest::testSingleElement);
        run("testZeroElements", DisjointSetTest::testZeroElements);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS  " + name);
            passed++;
        } catch (Throwable t) {
            System.out.println("FAIL  " + name + "  -> " + t);
            failed++;
        }
    }

    private static void testInitialState() {
        DisjointSet ds = new DisjointSet(5);
        for (int i = 0; i < 5; i++) {
            assertEquals(i, ds.find(i));
        }
        assertEquals(5, ds.setCount());
    }

    private static void testUnionBasic() {
        DisjointSet ds = new DisjointSet(5);
        assertTrue(ds.union(0, 1));
        assertTrue(ds.connected(0, 1));
        assertEquals(4, ds.setCount());
    }

    private static void testUnionAlreadyConnected() {
        DisjointSet ds = new DisjointSet(5);
        ds.union(0, 1);
        assertFalse(ds.union(0, 1));
        assertEquals(4, ds.setCount());
    }

    private static void testUnionChain() {
        DisjointSet ds = new DisjointSet(6);
        ds.union(0, 1);
        ds.union(1, 2);
        ds.union(2, 3);
        assertTrue(ds.connected(0, 3));
        assertFalse(ds.connected(0, 4));
        assertEquals(3, ds.setCount()); // {0,1,2,3}, {4}, {5}
    }

    private static void testDisjointGroups() {
        DisjointSet ds = new DisjointSet(6);
        ds.union(0, 1);
        ds.union(2, 3);
        ds.union(4, 5);
        assertTrue(ds.connected(0, 1));
        assertTrue(ds.connected(2, 3));
        assertTrue(ds.connected(4, 5));
        assertFalse(ds.connected(0, 2));
        assertFalse(ds.connected(2, 4));
        assertEquals(3, ds.setCount());
    }

    private static void testUnionAll() {
        int n = 10;
        DisjointSet ds = new DisjointSet(n);
        for (int i = 0; i < n - 1; i++) {
            ds.union(i, i + 1);
        }
        assertEquals(1, ds.setCount());
        for (int i = 0; i < n; i++) {
            assertTrue(ds.connected(0, i));
        }
    }

    private static void testPathCompressionFlattensTree() {
        DisjointSet ds = new DisjointSet(5);
        ds.union(0, 1);
        ds.union(1, 2);
        ds.union(2, 3);
        ds.union(3, 4);
        int root = ds.find(4);
        for (int i = 0; i < 5; i++) {
            assertEquals(root, ds.parentOf(i));
        }
    }

    private static void testUnionByRankKeepsTreeShallow() {
        DisjointSet ds = new DisjointSet(8);
        ds.union(0, 1);
        ds.union(2, 3);
        ds.union(4, 5);
        ds.union(6, 7);
        ds.union(0, 2);
        ds.union(4, 6);
        ds.union(0, 4);
        assertEquals(1, ds.setCount());
        int root = ds.find(0);
        assertEquals(3, ds.rankOf(root));
    }

    private static void testOutOfRangeThrows() {
        DisjointSet ds = new DisjointSet(3);
        assertThrows(IndexOutOfBoundsException.class, () -> ds.find(5));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.union(0, 10));
    }

    private static void testNegativeNThrows() {
        assertThrows(IllegalArgumentException.class, () -> new DisjointSet(-1));
    }

    private static void testSingleElement() {
        DisjointSet ds = new DisjointSet(1);
        assertTrue(ds.connected(0, 0));
        assertEquals(1, ds.setCount());
    }

    private static void testZeroElements() {
        DisjointSet ds = new DisjointSet(0);
        assertEquals(0, ds.setCount());
        assertThrows(IndexOutOfBoundsException.class, () -> ds.find(0));
    }

    // --- tiny assertion helpers ---

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("expected true but was false");
        }
    }

    private static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("expected false but was true");
        }
    }

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError("expected " + expectedType.getSimpleName()
                    + " but got " + t.getClass().getSimpleName());
        }
        throw new AssertionError("expected " + expectedType.getSimpleName() + " but nothing was thrown");
    }
}
