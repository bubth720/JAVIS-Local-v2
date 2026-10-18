# ALPHA1-003 — banc non sensible et campagne future

## Tests exécutés en phase A

1. `python3 tools/alpha1-003/trace_oracle.py` :25 tests d'un oracle de spécification
   sur traces synthétiques, zéro erreur/échec/ignoré. Cas : propriétaire unique,
   worker/génération/bail, ticket/binding, expiration, double consume, SYSTEM distinct,
   absence de mutation KV/rerendu/retokenisation, égalité de séquence et batches,
   budget, annulation avant admission et sortie tardive, distinction discard/unload,
   native toujours actif après annulation, inconnue/quarantaine, copie/hash exigés.
2. `cancellation_probe.kt` : vrai dispatcher coroutines1.9.0, worker/latches JVM,
   ressource **factice**. Annulation avant retour withContext : la valeur n'est pas
   livrée. Une borne de5s est un timeout du test, aucune preuve de libération native.
3. Audit en mémoire du patch/provenance, préservation de167 fichiers.
4. Builds amont propres et contrôles du lock décrits dans BUILD-REPRODUCIBILITY.md.
5. CORE263 + fixture36 tests JVM/Robolectric, builds et Lint, sur l'AAR historique.

L'oracle ne simule pas un moteur et n'atteste pas les compteurs JNI : il vérifie
la cohérence d'événements déclarés. Une future instrumentation devra enregistrer
indépendamment le **vrai** propriétaire, le worker et les IDs passés à llama_decode.
Ne jamais envoyer ses traces synthétiques à l'admission ou à un arbitre Binder.

## Reproduction actuelle

```sh
python3 tools/alpha1-003/verify_phase_a.py audit \
 --upstream "$UPSTREAM_SOURCE" --llama "$LLAMA_SOURCE" --aar "$AAR_011"
python3 tools/alpha1-003/trace_oracle.py
# JDK17, compilateur Kotlin2.0.21 et coroutines1.9.0 du lock, sans ajout de dépendance :
# compiler cancellation_probe.kt vers un JAR hors checkout ; lancer Cancellation_probeKt.
gradle -PincludeP2Fixture=true :app:testDebugUnitTest :p2-client:testDebugUnitTest \
 :app:assembleDebug :p2-client:assembleDebug :app:lintDebug :p2-client:lintDebug \
 --rerun-tasks --no-daemon --max-workers=4
python3 tools/check-004c.py
python3 tools/check-identity-002.py
python3 tools/check-alpha1-001.py
```

Utiliser l'environnement JDK/SDK/CA/proxy vérifié. Les trois dernières commandes
échouent actuellement sur leurs périmètres historiques, et doivent rester visibles.
Les4214 cas purs ALPHA1-002, ses typechecks et son C++ de revue sont historiques,
**non réexécutés** ici : aucun candidat natif n'a été matérialisé/compilé en phase A.

Pour le probe, appel du compilateur CLI :

```sh
"$JAVA_HOME/bin/java" -cp "$KOTLIN_COMPILER_CLASSPATH" \
 org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 11 \
 -classpath "$KOTLIN_STDLIB:$COROUTINES_190" \
 -d "$WORK/cancellation-probe.jar" tools/alpha1-003/cancellation_probe.kt
"$JAVA_HOME/bin/java" -cp "$WORK/cancellation-probe.jar:$KOTLIN_STDLIB:$COROUTINES_190" Cancellation_probeKt
```

KOTLIN_COMPILER_CLASSPATH comprend compiler-embeddable/stdlib/script-runtime/
daemon-embeddable2.0.21, reflect1.6.10, trove4j, annotations13.0 et coroutines1.9.0,
résolus dans le cache vérifié. Les artefacts doivent correspondre aux hashes du lock.

## Après validation distincte du patch révisé — NON EXÉCUTÉ

Reprendre N01–N25 de docs/alpha1-002/PLAN-TESTS.md ; compléter :

| Groupe | Cas à exécuter avec le vrai candidat autorisé | Preuve exigée |
|---|---|---|
| Provenance/ABI | Double build, JNI anciens/nouveaux, appels Kotlin, graphes AAR/ELF | Manifestes, SHA, exports ; aucun second ensemble natif |
| Modèle | Un load/contexte, reload, même adresse réutilisée, générations, mauvais handle | Compteurs réels, même propriétaire/bail ; refus des anciens tickets |
| Préparation | SYSTEM/question distincts, template autorisé, BOS/prefill, UTF-8 multilingue | Rendu réel et IDs exacts ; absence decode/KV/sampler/load durant prepare |
| Encodage | NUL, surrogate isolé, UTF-8 tronqué, emoji, NFC/NFD | Refus explicite ou IDs qualifiés ; aucune normalisation silencieuse |
| Contexte | Capacité exacte, +1token, sortie0/257, réserves/overflow | Refus AVANT decode, aucune hausse de budget |
| Consume | Plusieurs batches, sortie courte réelle, ancien ticket, double tentative, binding changé | Séquence des IDs préparés = concaténation des IDs de prefill réellement décodés |
| Concurrence | Deux callers, prepare/embed/complete/release, callback réentrant, worker remplacé | Ordre vérifié par latches/observations ; aucun deadlock, double load ou transfert implicite |
| Annulation | Avant dispatch/load, pendant copie, après allocation avant livraison, avant/après admission, JNI bloqué | Aucun handle orphelin ; drainage après admission ; aucune sortie SQLite/UI/TTS tardive |
| Erreurs natives | OOM, JNI pending exception, sampler/decode/output incomplet, erreur release | Refus/état inconnu explicite ; réservation conservée si libération non attestée |
| Snapshot | Destination non privée/symlink, fichier croissant, disque plein, hash erroné, interruption | Copie bornée, source intacte, propriété du seul snapshot ; aucun nettoyage global |
| Fin | Discard exact/répété/mauvais, fin génération, unload, suppression fichier échouée | Quatre états distincts ; nettoyage fichier ne confère pas de droit de recharger |
| Régressions | ALPHA1-001, SQLite/préférences/PDF/RAG/Whisper/TTS/IDENTITY, fixtures36 | Données synthétiques conservées ; aucun contrat/profil activé |
| Android | ARM64 API24/25 puis laboratoire autorisé, pression mémoire et cycle de vie | JNI/Android réels séparés du JVM/Linux ; mesures sans changer les budgets |

Un GGUF non déjà autorisé, toute installation physique et toute activation d'Athéna
nécessitent leurs autorisations propres. Aucun modèle n'est téléchargé par ces scripts.
Un test JNI Linux, un mock, l'oracle ou l'APK compilée ne prouvent pas un fonctionnement
sur POCO/Redmi. CORE004D/Binder/2 n'est ni testé positivement ni débloqué par ce banc.
