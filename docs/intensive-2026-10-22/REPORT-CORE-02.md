# CORE-02 — bus local livré

Dépôt `bubth720/JAVIS-Local-v2`, branche `feat/core-02-intensive`, parent effectif
CORE-01 `b98bd1c4722cd053021d4021717b73ff595d5fed`, base de fiche
`a6f13dd101990c6b5e3992be506c9a5b99873621`. UNI-01 vérifiée au commit
`d947dadf73f8dec91543a3f86a1aa2ffef0b0d2f` ; contrats wire v1 inchangés.

## Code et utilisation

`events/CoreEventBus.kt` expose événements typés, enveloppes UUID/version1,
ordre atomique, replay64 et déduplication sur les256 derniers identifiants.
L'overflow est observable. Désactiver/activer change la génération et refuse les
anciennes traces. StateFlow notifie sans moniteur externe ; les observations
ne commencent jamais une opération et ne réservent aucune ressource native.

`CoreSessionEvents.kt` relie la vraie SessionCoordinator à l'entrée, réflexion,
réponse disponible, erreur, demande d'annulation et fin d'annulation. Le suivi
est désactivé par défaut : l'hôte peut appeler `graph.events.setEnabled(true)`
puis collecter `graph.eventUi.state` selon son cycle de vie. Les UUID corrèlent
**l'opération métier CORE**, pas un bail ni une requête JNI. Aucun contenu de
conversation, chemin, prompt ou droit dans ces événements. RESPONSE_CHUNK et
SPEECH_REQUESTED sont des types disponibles, non émis comme faux streaming/voix.

`ModuleConnector.kt` fournit discover/connect/disconnect/health ; VOICE,
INTERFACE, NEXUS et SERVICES restent DISABLED, ou UNAVAILABLE après activation
sans implémentation. Aucune connexion fictive, service exporté ou IPC ajouté.
Un journal optionnel borné en RAM et sa projection diagnostique expurgée donnent
des durées quand les deux bornes ont été observées ; aucun fichier automatique.

## Validation réelle (23 octobre 2026)

La commande exacte figure dans `VALIDATION-CORE-02.json`.
`BUILD SUCCESSFUL in 3m 16s`,167 tâches :29 exécutées/138 up-to-date.
**348 tests réexécutés**,304 CORE debug et44 MVP002 ; zéro échec/erreur/ignoré.
La fixture /2 reste36 cas UP-TO-DATE, non comptés comme réexécutés.
Debug/MVP002/MVP003 compilent ; Lint debug/MVP002 passe avec0 erreur et30/40
avertissements. Certains résultats Lint sont réutilisés par Gradle.

Les22 nouveaux cas couvrent ordre, déduplication, overflow, concurrence réelle
entre threads, génération révoquée, anciennes réponses, journal borné, refus des
connecteurs, resouscription sans relance et drainage. Trois scénarios sont rejoués
sur API24/25/35 : vraie commande `/docs` via JarvisCore/SQLite sans appel modèle,
annulation pendant worker simulé, observation désactivée. Les autres tests sont
JVM purs. Cette démo peut se relancer par :

```sh
bash tools/identity-cloud-env.sh gradle :app:testDebugUnitTest --tests '*CoreEventBusTest*' --tests '*CoreSessionEventsTest*' --offline --no-daemon --max-workers=2
python3 tools/check-core-02.py
```

Le garde protège309 fichiers de CORE-01, dont moteurs, admission, SQLite,
paramètres, IDENTITY et fixtures ; `git diff --check` passe. Aucun changement JNI,
SDK, permissions, signatures, modèle ou budget. Aucun test natif, essai physique,
installation ou validation inter-APK annoncé. La CI CORE-02 est préparée, pas
présumée réussie. CORE-01 a une CI distante SUCCESS vérifiée :
https://github.com/bubth720/JAVIS-Local-v2/actions/runs/39915867662 au SHA b98bd1c.

Le reçu CORE-01 ajouté sur cette branche conserve la commande et les425 tests
réexécutés antérieurement ; il ne réinvente pas les36 résultats fixture en cache.
Suite : UI-01 peut consommer cette API locale ; toute connexion inter-APK demeure
une autorisation distincte. ID-01 puis MEM-01 peuvent avancer sans toucher aux
contrats figés, au tokenizer ou aux données historiques.
