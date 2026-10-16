# Validation — CORE 004B

## Livraison actuelle — admission dans le processus

Branche `mission-004b-core-backend-lifecycle`, depuis 004A `306a89b`, avec
réutilisation du commit `07cd282` puis complément d'admission process-wide.
Exécution finale du 16 octobre 2026 (XML de 20:34:18 à 20:34:43 UTC) :

```sh
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon --max-workers=4
```

**BUILD SUCCESSFUL**, 47 secondes ; 53 tâches, 15 exécutées, 38 à jour. La tâche
de tests est exécutée, les 19 suites XML contrôlées : **187 tests réussis,
0 échec, 0 erreur, 0 ignoré**. Cela comprend les 140 tests 004A inchangés,
les 34 tests de la première livraison 004B et 13 scénarios supplémentaires.

ProcessInferenceAdmissionTest couvre : plusieurs contrôleurs avec une même
admission, réservation avant préparation concurrente, délai/annulation sans
libérer le propriétaire, libération UNKNOWN/FAILED persistante après recréation
du contrôleur (y compris même backend), chargement aux ressources inconnues
sans repli, repli explicite après échec de préparation et unload confirmé,
instances historiques directes, échecs natifs simulés de load/release,
réservation continue pendant changement de contexte, confirmation d'un ancien
propriétaire sans effet sur le nouveau et admission commune par défaut aux factories. Deux cas supplémentaires protègent
la réutilisation de la même instance avec un nouveau jeton : un ancien accusé de
libération/quarantaine reste sans effet, y compris lors de la double confirmation
du moteur historique et du contrôleur après un rechargement intermédiaire.

Les tests 004B existants injectent désormais une admission privée par processus
simulé ; leurs assertions restent actives. Aucun reset du singleton de
production n'est ajouté. Les 140 tests/fixtures 004A restent identiques octet
pour octet à la référence. Tous les moteurs natifs restent simulés.

Première passe du complément : 184 cas réussis, build et Lint réussis ; la
passe intermédiaire à 185 ajoute le cas de redimensionnement et précise le
repli après échec de chargement. La passe finale à 187 protège également les
réservations successives du même backend identifiées lors de la revue finale. Aucune assertion supprimée, aucun échec masqué.

APK actuel : signature vérifiée, 7 bibliothèques ARM64, aucun modèle embarqué,
permissions INTERNET/ACCESS_NETWORK_STATE absentes. Lint : 0 erreur et 30
avertissements, catégories inchangées. Configurations SDK, stockage, réglages,
dépendances, schéma, voix et workflow inchangés. Aucun APK installé ou publié.

**CI distante non exécutée** : le workflow existant télécharge les modèles puis
publie un APK ; il ne lance pas tests/Lint. Il n'a pas été déclenché. Les
résultats ci-dessus proviennent des commandes locales réellement exécutées.
La recette native/appareil, le protocole officiel 003D/D4-02 et l'arbitrage
interprocessus restent non validés comme décrit ci-dessous.

## Première livraison 004B — résultats conservés

Base : `306a89b7cc33803aba9b7af9c6a553f1bc40f08f` sur
`mission-004a-core-stability`. Date d'exécution : 16 octobre 2026 UTC.
Java 17, Gradle 8.10.2, Android SDK 35 ; aucune dépendance modifiée.

Avant modification :

```sh
gradle :app:testDebugUnitTest --rerun-tasks --no-daemon --max-workers=4
```

BUILD SUCCESSFUL en 40 secondes, 26 tâches exécutées. Les 140 cas de référence
004A sont conservés sans modifier leurs sources ni leurs fixtures.

Validation finale après les changements :

```sh
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon --max-workers=4
```

BUILD SUCCESSFUL en 50 secondes ; 53 tâches dont 15 exécutées et 38 à jour.
La tâche de tests a été exécutée et les XML de 18 suites ont été contrôlés.

