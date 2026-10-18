# ALPHA1-002 — plan de tests et preuves réellement obtenues

## Résultats de cette mission

| Contrôle | Résultat | Niveau de preuve |
|---|---|---|
| Base b2ec982 locale/distante | Identique | Git |
| AAR/7 composants et checksum Maven | Identiques aux empreintes épinglées | Lecture binaire, HTTP200 |
| Tag v0.1.1/commit/gitlink | Vérifiés | Git, pas signature de build |
| Signature tag amont | **Absente**, verify-tag exit1 | Limite de confiance explicite |
| 31 fichiers source | SHA-256 vérifiés | Sources, pas équivalence binaire |
| Patch12 fichiers | Reconstruction + git apply --check réussis | Contrôle à blanc, jamais application historique |
| Préservation153 fichiers de base | Identiques | Exception documentaire AGENTS/connaissance uniquement |
| Wrappers Kotlin proposés | Contrôle de types réussi | Kotlin2.0.21/JDK17/android.jar35, pas build AAR |
| Appels CORE proposés | Contrôle de types réussi | Classes CORE inchangées comme dépendances, pas APK expérimental |
| Signatures JVM historiques | Comparaison statique réussie | Module Kotlin library_release épinglé ; ne qualifie pas le binaire Android |
| C++ proposé | Syntaxe réussie | g++14.2 + vrais headers llama.cpp ; stub déclaration log Android seulement |
| Budget et UTF-8 proposés | **4214 vérifications pures réussies** | 4200 combinaisons budget +14 chaînes ; aucun tokenizer ou JNI chargé |
| CORE inchangé / fixtures | **299 tests réussis**, 0 échec/erreur/ignoré | 263 app +36 fixture, JVM/Robolectric dont API24/25/35 |
| Builds/Lint actuels | Réussis | 27 avertissements app,0 erreur ; fixture0 avertissement |

La nouvelle exécution Gradle a utilisé --rerun-tasks :107 tâches exécutées,
BUILD SUCCESSFUL en1m36s. Les nombres sont conservés dans RESULTS.json avec les
classes XML, pas inférés de l'exit code. Aucun nouveau test de production supprimé,
modifié ou désactivé. Les tests299 portent sur ALPHA1-001 **sans** le patch natif.

## Reproduire les vérifications non sensibles

Racine CORE, outils cloud existants via tools/identity-cloud-env.sh :

```sh
bash tools/identity-cloud-env.sh gradle -PincludeP2Fixture=true \
 :app:testDebugUnitTest :p2-client:testDebugUnitTest \
 :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
 --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
```

Les scripts suivants n'effectuent pas de téléchargement, application de patch ou
chargement de JNI. Fournir les snapshots sources exacts et l'AAR déjà résolue :

```sh
python3 docs/alpha1-002/verify_review.py \
 --upstream /tmp/alpha1-002-review/llama-android-823d1de2b3ef85ba9ef4847a03bac423f4aa1c44 \
 --aar /workspace/.jarvis-core-tools/gradle-home/caches/modules-2/files-2.1/dev.ffmpegkit-maintained/llama-android/0.1.1/1b68382aa058fb10062268f56090731a51ab6608/llama-android-0.1.1.aar \
 --scratch /tmp/alpha1-002-review
```

Le JSON affiche un répertoire temporaire propre `scratch`. Passer ce chemin réel
comme --review-root (les noms aléatoires ne sont pas une configuration pérenne) :

```sh
python3 docs/alpha1-002/typecheck_review.py \
 --review-root CHEMIN_SCRATCH_RETOURNE \
 --upstream /tmp/alpha1-002-review/llama-android-823d1de2b3ef85ba9ef4847a03bac423f4aa1c44 \
 --llama /tmp/alpha1-002-review/llama.cpp-2da668617612d2df773f966e3b0ee22dc2beef7b \
 --tool-root /workspace/.jarvis-core-tools
git diff --cached --check -- . ':!docs/alpha1-002/PATCH-A-VALIDER.patch'
```

Le vérificateur reconstruit les candidats par analyse de hunks dans scratch et
lance uniquement git apply --check sur les originaux isolés. Aucun git apply réel.
Le second vérifie les hashes et les signatures JVM des candidats avant compilation isolée
(CLI Kotlin module library_release pour conserver le nommage interne) ; g++ ne lie
pas llama.cpp dans l'exécutable de tests purs (sections inutilisées éliminées).
Le stub android/log.h n'implémente aucun modèle ou tokenizer.

Les anciens gardes absolus check-004c.py/check-identity-002.py étaient en échec
sur la base ALPHA1-001. Le garde ALPHA1-001 limite aussi ses ajouts à son propre
dossier. Ils ne doivent pas être assouplis pour cacher ces incompatibilités de
périmètre ; les résultats de leur nouvelle exécution sont consignés dans RESULTS.json.
Le nouveau vérificateur compare la base exacte de cette mission et protège le runtime.

