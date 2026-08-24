package edu.ug.nexusb.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ug.nexusb.bench.TriageComparison.ComparisonResult;
import edu.ug.nexusb.bench.TriageComparison.DetailedResult;
import edu.ug.nexusb.bench.TriageComparison.TriageCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TriageComparisonTest {

    // ------------------------------------------------------------------
    // Normal case
    // ------------------------------------------------------------------

    @Test
    void compareMatchesTheHandTracedDemoScenario() {
        // Same four cases runComparison() prints, and the same 2-time-unit
        // processing cost per case.
        List<TriageCase> cases = List.of(
            new TriageCase("C001", 0, 3),
            new TriageCase("C002", 1, 1),
            new TriageCase("C003", 2, 2),
            new TriageCase("C004", 4, 1)
        );

        ComparisonResult result = TriageComparison.compare(cases);

        // FCFS (arrival order 0,1,2,4): waits 0,1,2,2 -> avg 5/4 = 1.25
        assertEquals(1.25, result.fcfsAverageWait, 1e-9);
        // Priority (C002 sev1@1, C004 sev1@4, C003 sev2@2, C001 sev3@0):
        // waits 0,0,2,6 -> avg 8/4 = 2.0. Worse raw average than FCFS here
        // precisely because reordering by severity makes the low-priority
        // case (C001) wait far longer -- this metric is unweighted total
        // wait, not urgency-weighted, so "priority mode" isn't automatically
        // the lower number on every case list.
        assertEquals(2.0, result.priorityAverageWait, 1e-9);
    }

    @Test
    void fcfsOrdersByArrivalTimeRegardlessOfInputListOrder() {
        // Deliberately out of arrival order in the input list.
        List<TriageCase> cases = List.of(
            new TriageCase("LATE", 10, 4),
            new TriageCase("EARLY", 0, 4)
        );

        ComparisonResult result = TriageComparison.compare(cases);

        // EARLY (arrival 0) must be served first: wait 0. LATE (arrival 10)
        // served second at currentTime=2: wait max(0, 2-10)=0. avg=0.
        assertEquals(0.0, result.fcfsAverageWait, 1e-9);
    }

    @Test
    void priorityOrdersBySeverityThenArrivalTimeOnTies() {
        List<TriageCase> cases = List.of(
            new TriageCase("A", 5, 1),
            new TriageCase("B", 1, 1) // same severity, earlier arrival -> served first
        );

        ComparisonResult result = TriageComparison.compare(cases);

        // B served first (arrival 1) at currentTime=0: wait 0.
        // A served second (arrival 5) at currentTime=2: wait 0.
        assertEquals(0.0, result.priorityAverageWait, 1e-9);
    }

    @Test
    void compareDetailedAveragesMatchCompareAndOrdersReflectEachModesRule() {
        List<TriageCase> cases = List.of(
            new TriageCase("C001", 0, 3),
            new TriageCase("C002", 1, 1),
            new TriageCase("C003", 2, 2),
            new TriageCase("C004", 4, 1)
        );

        DetailedResult detailed = TriageComparison.compareDetailed(cases);
        ComparisonResult aggregate = TriageComparison.compare(cases);

        // The detailed breakdown must average out to exactly the same
        // numbers compare() reports -- same simulation, just itemized.
        assertEquals(aggregate.fcfsAverageWait, detailed.fcfsAverageWait, 1e-9);
        assertEquals(aggregate.priorityAverageWait, detailed.priorityAverageWait, 1e-9);

        assertEquals(4, detailed.fcfsOrder.length);
        assertEquals(4, detailed.priorityOrder.length);

        // FCFS order is strictly arrival-time ascending.
        assertEquals("C001", detailed.fcfsOrder[0].caseId());
        assertEquals("C002", detailed.fcfsOrder[1].caseId());
        assertEquals("C003", detailed.fcfsOrder[2].caseId());
        assertEquals("C004", detailed.fcfsOrder[3].caseId());

        // Priority order is severity ascending, tie-broken by arrival:
        // C002 (sev1@1), C004 (sev1@4), C003 (sev2@2), C001 (sev3@0).
        assertEquals("C002", detailed.priorityOrder[0].caseId());
        assertEquals("C004", detailed.priorityOrder[1].caseId());
        assertEquals("C003", detailed.priorityOrder[2].caseId());
        assertEquals("C001", detailed.priorityOrder[3].caseId());
    }

    // ------------------------------------------------------------------
    // Boundary case
    // ------------------------------------------------------------------

    @Test
    void singleCaseAlwaysHasZeroWaitUnderBothModes() {
        List<TriageCase> cases = List.of(new TriageCase("ONLY", 7, 2));

        ComparisonResult result = TriageComparison.compare(cases);

        assertEquals(0.0, result.fcfsAverageWait, 1e-9);
        assertEquals(0.0, result.priorityAverageWait, 1e-9);
    }

    // ------------------------------------------------------------------
    // Invalid input
    // ------------------------------------------------------------------

    @Test
    void nullCasesThrows() {
        assertThrows(IllegalArgumentException.class, () -> TriageComparison.compare(null));
    }

    @Test
    void compareDetailedNullCasesThrows() {
        assertThrows(IllegalArgumentException.class, () -> TriageComparison.compareDetailed(null));
    }

    @Test
    void emptyCasesThrows() {
        assertThrows(IllegalArgumentException.class, () -> TriageComparison.compare(List.of()));
    }

    @Test
    void nullElementInCasesThrows() {
        List<TriageCase> cases = new ArrayList<>();
        cases.add(new TriageCase("C001", 0, 1));
        cases.add(null);
        assertThrows(IllegalArgumentException.class, () -> TriageComparison.compare(cases));
    }
}
