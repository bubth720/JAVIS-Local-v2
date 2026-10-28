# CORE-PF-02 — port de lecture CORE→UI, lot 3

## Références et périmètre

Branche de livraison : `codex/prefusion-lot3-core`, PR brouillon prévue vers
`mission/prefusion-lot3-20261027`, jamais vers main.
Base mission vérifiée : `af6fe97e2d3f1cded76a4c0e28754b2c21272692`, parent
`a3113797a3c481bc516c5ef1fd4d7a74b919de40`. Le SHA final est celui du commit
contenant ce rapport ; vérifier avec `git rev-parse HEAD` et le SHA distant.

Le port Kotlin `ReadOnlyCoreUiLabPort` est exécutable par construction explicite
de fixture. Il lit le véritable type `EventSnapshot` du bus existant, conserve les
identifiants et séquences, et expose les diagnostics pinned existants. Aucun appel
de production ne le construit : **OFF par défaut, PROPOSED / intégration BLOCKED**.
Un opt-in synthétique ne peut produire que LABORATORY / UNTRUSTED ; jamais READY,
permission, réponse documentaire, citation, preuve JNI ou preuve système UID.

Fichiers : nouveau port et test sous `coreui/lab/`, garde
`tools/check-prefusion-lot3.py`, workflow `prefusion-lot3-core.yml`, ce dossier et
appendices d'instructions AGENTS/PROJECT_KNOWLEDGE. Aucun fichier applicatif
historique n'est modifié. Voir `IMPACT.md` et `PROTOCOL-REVIEW.md`.

## Protections et limites

Le banc refuse version inconnue, payload usurpant identité ou READY, identité
synthétique incohérente/périmée, mauvaise epoch/session/génération, séquence trouée,
replay, modification rétroactive et perte de fenêtre. Annulation avant ACK et
process death sont terminaux pour l'instance ; aucune reprise/reset implicite.
Une liste de plusieurs requêtes ou une séquence source préexistante incompatible
est refusée : ce prototype n'est pas un adaptateur de routage multi-session.

L'epoch locale n'atteste pas une epoch du bus, lequel n'en possède pas. Les
certificats sont des hashes fictifs comparés en mémoire, pas une chaîne X.509
Android vérifiée. La proposition d'adaptateur Android reste documentaire. Aucun
Binder, service exporté, permission, signature, admission, moteur, modèle, budget,
SQLite, préférence, persona, voix ou fixture /2 n'est modifié.

Les anciennes bibliothèques réellement consommées dans `integration-lab` restent
NEXUS `777e6afa75ecb99b85e52d68837cb0fefcf161cd` et SERVICES
`3decdc47d82f9b43713c48cab244762941770a81`. Les références pinned lot2 sont
diagnostiques ; elles ne remplacent pas ces snapshots JVM.

## Reproduction

JDK17, Gradle8.10.2, SDK35 ; pas de téléchargement de modèle. Environnement cloud
préparé hors checkout : `/workspace/.lot3-tools/env.sh core gradle ...`.

```sh
python3 tools/check-prefusion-lot3.py
gradle :app:testDebugUnitTest --tests '*ReadOnlyCoreUiLabPortTest*' --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
gradle -p integration-lab -PcoreNexusSource=/tmp/core-lot3-dependencies/nexus -PcoreServicesSource=/tmp/core-lot3-dependencies/services test run --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
gradle -PincludeP2Fixture=true :app:testDebugUnitTest :app:testMvp2UnitTest --tests '*Mvp*' --tests '*MemoryLabUiTest*' :p2-client:testDebugUnitTest :app:assembleDebug :app:assembleMvp2 :app:assembleMvp3 :app:lintDebug :app:lintMvp2 :app:lintMvp3 --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
python3 tools/check-prefusion-lot3.py --results
```

Les snapshots doivent être préparés par `tools/prepare-core03-sources.py` et
vérifiés par son manifeste ; ne pas remplacer ces chemins par les worktrees de
développement producteurs. La CI fournit ses propres répertoires RUNNER_TEMP.
Le premier ciblage est une démonstration des 27 cas ; ne pas additionner son
résultat aux mêmes cas rejoués par la suite complète.

Les résultats terminaux réellement obtenus sont consignés dans
`evidence/VALIDATION.json` et les logs compressés ; les preuves du lot précédent
restent historiques. Les gardes anciens au périmètre absolu ne sont pas assouplis
et ne sont pas substitués au garde lot3. Aucun test Android instrumenté ou essai
physique, JNI natif, transport authentifié, PSS/batterie/thermique n'est revendiqué.

Exécution locale du 28 octobre 2026 : **629 cas réussis, 0 échec, 0 erreur,
0 test ignoré** : Debug510 (dont27 nouveaux), MVP2 53, fixture36 et laboratoire
JVM30. Les602 cas historiques sont conservés, ce que le garde vérifie par noms.
Le CLI JVM a réellement exécuté recherche NEXUS/citation, somme CSV0.30 et
export DOCX/relecture sur fichiers fictifs ; Android connecté et inférence native
restent faux. Le ciblage27 du 27 octobre n'est pas additionné une seconde fois.
L'APK Debug conserve minSdk24/target35, est signé debug et ne déclare ni INTERNET
ni ACCESS_NETWORK_STATE ; les contrôles APK ne prouvent pas son fonctionnement
physique. Les totaux Lint et empreintes du code testé sont dans VALIDATION.json.
Builds Debug/MVP2/MVP3 et Lint correspondants réussis ; Lint : zéro Error/Fatal,
27 avertissements Debug, 37 MVP2, 37 MVP3. La campagne Android de compilation,
JVM/Robolectric et Lint a pris20m45s à cache partiellement froid, sans constituer
une mesure des performances d'un téléphone. Les trois APK restent minSdk24,
sans permission réseau, signés debug, sans poids GGUF/Whisper embarqués.

## Retour arrière et décisions

La création de PR par API est bloquée dans l'environnement partagé : réponse
`Post https://api.github.com/graphql: Forbidden`, observée par le lot INTERFACE.
Aucune nouvelle tentative équivalente ni contournement n'est effectué ici.
Après publication Git, créer manuellement une **PR brouillon**, base
`mission/prefusion-lot3-20261027`, head `codex/prefusion-lot3-core` :
<https://github.com/bubth720/JAVIS-Local-v2/compare/mission%2Fprefusion-lot3-20261027...codex%2Fprefusion-lot3-core?expand=1>.
La publication Git et les résultats Actions doivent être vérifiés séparément ;
une compilation locale n'établit ni l'existence d'une PR ni la réussite d'Actions.

Revert des seuls ajouts de la branche après revue ; aucune donnée à migrer.
Pour une connexion réelle, valider transport/identité système, autorité de session,
mapping consommateur UI et règles de restitution. Le présent lot n'autorise pas
ces activations, ni un export SAF, ni un backend FTS5, ni l'ADR-002.
Prochaine action : revue des résultats au SHA exact, puis mission bornée de
contrat consommateur avant tout adaptateur Android actif.