## Campagne requise après autorisation — NON EXÉCUTÉE

Corpus purement synthétique, modèle GGUF autorisé et hash connu, aucun contenu
personnel. Instrumentation de test isolée pour tracer identités/compteurs/digests,
jamais prompts ou tokens personnels en logs. Aucun succès simulé comme preuve native.

| ID | Scénario futur | Critère de réussite / preuve attendue |
|---|---|---|
| N01 | Build double propre ARM64/API24 | Lock complet, hashes et différences expliquées, dépendances vérifiées |
| N02 | Chargement mesuré | Un modèle/un contexte, hash de copie figée attendu, même bail acquis avant load |
| N03 | UTF-8 ASCII/accents/CJK/emoji/NFC/NFD | IDs du tokenizer réel et rendu exacts ; aucune normalisation implicite |
| N04 | NUL/surrogates/UTF-8 tronqué/invalide | Refus avant decode, buffers abandonnés, pas fallback |
| N05 | SYSTEM + question/historique/sources | Template/roles/prefill/BOS mesurés, provenance conservée dans binding |
| N06 | Template absent/inconnu/SYSTEM ignoré | Refus ou échec qualification template, jamais promotion SYSTEM en user |
| N07 | Capacité exacte et +1 token | Égalité admise, dépassement refusé AVANT decode ; même contexte effectif |
| N08 | Réserves négatives/overflow/sortie0/257 | Refus fermé, aucune hausse de limite ou approximation |
| N09 | prepare sans effets KV | Compteurs decode/clear/sampler/load/context identiques avant/après ; vrai backend |
| N10 | consume | Séquence complète des IDs préparés identique aux IDs réellement préremplis, même thread/génération/bail |
| N11 | Double consommation ou mauvais ticket | Refus ; une tentative consomme définitivement, pas d'autre génération |
| N12 | Autre modèle/reload/contexte/ABA | Ticket périmé refusé, génération différente même adresse/bail réutilisés |
| N13 | Expiration native et révocation CORE | Refus, pas refresh/réemploi ; état connu ou quarantaine conforme à la phase |
| N14 | Changement session/epoch/policy avant admission | Callback refuse, discard confirmé, aucune génération |
| N15 | Annulation avant dispatch/admission et pendant JNI | Zéro entrée avant admission ; après admission drainage puis sortie tardive rejetée |
| N16 | Deux callers/prepare/embed/release concurrents | Sérialisation ; aucun second modèle/context ; pas deadlock/réentrée |
| N17 | Worker mort ou remplacé | Refus WRONG_WORKER, aucun transfert implicite ou reset |
| N18 | Decode/OOM/sampler/output UTF-8 incomplet | Erreur explicite, pas réponse partielle réussie ; bail conservé si état inconnu |
| N19 | Discard normal/répété/incorrect et nettoyage échoué | Buffers exacts détruits ; unload toujours distinct ; échec => quarantaine |
| N20 | Snapshot disque plein/crash/source mutée | Hash attendu sinon refus ; source originale conservée ; pas nettoyage global |
| N21 | Release/reload et ticket perdu | Release attestée par chemin existant avant changement ; perdu => blocage, jamais reset à chaud |
| N22 | Graphe Gradle/APK | Un AAR/ensemble ELF Llama, pas second moteur, minSdk24 et absence Internet |
| N23 | ALPHA1-001/IDENTITY/SQLite/préférences/VOICE | Toutes suites rejouées avec nouvel AAR, contrats/manifeste/persona off, données fictives inchangées |
| N24 | Fixture /2 |36 cas et hashes canon inchangés ; aucun déblocage de004D |
| N25 | Android ARM64 API24/25 puis matériel autorisé | JNI réel, mémoire/température/batterie/annulation/offline mesurés séparément du JVM/Linux |

Les doubles de tests peuvent vérifier l'ordre des appels, pas N02/N03/N09/N10/N25.
Les 4214 cas actuels ne sont pas4214 tests JNI et ne valident pas le comptage du
modèle. Aucun modèle, appareil/émulateur, native prefill/génération, nouvelle AAR,
conversation réelle ou personnalité Athéna qualifiés dans cette mission.

## Espaces du fichier patch

Le contrôle brut `git diff --cached --check` signale les lignes de contexte vides
préfixées par une espace dans le fichier patch unifié (exit2). Ces espaces sont
la syntaxe du patch, conservée pour son intégrité. Le contrôle des autres fichiers
staged, avec exclusion de ce seul artefact, réussit ; les douze candidats reconstruits
sont également vérifiés sans espaces finaux ajoutés. Aucune règle Git permanente,
assertion ou test n'a été désactivé pour masquer cet avertissement de représentation.
