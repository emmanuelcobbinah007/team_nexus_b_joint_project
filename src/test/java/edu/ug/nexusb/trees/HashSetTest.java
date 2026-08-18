package edu.ug.nexusb.trees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ug.nexusb.core.MyIterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HashSetTest {

    // ---- normal case ----

    @Nested
    @DisplayName("normal case")
    class NormalCase {

        @Test
        @DisplayName("add reports true for a new element, false for a duplicate")
        void addReportsTrueForNewFalseForDuplicate() {
            HashSet<String> set = new HashSet<>();
            assertTrue(set.add("F001"));
            assertFalse(set.add("F001"));
            assertEquals(1, set.size());
        }

        @Test
        @DisplayName("remove reports true when present, false when absent")
        void removeReportsTrueWhenPresentFalseWhenAbsent() {
            HashSet<String> set = new HashSet<>();
            set.add("F001");
            assertTrue(set.remove("F001"));
            assertFalse(set.remove("F001"));
        }

        @Test
        @DisplayName("contains reflects add and remove")
        void containsReflectsAddAndRemove() {
            HashSet<String> set = new HashSet<>();
            assertFalse(set.contains("F001"));
            set.add("F001");
            assertTrue(set.contains("F001"));
            set.remove("F001");
            assertFalse(set.contains("F001"));
        }

        @Test
        @DisplayName("iterator visits every element exactly once")
        void iteratorVisitsEveryElementOnce() {
            HashSet<String> set = new HashSet<>();
            set.add("A");
            set.add("B");
            set.add("C");

            int count = 0;
            boolean sawA = false;
            boolean sawB = false;
            boolean sawC = false;
            MyIterator<String> it = set.iterator();
            while (it.hasNext()) {
                String value = it.next();
                count++;
                sawA |= value.equals("A");
                sawB |= value.equals("B");
                sawC |= value.equals("C");
            }
            assertEquals(3, count);
            assertTrue(sawA && sawB && sawC);
        }
    }

    // ---- boundary case ----

    @Nested
    @DisplayName("boundary case")
    class BoundaryCase {

        @Test
        @DisplayName("a new set is empty")
        void newSetIsEmpty() {
            HashSet<String> set = new HashSet<>();
            assertTrue(set.isEmpty());
            assertEquals(0, set.size());
        }

        @Test
        @DisplayName("single element added then removed leaves the set empty")
        void singleElementAddedThenRemovedIsEmptyAgain() {
            HashSet<String> set = new HashSet<>();
            set.add("F001");
            set.remove("F001");
            assertTrue(set.isEmpty());
        }

        @Test
        @DisplayName("clear empties a populated set and it behaves like new afterward")
        void clearEmptiesAPopulatedSet() {
            HashSet<String> set = new HashSet<>();
            set.add("A");
            set.add("B");
            set.clear();
            assertTrue(set.isEmpty());
            assertEquals(0, set.size());
            assertFalse(set.contains("A"));
            assertTrue(set.add("A")); // behaves like a fresh set, not a stale one
        }

        @Test
        @DisplayName("iterating an empty set never yields an element")
        void iteratingAnEmptySetYieldsNothing() {
            HashSet<String> set = new HashSet<>();
            assertFalse(set.iterator().hasNext());
        }
    }

    // ---- invalid input ----

    @Nested
    @DisplayName("invalid input")
    class InvalidInput {

        @Test
        @DisplayName("add(null) throws")
        void addNullThrows() {
            HashSet<String> set = new HashSet<>();
            assertThrows(IllegalArgumentException.class, () -> set.add(null));
        }

        @Test
        @DisplayName("remove(null) throws")
        void removeNullThrows() {
            HashSet<String> set = new HashSet<>();
            assertThrows(IllegalArgumentException.class, () -> set.remove(null));
        }

        @Test
        @DisplayName("contains(null) throws")
        void containsNullThrows() {
            HashSet<String> set = new HashSet<>();
            assertThrows(IllegalArgumentException.class, () -> set.contains(null));
        }
    }

    // ---- use-case evidence (from MySet's own javadoc) ----

    @Nested
    @DisplayName("use-case evidence")
    class UseCaseEvidence {

        /**
         * The visited-marking use case {@link MySet}'s javadoc names for
         * BFS/DFS: a traversal marks each vertex visited exactly once and
         * never re-explores it, and {@link MySet#clear()} lets the same set
         * instance be reused for a second, independent traversal run.
         */
        @Test
        @DisplayName("visited-marking: a simulated BFS never revisits a vertex, and clear() resets it for reuse")
        void visitedMarkingNeverRevisitsAndClearResetsForReuse() {
            HashSet<String> visited = new HashSet<>();
            String[] frontier = {"F001", "F002", "F003", "F001", "F002"}; // F001/F002 rediscovered via a second edge

            int actuallyVisited = 0;
            for (String vertex : frontier) {
                if (visited.add(vertex)) {
                    actuallyVisited++;
                }
            }
            assertEquals(3, actuallyVisited, "each vertex should be newly visited exactly once");
            assertEquals(3, visited.size());

            // A second traversal run reuses the same set instance after clear().
            visited.clear();
            assertTrue(visited.add("F001"), "F001 must count as newly visited again after clear()");
        }

        /**
         * The duplicate-detection use case {@link MySet}'s javadoc names for
         * the data loader: {@code add}'s boolean return lets a caller detect
         * a duplicate inline, without a separate {@code contains} check
         * first.
         */
        @Test
        @DisplayName("duplicate detection on load: add() alone reports which rows were duplicates")
        void duplicateDetectionOnLoadReportsWhichRowsWereDuplicates() {
            HashSet<String> seenCodes = new HashSet<>();
            String[] incomingRows = {"F001", "F002", "F001", "F003", "F002", "F002"};

            int duplicates = 0;
            for (String code : incomingRows) {
                if (!seenCodes.add(code)) {
                    duplicates++;
                }
            }

            assertEquals(3, duplicates, "F001 and F002 repeat once and twice respectively -> 3 duplicate rows total");
            assertEquals(3, seenCodes.size(), "only the 3 distinct codes should remain in the set");
        }
    }
}
