# Frozen Interfaces

Human-readable description of the contracts defined in
[`src/main/java/edu/ug/nexusb/core/`](../src/main/java/edu/ug/nexusb/core/).

These are the boundaries between sub-teams. Once a sub-team starts building
against an interface here, changing it requires Cobbinah's sign-off and a
heads-up to everyone depending on it (see [CONTRIBUTING.md](../CONTRIBUTING.md)).

Update this file in the same PR that changes the interface — it should never
drift from `core/`.

## Status

No interfaces frozen yet. As each is added to `core/`, document it below:

### `<InterfaceName>`

- **Package:** `edu.ug.nexusb.core`
- **Owned by:** Cobbinah
- **Consumed by:** _(which sub-team packages implement/call this)_
- **Contract:** _(method signatures and what they guarantee — pre/post conditions, complexity expectations if relevant)_
