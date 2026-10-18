# JARVIS CORE — ALPHA1-003A, patch bibliothèque soumis à revue

Base exacte : `feature/alpha1-003-native-validation`,
`84571ae6058518e75d63077dfda15a0ef60a0e46`.
Branche : `design/alpha1-003a-library-patch-review`.
Commit de livraison : `git log -1 --format=%H -- docs/alpha1-003a/RAPPORT.md`.
Cette mission prépare un document de modification ; elle n'applique aucune extension
JNI, ne construit aucune AAR modifiée et ne relance pas la phase A.

## Nouveau diff

[PATCH-LIBRARY-A-VALIDER.patch](PATCH-LIBRARY-A-VALIDER.patch), empreinte exacte dans
[PATCH.sha256](PATCH.sha256). Le même SHA figure dans RESULTS.json et STATIC-RESULTS.json.
Le patch vise exclusivement :

1. library/build.gradle.kts
2. library/src/main/jni/llama_jni.cpp
3. library/src/main/jni/measured.inc
4. library/src/main/kotlin/dev/ffmpegkit/llama/Llama.kt
5. library/src/main/kotlin/dev/ffmpegkit/llama/LlamaJNI.kt
6. library/src/main/kotlin/dev/ffmpegkit/llama/LlamaMeasured.kt
7. library/src/main/kotlin/dev/ffmpegkit/llama/LlamaModel.kt

Les cinq modifications CORE de l'ancien composite sont entièrement absentes.
llama_jni.cpp/measured.inc/LlamaJNI.kt conservent les octets candidats de la revue002 ;
les corrections003A portent sur le propriétaire/wrapper Kotlin et la version proposée
de l'artefact. Aucun de ces fichiers cibles n'est créé/modifié dans un source-set réel.

## Corrections proposées

- R01 : destination contrôlée via Context privé Android, UID/droits/répertoire et
  identité du fichier ; création exclusive sans suivre de lien, jamais à côté de la source.
- R02 : propriétaire retenu avant allocation, handle retenu avant wrapper, livraison
  annulable avec nettoyage d'abandon sérialisé ; aucune libération sur simple annulation.
- R03 : taille attendue approuvée, copie strictement bornée, EOF/fstat final/SHA ;
  croissance refusée, fichier source jamais modifié ou supprimé.
- R05 : gate avant attente, refus BUSY/réentrée/UI synchrones, callback gardé et
  attente interrompue sans annuler le travail ni rendre prématurément le propriétaire.
- R06 : fin native et fichier distingués, retry du nettoyage après handle0, conservation
  du slot en cas d'erreur ; aucun reset d'une résidence native inconnue.
- R04 : sept fichiers bibliothèque, aucune modification d'admission ou dépendance CORE.

La [matrice de risques](RISK-MATRIX.md) précise les limites de chaque proposition.
Le [contrat/retour arrière](CONTRACT-AND-ROLLBACK.md) décrit les restrictions à approuver.

## Vérifications réellement effectuées

| Contrôle | Résultat / limite |
|---|---|
| Régénération déterministe et reconstruction des hunks en mémoire |7 fichiers exacts,0 hunk CORE |
| Contrôles de structure |26 réussis ; présence/ordre/périmètre, pas preuve formelle des invariants |
| Tests du vérificateur et mutations |22 réussis,0 échec/erreur/ignoré ; ne font tourner aucun modèle candidat |
| Contrôle à blanc `git apply --check` |Réussi sur sources amont intactes ; aucune application |
| Types Kotlin2.0.21/coroutines1.9.0/SDK35, cible JVM11 |Réussis sur sept fichiers Kotlin de revue isolés, aucun C++/JNI compilé ou chargé |
| Signatures JVM historiques |Llama9, LlamaJNI6, LlamaModel6 déclarations publiques toutes présentes, y compris bridge synthétique |
| Préservation de la base84571ae |187 fichiers identiques ; anciens documents/protections/dépendances intacts |

Échecs intermédiaires corrigés dans la proposition : O_DIRECTORY et Os.unlink non
exposés par l'API publique Android ; remplacés par open O_RDONLY/O_NOFOLLOW avec
fstat répertoire, et Os.remove après vérification du fichier régulier détenu.
Le changement de forme des lambdas supprimait un bridge JVM public synthétique :
il est maintenant maintenu explicitement. Aucune assertion de compatibilité retirée.

Le contrôle Kotlin écrit seulement des fichiers .kt de revue/classes JVM hors du
checkout et hors des sources épinglées. Pas de C++/measured.inc matérialisé, ni build
Gradle Android du candidat, ni AAR liée. Les logs de résultats sont dans EVIDENCE.md.
Les scripts proposal.py/review.py génèrent/vérifient des chaînes et documents ;
typecheck.py ne compile que Kotlin ; test_review.py teste les contrôles statiques.

Le contrôle Git des espaces hors artefact patch réussit. Le contrôle brut retourne2
sur les lignes de contexte vides préfixées d'une espace dans le diff unifié : ces
espaces font partie de sa syntaxe, son empreinte est conservée. Aucune assertion
ou règle Git permanente n'est désactivée pour masquer ce résultat.

## Non exécuté et gate

Les builds amont,299 tests CORE/fixture, Lint et autres preuves de la phase A restent
historiques : **non relancés**. Aucun C++ candidat, JNI/GGUF, comptage exact réel,
test de concurrence/annulation/stockage Android du candidat, appareil/émulateur,
installation ou activation Athéna n'est exécuté. Aucun remplacement de l'AAR0.1.1.

Les garanties proposées restent à qualifier. Les signatures sont conservées mais
les refus main-thread/BUSY/réentrée changent le contrat d'exécution ; ils doivent
être explicitement acceptés avant application. La garantie filesystem exclut les
attaques du même UID/root ; les erreurs d'identité/IO et états natifs inconnus bloquent.
Ce propriétaire local ne prouve aucun unload pour VOICE/Whisper ou un autre APK.
F3 et CORE004D/Binder/2 restent bloqués ; aucun changement IDENTITY, profil ou SQLite.

## Publication et prochaine décision

Seuls les documents et outils de revue de docs/alpha1-003a sont ajoutés à cette branche.
Les documents ID-002N préexistants sont préservés hors livraison. Aucune PR, fusion,
installation, modification d'autre dépôt ou publication d'AAR/modèle. Le SHA du commit
et la confirmation distante sont fournis dans le compte rendu utilisateur.

**Arrêt après publication.** Prochaine action : validation/refus par l'utilisateur
du nouveau SHA et des sept fichiers, puis autorisation explicite distincte avant
toute phase B. Ce rapport et un message « reprends » ne constituent pas cet accord.
