# ADR-002 LOT4 — GitHub recovery / CORE

Base mission: b51a3720fd7abd7adaf65983c5fa4363f5ead225
Branch: feat/lot4-offline-recovery-20261028
Scope: app/src/test/java/com/stark/jarvislocal/coreui/lab/CoreLot4SyntheticAuthorityTest.kt + workflow only.

12 Kotlin/JUnit4 synthetic tests review an inert CORE-like fixture for default disabled state, immutable runtime denial, epoch/generation validation, revocation, cancellation, timeout, synthetic replay and UNKNOWN sticky. The fixture never runs in app/src/main, never assigns real Android authentication and never calls SERVICES or SAF; it is **not** a real authority implementation or approval of IPC.

Acceptance: app:testDebugUnitTest on CI at exact branch HEAD, Android build provenance and historical CORE regressions separately; a passing test-only class is not a certified CORE fence. No AppGraph, manifest, JNI or main change.
