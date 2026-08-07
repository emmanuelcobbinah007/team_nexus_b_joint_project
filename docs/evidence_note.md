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

- **The dataset does not currently join.** `data/locations.csv` (the real,
  150-row facility list) has no `facility_id` column, while
  `data/roads.csv`, `data/resources.csv`, and `data/request.csv` already
  reference facility IDs (`F001`–`F117`) against a master facility list
  that doesn't exist yet — see the note left in `data/facilities.csv` for
  the specifics (only 4 of 71 facility names in `request.csv` currently
  resolve against `locations.csv`). Nothing downstream (loader, schema,
  algorithms) can be exercised on the real dataset until this is resolved.
- `docs/data_dictionary.md` still describes the original placeholder column
  layout (`facility_id, name, type, latitude, longitude, capacity`), not
  the actual columns in `locations.csv`/`roads.csv`/`request.csv`/
  `resources.csv`. Needs updating once the join gap above is resolved, so
  the dictionary describes real files rather than superseded ones.
- _(fill in once resolved: what the synthetic data does not capture,
  simplifications made for the DSA benchmarks)_
