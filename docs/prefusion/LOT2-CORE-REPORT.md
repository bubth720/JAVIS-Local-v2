# CORE-PF-01 / Lot 2 — passive readiness projection

Base: `mission/prefusion-gates-20261026` commit `cd2adea393287ea94eb3145e8463209f6fa37e9d`.
No dependency provider, signature, session authority, connector or Android permission is changed.

**Files changed:** `DocumentToolGateSnapshot.kt`, `DocumentToolGateSnapshotTest.kt`,
`.github/workflows/prefusion-core-lot2.yml`, and this report.

The new snapshot reads both preexisting connector readiness statuses. No activation setter, service lookup, producer callback, model load or URI is created. `canExecute` is permanently false. It is a local projection, not a cross-app protocol or proof of FTS5/SAF/real device readiness.

New JVM/Robolectric tests check default denial, enabled-but-unqualified producer and repeated non-mutating snapshots. The dedicated CI runs existing app JVM regressions and Android assemble/lint **at the new HEAD**, without altering immutable historical `tools/check-fusion-02.py` receipts (which were designed for exact FUSION-02 source scope).

**Blockers:** ADR-002 CORE/SERVICES unapproved, NEXUS FTS5 unqualified, production SAF export OFF. C1-C4 not activated.

**Rollback:** revert only these additions on the isolated branch. No data/schema migration, no runtime activation, and no main merge.
