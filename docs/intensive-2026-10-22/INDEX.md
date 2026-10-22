# CORE / IDENTITY / MEMORY — campagne de développement
Référence : `a6f13dd101990c6b5e3992be506c9a5b99873621`, plan `plan/intensive-2026-10-22`. Les missions IDENTITY et MEMORY vivent dans le dépôt CORE, ce ne sont pas des dépôts GitHub indépendants.
- [CORE-01 — Orchestrateur d'intentions et gestionnaire de tâches locales](./CORE-01.md) — CORE, P0, dépendances Aucune
- [CORE-02 — Bus d'événements utilisateur et façade de modules autonomes](./CORE-02.md) — CORE, P1, dépendances CORE-01,UNI-01
- [CORE-03 — Intégrateur local de connaissances et d'outils (ports opt-in)](./CORE-03.md) — CORE, P2, dépendances CORE-01,CORE-02,NEX-03,SERV-01,UNI-01
- [ID-01 — Profils Athéna et moteur local de personnalité versionnée](./ID-01.md) — IDENTITY, P1, dépendances CORE-01
- [ID-02 — Centres d'intérêt simulés, règles de langage et contrôle des changements](./ID-02.md) — IDENTITY, P2, dépendances ID-01,MEM-01
- [MEM-01 — Registre de souvenirs explicitement confirmés et consentements](./MEM-01.md) — MEMORY, P1, dépendances CORE-01
- [MEM-02 — Rappel contextuel borné et export volontaire des souvenirs](./MEM-02.md) — MEMORY, P2, dépendances MEM-01,NEX-03
No-Go JNI Android encore en vigueur. Les missions de code ne sont pas autorisées à s'exécuter par cette publication.