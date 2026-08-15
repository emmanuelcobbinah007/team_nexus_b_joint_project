package edu.ug.nexusb.scheduling;

// A lightweight stand-in for one row of case_request.
// Only holds the fields the dispatch engine actually needs.
public class TriageCase {

    private final int caseId;
    private final int triageLevel;   // 1-5, ASSUMED 1 = most urgent, confirm with Victor
    private final String requestedAt; // used as a tie-breaker: earlier request wins

    public TriageCase(int caseId, int triageLevel, String requestedAt) {
        this.caseId = caseId;
        this.triageLevel = triageLevel;
        this.requestedAt = requestedAt;
    }

    public int getCaseId() {
        return caseId;
    }

    public int getTriageLevel() {
        return triageLevel;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    // decreaseKey() and indexOf() in the heap use equals() to find a case,
    // so two TriageCase objects representing the same DB row must be equal.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TriageCase)) return false;
        return this.caseId == ((TriageCase) other).caseId;
    }

    @Override
    public int hashCode() {
        return caseId;
    }

    @Override
    public String toString() {
        return "TriageCase{caseId=" + caseId + ", triageLevel=" + triageLevel
                + ", requestedAt=" + requestedAt + "}";
    }
}