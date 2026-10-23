# CORE-01 — orchestrateur local livré

- Dépôt : `bubth720/JAVIS-Local-v2` ; branche `feat/core-01-intensive`.
- Base vérifiée : `a6f13dd101990c6b5e3992be506c9a5b99873621`.
- Fiche lue au commit de planification `1b7ac9b35b5e6e133167fcaf1873e58b9db7288e` ; son diff ne contient que huit fiches Markdown.
- Validation logicielle exécutée le 22 octobre 2026 ; clôture le 23 octobre.
- Aucun merge, installation, téléchargement de modèle ou changement natif.

## Fonction livrée

`orchestration/` contient IntentEnvelope/IntentContext, StrategyRegistry,
IntentRouter, permissions hôte indépendantes du contenu et TaskCoordinator.
Les requêtes ont UUID, catégorie, origine déclarative, contexte documentaire
explicitement sélectionné, confirmation et deadline monotone locale. Ce contrat
est **in-process**, distinct des contrats wire UNIFIED et des fixtures /2.

Le routeur utilise les vrais LocalDatabase, RagEngine et l'horloge CORE pour
historique, liste de documents, recherche sourcée et heure. Aucun moteur n'est
créé par ce package. Les sorties tardives sont rejetées après annulation/délai ;
la tâche reste occupée jusqu'au retour effectif de l'adaptateur. Ceci ne prouve
ni arrêt JNI ni unload. Quota256 IDs acceptés par façade, journal de64 états
maximum uniquement RAM, pas d'auto-retry/reset. Les demandes inconnues,
concurrentes, rejouées, expirées ou non autorisées sont des refus typés.

La route conversation est branchable sur chatVolatile mais **indisponible dans
la composition livrée** faute de qualification native. Pas de réponse simulée.
Le filtre documentaire s'applique aux dix premiers hits du RagEngine historique ;
il peut donc omettre des hits moins bien classés, sans révéler une source exclue.

## Utilisation réelle de la démo

Dans la variante MVP002 ou MVP003, ouvrir « Voix, souvenirs et capacités »,
cocher explicitement le laboratoire d'intentions puis choisir historique,
documents, recherche ou heure. Ces quatre actions empruntent `MvpRuntime.inspect`
et donc le gate opérationnel existant. Résultats et états corrélés restent en
RAM ; aucune nouvelle écriture d'historique, de souvenir ou de préférence.
Le mode est désactivé par défaut, sans nouvel écran principal ni moteur parallèle.

Démo JVM reproductible, sans installation :

```sh
bash tools/identity-cloud-env.sh gradle :app:testDebugUnitTest --tests '*CoreIntentRouterTest*' --tests '*TaskCoordinatorTest*' --no-daemon --max-workers=2
```

Le test de quatre commandes utilise une vraie SQLite synthétique sous
Robolectric API24/25/35 et affiche uniquement les nombres de résultats :
`history=1, documents=1, search=1, time=1; no native inference`.

## Vérifications réellement obtenues

Commande complète :

```sh
bash tools/identity-cloud-env.sh gradle -PincludeP2Fixture=true -Dorg.gradle.jvmargs='-Xmx1536m -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest :p2-client:testDebugUnitTest :app:testPreviewUnitTest --tests '*PreviewActivityTest*' :app:testMvpUnitTest --tests '*Mvp*' :app:testMvp2UnitTest --tests '*Mvp*' :app:testMvp3UnitTest --tests '*Mvp*' :app:assembleDebug :app:assembleMvp :app:assembleMvp2 :app:assembleMvp3 :app:lintDebug :app:lintMvp2 :app:lintMvp3 :p2-client:assembleDebug :p2-client:lintDebug --no-daemon --max-workers=2
```

`BUILD SUCCESSFUL in 8m 52s`, 274 tâches :166 exécutées/108 up-to-date.

| Suite | Tests XML | Exécution de cette commande | Échecs/erreurs/ignorés |
|---|---:|---|---|
| CORE debug |282|Exécutée|0/0/0|
| Preview ciblée |12|Exécutée|0/0/0|
| MVP001 ciblée |72|Exécutée|0/0/0|
| MVP002 ciblée |44|Exécutée|0/0/0|
| MVP003 ciblée |15|Exécutée|0/0/0|
| Fixture /2 |36|UP-TO-DATE : résultat réutilisé, entrées inchangées|0/0/0|

Donc **425 tests réexécutés**, plus36 résultats de fixture réutilisés par Gradle,
soit461 résultats validés. Les22 nouveaux cas incluent19 domaine/SQLite et3 UI.
Lint debug/MVP002/MVP003 :0 erreur, respectivement30/40/40 avertissements.
Lint fixture : tâche validée UP-TO-DATE. La commande ne relance pas Lint Preview.

`python3 tools/check-core-01.py` protège299 fichiers historiques : moteurs,
admission, schéma SQLite, paramètres, manifests, contrats et fixtures inchangés.
`git diff --check` passe. Les anciens gardes absolus restent intacts et ne sont
pas remplacés par une assertion assouplie ; le nouveau garde porte la base exacte
et les seuls changements autorisés pour cette mission.

Inspection APK MVP002 via aapt : package `com.stark.jarvislocal.mvp002`,
minSdk24/target35, aucune permission INTERNET/ACCESS_NETWORK_STATE/RECORD_AUDIO.
Sept bibliothèques ARM64, aucun asset GGUF/Whisper embarqué ; JNI llama SHA256
`4d139d2d95569d8d9336c9687f70eb4b5b8d2daf23ca4cfa7ce5a75721c8de20`, inchangé.
Le workflow `core-01-intensive.yml` est limité à cette branche, contents:read,
sans modèles ni déploiement. Sa présence n'est pas une preuve de succès CI.

## Limites et suite

Aucun GGUF chargé, appel JNI réel, essai acoustique, émulateur ou appareil physique.
Pas d'activation Athéna, Binder, VOICE ou NEXUS. Une confirmation déclarée dans
l'enveloppe n'est pas une autorisation ; seules les capacités admises par l'hôte
peuvent être appelées. Les adaptateurs livrés ne comportent aucune action mutante.

Prochaine mission : CORE-02, dépendante de CORE-01 et UNI-01 effectivement livrées.
La référence finale à transmettre est le SHA du commit de cette branche obtenu
par `git rev-parse feat/core-01-intensive` ; aucun SHA auto-référentiel inventé ici.
