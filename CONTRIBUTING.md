# Contributing

## Branches

- `main` — always buildable, protected.
- `feature/<subteam>-<short-desc>` e.g. `feature/graphs-dijkstra`, `feature/data-loader`.
- `fix/<short-desc>` for bug fixes.

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): message`.

- `feat(graphs): add Dijkstra shortest path`
- `fix(linear): correct off-by-one in circular queue`
- `docs(report): add complexity analysis section`
- `test(trees): cover RB-tree deletion cases`

Scope = the package you touched (`core`, `data`, `linear`, `trees`, `graphs`, `algorithms`, `bench`, `app`).

## Pull requests

- One feature/fix per PR. Keep diffs reviewable.
- PR description: what changed, why, and how it was tested.
- Must pass `mvn test` before requesting review.
- Requires approval from the relevant CODEOWNER before merge.
- Changes under `core/` (frozen interfaces) require Cobbinah's explicit sign-off — open the PR early to discuss before writing the implementation against it.
- Squash-merge into `main`.
