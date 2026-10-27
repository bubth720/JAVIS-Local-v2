# CORE-PF-01 — deuxième lot pré-fusion / 2026-10-27

Base mission vérifiée Git/GitHub : `cd2adea393287ea94eb3145e8463209f6fa37e9d`, branche `mission/prefusion-gates-20261026`.
Développement : `codex/prefusion-lot2-20261027`. PR brouillon vers la branche mission, jamais main. SHA publié et résultats CI exacts dans la description de PR ; aucune auto-référence inventée.

## Changement borné

Projection PinnedDocumentReadiness passive, épinglant NEXUS201f0d2… et SERVICESea541d2… du premier lot sans les consommer. Réutilise DocumentToolReadinessPort, canExecute toujours faux ; aucune composition AppGraph, aucun registre/coordinateur/port réel. Trois nouveaux tests JUnit : références/refus, erreur expurgée et absence de mutation. AGENTS/PROJECT_KNOWLEDGE complétés par append.

## Validation et limites

Garde de gel local exécuté ; gardes historiques inchangés au SHA f6a3b2… dans clone temporaire. Nouvelle campagne complète : laboratoire JVM sur anciens snapshots immuables, régressions debug/MVP2/fixture, builds/lint. Vérifie tous les cas historiques présents et les trois nouveaux cas sans altérer les inventaires.

Les nouveaux tests Kotlin sont soumis à la CI après publication ; aucun résultat anticipé. Tests JVM/host != instrumentation != preuve physique. SDK, ABI, moteurs, modèles, budgets, contrats et anciennes preuves conservés. Aucun dépôt du premier lot modifié. Les gardes historiques CORE refusent volontairement le nouveau périmètre au HEAD ; leur garde dédié historique n’est ni assoupli ni présenté comme preuve actuelle. Les résultats nouveaux sont distincts.

## Blocages et décision

Backend NEXUS FTS5/vrai SAF, ADR-002/autorité durable et transport authentifié ; pas de pilote Android opérationnel, ni effet d’écriture autorisé.

Les essais sur appareil personnel, toute migration ou activation sensible attendent validation. Aucun merge main, déploiement, permission réseau/runtime ou nouvel import de dépendance. Les missions complètes restent PARTIAL/BLOCKED, seul ce lot ciblé est livré.

## Retour arrière et suite

Revert des commits de cette branche ; aucune migration ni donnée produit à restaurer. Lire la CI au SHA publié, corriger ses échecs sans skip ; ensuite traiter une tranche OFF indépendante. L’architecture d’ensemble, ADR-002 et les raccordements actifs ne sont pas présumés approuvés.
