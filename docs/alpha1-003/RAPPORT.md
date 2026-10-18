# JARVIS CORE — ALPHA1-003, clôture de phase A

18 octobre 2026. Branche : `feature/alpha1-003-native-validation`.
Base : `d2a677bb35e63ee94db9dfa4edcaa97036b45dce`.
Commit de livraison : `git log -1 --format=%H -- docs/alpha1-003/RAPPORT.md`.
Seuls documents, logs techniques et scripts non sensibles ajoutés ; aucun fichier
de la base modifié. Aucun candidat natif matérialisé, patch appliqué, AAR substituée,
modèle exécuté/téléchargé, installation ou activation d'Athéna.

## Résultat principal

Trois compilations propres de l'amont non modifié réussissent. Les deux racines
produisent des AAR différentes ; le contrôle au même chemin reproduit exactement
la première AAR. Classes.jar et libc++ identiques à Maven ; cinq ELF compilés
différents. Reproductibilité conditionnelle à chemin fixe sur cet hôte, pas
reproductibilité indépendante ni équivalence native avec l'AAR publiée.

La revue du patch identifie des corrections nécessaires : destination privée de
copie non imposée, copie non bornée si la source croît, livraison de handle sensible
à l'annulation dans l'API publique. Un probe de coroutines reproduit ce dernier
mécanisme avec ressource factice ; le CORE protégé par NonCancellable n'est pas
déclaré affecté par une fuite reproduite. Le composite modifie aussi la sélection
de dépendance CORE : séparer bibliothèque et futur raccordement avant application.

**Gate : NO_GO pour le patch initial en l'état ; phase B suspendue jusqu'à révision
et autorisation explicite du diff exact.** Aucun garde ni protection affaibli.

## Livrables

- [PROVENANCE.md](PROVENANCE.md) : sources/licences/archives/AAR/patch.
- [BUILD-REPRODUCIBILITY.md](BUILD-REPRODUCIBILITY.md) : outils, trois builds, limites.
- [SECURITY-REVIEW.md](SECURITY-REVIEW.md) : invariants, constats et risques.
- [TEST-PLAN.md](TEST-PLAN.md) : banc actuel et vrais tests natifs futurs.
- [PATCH-DECISION.md](PATCH-DECISION.md) : douze fichiers, gate, propriétaire et rollback.
- [RESULTS.json](RESULTS.json) : hashes, composants, lock181entrées, résultats détaillés.
- `evidence/` : huit logs techniques inspectés, sans modèle, APK, AAR ou secret.
  Seuls les espaces de fin de ligne sont normalisés dans les copies publiées ;
  originaux conservés hors checkout, hashes bruts et publiés distincts dans RESULTS.json.
- `tools/alpha1-003/` : vérificateur en mémoire/comparateur AAR-ELF, lanceur build
  amont, inventaire/refus du lock, oracle synthétique et probe Kotlin sans JNI.

## Exécutions actuelles

| Vérification | Résultat / portée |
|---|---|
| Sources, patch12fichiers, AAR historique | Réussis ; reconstruction en mémoire uniquement |
| Préservation167fichiers de base | Réussie, y compris laboratoire/app/contrats/migrations |
| Trois builds amont propres | Réussis,33 tâches chacun ; ARM64/API24 inspectés, aucun JNI exécuté |
| Lock dépendances | 181entrées/168artefacts, identique sur3builds ; bon lock accepté, mauvais refusé avant tâches |
| CORE JVM/Robolectric | 263 réussis,0 échec/erreur/ignoré |
| Fixture historique | 36 réussis,0 échec/erreur/ignoré |
| Oracle de spécification | 25 réussis, synthétiques, aucune autorité native |
| Probe annulation | Réussi, ressource factice sur vrai worker/coroutines JVM |
| APK debug CORE/fixture | Compilation réussie ; pas installation ou publication des APK |
| Lint |0 erreur ;32 avertissements app,0 fixture |
| Trois anciens gardes | ÉCHECS de périmètre, détaillés ci-dessous |

Lint rapporte32 avertissements contre27 historiquement documentés. Les cinq
avertissements GradleDependency actuels concernent des versions plus récentes
annoncées par les métadonnées ; aucun fichier de dépendance n'est changé. Les autres
catégories ne sont pas corrigées hors périmètre. Ce rapport ne remplace pas32 par27.
Les299 tests/builds/Lint ont été forcés avec --rerun-tasks :107 tâches exécutées,
1m38s. Les succès de cette session concernent l'application avec son ancienne AAR.

Échecs conservés :

- check-004c.py : règle absolue contre004B, `historical CORE changed`, déjà hors base actuelle.
- check-identity-002.py : ancien AppGraph, déjà différent à la base.
- check-alpha1-001.py : whitelist de ses seuls ajouts, refuse les nouveaux dossiers
  documentaires002/003 et les documents ID-002N préexistants. Aucun fichier exclu,
  assertion modifiée ou faux succès. Le contrôle de la base exacte passe séparément.

Une première invocation utilisait le répertoire d'outils par défaut indisponible
et échouait127 ; l'environnement existant vérifié a ensuite été sélectionné avec
variables Java/SDK/Gradle/CA/proxy scoped. Pas de HOME redéfini ni TLS désactivé.

## Non exécuté et blocages

Pas de prepare/consume/discard JNI candidat, égalité native réelle des tokens,
GGUF, coroutine candidate exécutée, appareil/émulateur, Binder entre APK, mesures
RAM/PSS/thermique/batterie, corpus40 ou conversation Athéna. Les4214 tests purs et
typechecks ALPHA1-002 restent historiques. F3 et CORE004D restent bloqués.

Les outils NDK/CMake manquants sont maintenant disponibles dans cet environnement,
mais cela n'autorise aucun patch sensible. Pas d'attestation anti-rollback ou de
preuve de fin native déduite des checksums, du probe ou des simulations.

## Publication et prochaine action

Publication demandée : cette branche seulement, aucun autre dépôt, aucune PR/fusion
ou workflow manuel de publication déclenché. Le workflow existant est workflow_dispatch ;
aucun téléchargement de gros modèle lancé. Le résultat exact du push et le SHA
de livraison sont fournis dans le compte rendu utilisateur ; une branche locale
ne doit jamais être annoncée comme publiée avant vérification distante.

Prochaine action recommandée : validation de la révision documentaire proposée dans
PATCH-DECISION.md. Produire ensuite un nouveau diff et son hash à revoir ; ne pas
commencer la phase B sans autorisation explicite supplémentaire. Tous les prompts,
profils, contrats IDENTITY, données SQLite, protections CORE et anciens APK restent
préservés.
