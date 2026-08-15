package edu.ug.nexusb.scheduling;

import edu.ug.nexusb.core.MyComparator;

// Orders cases for the heap. Lower triage_level = more urgent = comes out first.
// ASSUMPTION: triage_level 1 is most urgent, 5 is least urgent. Confirm with Victor.
// If two cases share the same triage_level, whichever was requested earlier
// wins (this is the "FIFO vs urgency" blend the charter asks for).
public class TriageComparator implements MyComparator<TriageCase> {

    @Override
    public int compare(TriageCase a, TriageCase b) {
        if (a.getTriageLevel() != b.getTriageLevel()) {
            return a.getTriageLevel() - b.getTriageLevel();
        }
        return a.getRequestedAt().compareTo(b.getRequestedAt());
    }
}