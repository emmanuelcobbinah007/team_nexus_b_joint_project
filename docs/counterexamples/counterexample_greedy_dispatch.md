# Counterexample: Nearest-First Greedy Dispatch Is Not Optimal

T051 asks for a greedy dispatch heuristic *and* an engineered case where it
fails — this is that case, worked by hand and confirmed by
`GreedyDispatchTest.greedyProducesAStrictlyWorsePenaltyThanOptimalOnAnEngineeredCase`.

## What "optimal" means here

A single resource visiting requests one after another can't be in two places
at once — travel time accumulates, so every request after the first waits at
least as long as everything ahead of it took to reach. `triageLevel` (1 =
most urgent, 4 = least) is treated as a wait-tolerance denominator: an
urgent case has almost no tolerance for delay, so its wait counts against the
total almost fully; a routine case tolerates the same wait four times as
well, so it counts for a quarter as much.

```
penalty(order) = Σ over i: (cumulative distance up to and including
                             request i) / triageLevel of request i
```

This is `GreedyDispatch.totalWeightedPenalty`. It is a standard scheduling
objective (weighted completion time), and it has a known optimal rule
(Smith's rule / weighted-shortest-processing-time): sort by
`processingTime / weight` ascending. Here `processingTime = distance` and
`weight = 1/triageLevel`, so the optimal sort key is
`distance * triageLevel` ascending — exactly what `runOptimalDispatch`
sorts by. `runGreedyDispatch` sorts by distance alone, with no reference to
`triageLevel` at all, so there is no reason to expect it to agree.

## The engineered case

One resource station, two waiting cases:

| Case | Facility | Distance from station | Triage level |
|---|---|---:|---:|
| `REQ_ROUTINE` | `F024` | 3 km | 4 (lowest urgency) |
| `REQ_URGENT` | `F053` | 5 km | 1 (highest urgency) |

The nearer case is the *less* urgent one — that mismatch is what makes
nearest-first fail here.

**Greedy dispatch** (`runGreedyDispatch`): sorts by distance alone.
3 km < 5 km, so it serves `REQ_ROUTINE` first: `[REQ_ROUTINE, REQ_URGENT]`.

**Optimal dispatch** (`runOptimalDispatch`): sorts by `distance * triageLevel`.
`REQ_ROUTINE` scores `3 × 4 = 12`; `REQ_URGENT` scores `5 × 1 = 5`. Lower
goes first: `[REQ_URGENT, REQ_ROUTINE]`.

## The two orders, scored

**Greedy order** `[REQ_ROUTINE, REQ_URGENT]`:

| Step | Case | Cumulative distance | ÷ triageLevel | Running penalty |
|---:|---|---:|---:|---:|
| 1 | `REQ_ROUTINE` | 3 | 3 / 4 | 0.75 |
| 2 | `REQ_URGENT` | 3 + 5 = 8 | 8 / 1 | 8.75 |

**Total: 8.75**

**Optimal order** `[REQ_URGENT, REQ_ROUTINE]`:

| Step | Case | Cumulative distance | ÷ triageLevel | Running penalty |
|---:|---|---:|---:|---:|
| 1 | `REQ_URGENT` | 5 | 5 / 1 | 5.0 |
| 2 | `REQ_ROUTINE` | 5 + 3 = 8 | 8 / 4 | 7.0 |

**Total: 7.0**

## Why greedy loses

Greedy's nearest-first rule makes the urgent case — which has almost no
tolerance for waiting — sit behind the routine case's entire service time
before it's even started. The 3 km saved by visiting the routine case first
is real, but it's spent making the *wrong* case wait: the routine case can
absorb a long wait cheaply (÷4), while forcing the urgent case to wait for
that same 3 km costs nearly the full amount (÷1). Optimal dispatch accepts
a longer first leg (5 km instead of 3 km) specifically to avoid stacking
that wait onto the case least able to tolerate it — a 20% lower total
penalty (7.0 vs 8.75) for the same two visits, just reordered.

This isn't just asserted here — it's confirmed by execution in
`GreedyDispatchTest`, using this exact scenario.
