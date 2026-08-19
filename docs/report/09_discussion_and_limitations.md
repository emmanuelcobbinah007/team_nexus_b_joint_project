# 9. Discussion, Limitations & Future Work

**Status: not yet written (T075).**

Scope for this section: honest trade-offs made under the project's
constraints (no `java.util` collections in core logic, index-derived
parameters, fixed timeline) and known limitations worth naming rather than
hiding — e.g. `IndexingEngine`'s time-range index compares `requested_at`
as plain lexicographic text (works for this dataset's consistent format,
but isn't a real calendar-aware range query), or any other rough edges
found during Week 4 integration testing (T069).