| Ensemble | Cas réussis | Portée |
|---|---:|---|
| Référence 004A inchangée | 140 | SQLite, heure, Core, cycle de vie, modèles, PDF, réglages, voix simulée |
| InferenceControllerTest | 20 | Sélection avant construction/warmup, pas de double opération, mode délégué désactivé par défaut, annulation avant dispatch/après réservation/pendant/après génération, timeout drainé, arrêt distinct de fin, anciens IDs/session refusés, release inconnue/échouée, retour historique explicite, système/contexte/sources/limites, refus et legacy exact |
| LlamaLifecycleTest | 6 | Réutilisation, système distinct, release idempotente, échec release/redimensionnement/load, annulation, OOM simulé et quarantaine |
| SessionInferenceLifecycleTest | 2 | Délai via coordinateur, arrêt transmis et réponse tardive refusée ; fermeture avec fin inconnue jamais présentée prête |
| CoreInferenceContractTest | 6 | 2 scénarios × API simulées 24/25/35 ; champs séparés, conservation données/préférences et refus avant persistance assistant |
| **Total** | **174** | **0 échec, 0 erreur, 0 ignoré** |

Les scénarios SQLite historiques comprennent migrations réussies/interrompues,
refus de version inconnue, rollback et réouverture. Ils sont réexécutés sans
modifier LocalDatabase ou le schéma. Une comparaison octet par octet à la base
confirme que tous les tests et fixtures 004A sont inchangés, ainsi que les
fichiers de stockage, réglages, modèles, PDF/RAG, Whisper, recorder, TTS,
manifeste et build.gradle de l'app.

Les passes intermédiaires ont également réussi ; aucune assertion désactivée
et aucun test supprimé pour obtenir le résultat final.

## APK, Lint et compatibilité

- `assembleDebug` réussi ; APK local seulement, non publié/installé.
- `lintDebug` : 0 erreur, 30 avertissements, mêmes catégories qu'en 004A ;
  aucune suppression Lint ajoutée, aucun nouvel avertissement NewApi.
- `apksigner verify` réussi sur l'APK reconstruit.
- `aapt dump badging` : minSdk 24, targetSdk 35 ; versions applicatives inchangées.
- `aapt dump permissions` : RECORD_AUDIO et permission interne AndroidX,
  pas d'INTERNET ni ACCESS_NETWORK_STATE.
- 7 bibliothèques ARM64 présentes ; modèles GGUF/Whisper absents de cet APK local.
- `git diff --check` réussi ; ni Gradle ni dépendance native ou manifeste modifiés.

## Simulations et non-exécutés

Le Core utilise SQLite natif sous Robolectric sur API 24/25/35. Cela reste une
exécution JVM, pas un appareil Android. Les anciens tests d'activité utilisent
les ressources Android simulées ; les nouvelles classes de contrat/contrôleur
sont testées sans Android ni modèle lourd.

`LlamaLifecycleTest` exécute la vraie classe LlamaLocalEngine avec un faux
LlamaBackend. Les compteurs de load/release, erreurs et OOM sont injectés.
Ils ne démontrent pas un chargement, arrêt ou déchargement natif effectif.
Le délégué est uniquement SimulatedEngine dans les tests ; aucune interaction
avec JARVIS LLM n'est exécutée.

Non exécutés : JNI métier et vrais modèles, application sur appareil/émulateur
ARM64, mémoire native réelle après release, arrêt natif (non fourni par le
backend historique), coordination entre APK, variante release minifiée,
installation, migration sur appareil, sauvegarde/restauration réelle. Aucun
émulateur ou KVM disponible ; aucun modèle téléchargé pour les tests. Il
n'existe pas de sources `app/src/androidTest` dans la référence.

Les textes UNIFIED D4-02 et LLM 003D n'ont pas pu être relus : références
absentes des documents disponibles. Cette validation de code ne remplace pas
leur revue contractuelle et ne déclare pas l'intégration opérationnelle.
Aucun workflow de téléchargement/publication APK lancé.
