# Validation CORE 004C

Résultats de cette session cloud du 17 octobre 2026. Résumé machine : `RESULTS.json`.

| Contrôle | Résultat final |
|---|---|
| Référence CORE004B | Commit exact `29646448c80568d868a702192df761fc9d7a896d` vérifié localement et sur la branche distante |
| D4-02C / LLM003E | Références lues en dépôts bare, aucune modification ou exécution de leurs sources |
| CORE historique | 187 tests, 0 échec, 0 erreur, 0 ignoré ; réexécution forcée, pas seulement cache |
| Fixture | 36 tests, 0 échec, 0 erreur, 0 ignoré ; 13 JUnit purs et 23 Robolectric API35 |
| Compilation APK historique / fixture | Réussie ; fixture non signée faute de clé commune de laboratoire |
| Lint historique | 0 erreur, 0 fatal, 30 avertissements dans le module inchangé |
| Lint fixture | 0 erreur, 0 fatal, 0 avertissement |
| Contrat canonique | 4 fichiers exacts et SHA bundle contrôlés ; vecteur inter-dépôts du digest réussi |
| Préservation du CORE | `git diff` contre la base sur `app/`, build racine et workflows vide |
| Permissions APK | Aucune INTERNET / ACCESS_NETWORK_STATE dans les deux APK ; service broker uniquement dans fixture, permission BIND_P2 |
| minSdk | 24 dans APK historique, 29 dans APK fixture ; ABI historique ARM64 inchangée |
| Instrumentation / Binder entre APK | Non exécuté ; aucun émulateur installé, pas de chemin ADB fonctionnel validé |
| Linux natif JNI/GGUF | Non exécuté dans cette mission ; résultats LLM publiés non repris comme preuve locale |
| Android JNI/GGUF réel | Non exécuté ; aucun Android de laboratoire et modèle vérifié provisionnés |
| Appareils physiques | Non exécuté ; aucune installation personnelle |
| Admission native de production | Bloquée RECOVERY_REQUIRED, pas de preuve initiale/réconciliation fournie par 003E |

ADB présent mais `adb devices -l` échoue avant détection : création de `/home/agent/.android` impossible, système de fichiers en lecture seule. Cela ne prouve pas l'absence de tout appareil physique ; aucun accès utilisable n'a été établi. Aucun émulateur ni `/dev/kvm` présent. Aucun contournement de permissions ni installation n'a été tenté.

## Commandes reproductibles

Prérequis : Java17, Gradle8.10.2, SDK35 et build-tools35, accès aux dépendances de compilation, cache utilisateur Robolectric accessible. Aucun modèle requis pour les tests isolés.

```sh
python3 tools/check-004c.py
gradle -PincludeP2Fixture=true :app:testDebugUnitTest :p2-client:testDebugUnitTest --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
gradle -PincludeP2Fixture=true :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
git diff --check
```

Les tâches combinées ont été exécutées puis les tests historiques et de fixture forcés à nouveau. Après les derniers ajouts, les 36 tests de fixture, son APK et Lint ont été réexécutés avec succès. Le module historique n'a subi aucun changement entre les passes.

Résultats XML sous `<module>/build/test-results/testDebugUnitTest/`, rapports Lint sous `<module>/build/reports/`. APK historique : `app/build/outputs/apk/debug/app-debug.apk`. Fixture sans clé : `fixtures/p2-client/build/outputs/apk/debug/p2-client-debug-unsigned.apk`. Ces fichiers de build ne sont pas suivis ni publiés comme APK. L'APK historique local ne contient pas les grands modèles : sa compilation n'atteste pas un assistant autonome utilisable.

Contrôles APK exécutés avec `aapt dump badging`, `aapt dump permissions` et `aapt dump xmltree … AndroidManifest.xml`. Les empreintes APK de cette compilation figurent dans RESULTS.json ; elles ne promettent pas une compilation binaire reproductible sur un autre poste.

## Portée des tests et corrections de validation

- Arbitre : un bail, concurrence simulée, stop distinct de fin/unload, timeout conservateur, propriétaire exact, ACK perdu, journaux échoués, redémarrage et défi frais, retour historique seulement après preuve simulée.
- Mapping : système/question séparés, epoch serveur, corpus fermé, contexte et sources refusés, JSON strict, identifiants complets, digest publié, budgets natifs bornés (réduction explicite permise, dépassement refusé), réception obsolète refusée.
- Client : génération simulée explicitement étiquetée, annulation et absence de texte assistant tardif, déchargement échoué, perte et délai ; jamais de moteur JNI dans ces tests.
- Android simulé : persistance AtomicFile réelle sur fichiers synthétiques, corruption sans reset, capture d'UID au dispatch Binder local, refus UID/package/certificat, garde du vrai Stub avant lecture d'un payload malformé, création/destruction d'Activity.

Une assertion initiale attendait à tort la propagation d'une exception depuis une transaction Binder oneway. Elle a été corrigée pour tester directement le dispatch du Stub ; le contrat oneway ne rapporte pas les exceptions à l'appelant. Ce test démontre une garde locale sous Robolectric, pas une permission imposée par le noyau Android entre deux APK. Les refus par permission signature, signatures appariées et décès réels de processus restent à qualifier en laboratoire.

## Livraison GitHub

La lecture Git fonctionne. La requête API `gh api repos/bubth720/JAVIS-Local-v2` reçoit `Forbidden` dans cette session : lecture Git et accès API sont distincts. Aucun token n'a été extrait ou remplacé. La publication Git est vérifiée séparément après création du commit. Les liens de branche/comparaison seront fournis ; ne pas présenter une URL de préparation comme une PR créée. Aucun workflow de publication de modèles lancé et aucune fusion.

### Publication effectivement vérifiée

Le commit de code `71be9ef6d3ee632c95ff9c74ffe76ede2734eb03` est publié sur
`origin/fixture/004c-p2-client` ; l'égalité locale/distante a été vérifiée par
`git ls-remote`. Une clôture documentaire peut suivre ce commit sans changement
applicatif. Le workflow existant est uniquement `workflow_dispatch` : aucun job
n'a été déclenché par ce push et aucun dispatch n'a été demandé.

Création de PR brouillon tentée avec `gh pr create`, base
`mission-004b-core-backend-lifecycle` pour isoler cette mission de la base 004B
encore non fusionnée sur main. Résultat : `Post https://api.github.com/graphql:
Forbidden`. **Aucune PR créée**, aucun lien de CI disponible. Le push Git réussi
ne démontre pas un accès API ; ce refus ne permet pas d'identifier un scope de
token manquant ou de conclure à l'absence de droits d'écriture du dépôt.

Branche publiée : <https://github.com/bubth720/JAVIS-Local-v2/tree/fixture/004c-p2-client>.
Préparation manuelle de PR incrémentale :
<https://github.com/bubth720/JAVIS-Local-v2/compare/mission-004b-core-backend-lifecycle...fixture/004c-p2-client?expand=1>.
Ce lien ouvre une comparaison, pas une PR existante. La base main doit être
choisie explicitement lors d'une future revue d'ensemble si les livraisons
précédentes ne sont toujours pas fusionnées. Ne rien fusionner automatiquement.
