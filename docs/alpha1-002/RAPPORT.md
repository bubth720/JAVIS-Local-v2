# CORE — ALPHA1-002 : dossier de revue JNI

18 octobre 2026. **Préparation livrée ; aucune implémentation native appliquée au runtime.**

## Références et publication

Dépôt : bubth720/JAVIS-Local-v2. Base exacte
`b2ec98238f70e89edc636c1c671f2ddf1b957d74`, vérifiée localement et sur origin.
Branche de livraison : `design/alpha1-002-jni-review`.
Le commit de livraison est celui contenant ce rapport :
`git log -1 --format='%H %s' -- docs/alpha1-002/RAPPORT.md`.
Son SHA final et sa publication distante sont fournis dans le compte rendu de livraison.
Aucune fusion, PR, installation ou modification d'un autre dépôt autorisée/exécutée.

## Livrables

- [AUDIT-NATIF.md](AUDIT-NATIF.md) : AAR, empreintes, source, propriétaire et chaîne de fabrication.
- [ARCHITECTURE-JNI.md](ARCHITECTURE-JNI.md) : transaction, tickets, génération, worker, budgets et erreurs.
- [PATCH-A-VALIDER.patch](PATCH-A-VALIDER.patch) : douze fichiers proposés, corps C++/JNI/wrappers et appels CORE.
- [RISQUES-REGRESSION.md](RISQUES-REGRESSION.md) : chaque fichier et les surfaces sensibles.
- [PLAN-TESTS.md](PLAN-TESTS.md) : exécutions obtenues, reproduction et25 scénarios natifs futurs.
- SOURCE_LOCK.json, PATCH_FILES.json, RESULTS.json : provenance et preuves structurées.
- verify_review.py, typecheck_review.py, BUDGET_UTF8_REVIEW_TEST.cpp : vérifications isolées,
  sans application de patch aux sources historiques, modèle ou exécution JNI.
- AGENTS.md et docs/PROJECT_KNOWLEDGE.md : ajout documentaire de l'état et des gates,
  requis par les instructions du dépôt ; aucune règle historique supprimée.

Le patch ne contient pas de TODO d'implémentation, de corps ellipsé ou de faux
retour tokenizer. Il est reconstructible contre ses deux bases exactes ; les corps
C++ passent la syntaxe et les wrappers/appels Kotlin le contrôle de types. Cela
n'en fait pas un correctif Android qualifié ou autorisé à appliquer. Les deux
préfixes upstream/ et core/ sont un format de revue composite, pas des répertoires
runtime créés dans CORE. Tous les candidats compilés sont hors checkout.

## Résultats vérifiés

- Base et tag source revérifiés ; gitlink llama.cpp exact retrouvé.
- AAR et sept composants identiques ; checksum Maven HTTP200 identique.
- 31 fichiers source épinglés,153 fichiers antérieurs protégés identiques.
- Patch12 fichiers : reconstruction et contrôle à blanc réussis ; typecheck Kotlin
  bibliothèque/CORE et syntaxe C++ réussis, sans exécution native du modèle.
- 4214 cas purs budget/UTF-8 réussis ; pas4214 tests de tokenisation JNI.
- 299 tests actuels réussis (263app +36fixture),0 échec/erreur/ignoré,
  builds et Lint réussis ;27 avertissements app conservés. Commande avec --rerun-tasks,
 107 tâches exécutées,1m36s. Résultats completsdans RESULTS.json.
- Trois anciens gardes de périmètre échouent : check-004c (historical CORE changed),
  check-identity-002 (AppGraph), check-alpha1-001 (nouveau dossier documentaire).
  Les deux premiers échouaient déjà sur la base ; le troisième n'autorise que les
  ajouts ALPHA1-001. Aucun garde modifié/désactivé. Nouveau contrôle de préservation
  sur la base exacte réussi. Pas d'affirmation « tous les anciens validateurs verts ».
- Le diff brut signale les espaces syntaxiques du patch ; contrôle des autres
  fichiers et des candidats reconstruits réussi, détail dans PLAN-TESTS.md.
- Tag annoté v0.1.1 non signé : verify-tag exit1, no signature found.

## Blocages et limites

La source officielle et ses empreintes sont établies. **L'équivalence reproductible
entre cette source et l'AAR0.1.1 publiée ne l'est pas.** Tag non signé, environnement
amont partiellement mouvant et absence d'attestation contrôlée empêchent une telle
conclusion. Le lock outillage/dépendances de la future fabrication reste à compléter.
NDK/CMake Android absents ; aucune AAR expérimentale construite ou substituée.
Aucun GGUF ni appareil/émulateur autorisé utilisé. Aucune égalité réelle des IDs
prepare/prefill, mesure mémoire Android ou qualification API24 native encore obtenue.

La compatibilité source des anciens appels est préparée, pas une preuve binaire
ou physique. Worker série, registre JNI, snapshot disque et quarantaine doivent
être éprouvés. Les tests299 restent ceux du runtime inchangé. Aucun patch F3/Binder
importé, aucun déblocage CORE004D ; Athéna, IDENTITY et les contrats existants inchangés.

## Décision demandée et prochaine mission

Examiner puis autoriser ou refuser **ce patch précis**, identifié par son SHA-256
dans RESULTS.json, avec ses changements sensibles :

1. Propriété de la bibliothèque expérimentale sous contrôle du mainteneur CORE ;
   construction épinglée et coordonnées séparées, sans remplacer la production.
2. Registre/worker communs aux JNI anciens et nouveaux, refus de second modèle,
   contrôle de génération/KV et trois nouvelles opérations.
3. Copie privée figée du GGUF et nettoyage limité à ce snapshot, sans migration.
4. Adaptation du backend/contrôleur et sélection exclusive d'un AAR revu ; aucun
   changement du mécanisme de bail, des seuils ou des contrats v1.

Recommandation : une mission ALPHA1-003 limitée à construire et qualifier un
artefact expérimental isolé après accord sur ces points et complétion du lock ;
aucune activation dans AppGraph, aucune persona et aucune installation personnelle.
Elle doit résoudre la provenance binaire, tester les IDs réellement retenus/décodés
et les refus natifs avec un GGUF synthétique/public explicitement autorisé.
L'intégration runtime, le profil Athéna, la recette physique et toute fusion main
restent des autorisations distinctes. En cas de refus, conserver ALPHA1-001 intacte.
