# Evidence Note

Assumptions and sourcing behind the synthetic dataset in [`data/`](../data/).

## Purpose

The dataset is synthetic (no real patient data). This note records what it's
meant to approximate and any assumptions baked into its generation, so the
report can cite them accurately.

## Assumptions

- **No real patient data, ever.** The database schema has no column for a
  name, contact detail, national ID, or diagnosis, so real patient data
  could not be stored even if someone offered it. Every case carries only a
  synthetic case ID, age band, and urgency level.
- **Generation is reproducible, not hand-picked.** The dataset generation
  seed and the two other index-number-derived parameters (hash table size,
  condition/priority weight factor) are computed from the 15 members' real
  index numbers — see [`parameters.md`](parameters.md) for the exact
  formulas and values, so anyone (including an examiner) can recompute them
  from the roster and get the same numbers.
- **Facility and road data models Greater Accra, not one building.** A
  single hospital doesn't naturally contain 50+ locations and 100+ weighted
  roads; the district network (teaching/regional hospitals, district
  hospitals, polyclinics, health centres, CHPS compounds, and support
  sites) gives every required graph algorithm a genuine reason to exist.
- ⚠️ **Not yet filled in — Sub-team A's call, not guessed at here:**
  population/region basis for the 150 facilities in `data/locations.csv`,
  the severity/urgency distribution rationale behind `data/request.csv`,
  and the resource-to-facility ratio behind `data/resources.csv`. These
  need the person who actually built each file, since the "how" behind the
  numbers is exactly what makes this defensible as genuinely local rather
  than generic.

## Known limitations

- **The dataset now joins, but 46 of the 117 facility records are not
  verified real institutions.** `data/locations.csv` was rebuilt (2026-08-09)
  with a `code` column covering all `F001`–`F117` referenced by
  `data/roads.csv`/`data/resources.csv`/`data/request.csv`. 4 facilities kept
  their original real coordinates; 71 have names recovered from
  `request.csv`'s redundant source/destination columns, of which 5
  well-known institutions were verified via web search and the rest use
  real Greater Accra district/neighborhood names with `facility_type`
  derived from the name suffix. The remaining **46 facility records were
  invented** — real place names and plausible coordinates, but not verified
  individual institutions, since no source for them existed anywhere on the
  team. Neither the original master list Sub-team A generated
  `roads.csv`/`resources.csv`/`request.csv` against, nor any record of who
  built it, could be recovered (Franca and John both confirmed they don't
  have it). Treat those 46 rows as synthetic filler, not researched fact,
  until someone verifies or replaces them.
- _(fill in: what the synthetic data does not capture, simplifications
  made for the DSA benchmarks)_
