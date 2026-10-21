# Livraison MVP-004 — préparatifs NON QUALIFIÉS

Module JARVIS CORE ; branche test/mvp-004-native-qualification.
Commit de fabrication/préparatifs : `c10444e8f78047b593aebc8eab37ed7f6e01b859`.
Un commit documentaire suivant ajoute les preuves APK/publication, sans
modification applicative. PUBLICATION.json confirme la publication initiale.

APK candidate locale : JARVIS-MVP-003.apk, package `com.stark.jarvislocal.mvp003`,
18247218 octets, source propre. Elle est identique aux octets de
MVP003 : `d82d3a6c4570cc2fbe3c9da021f6684f6c711d1e8af635adbdf9fd677f684dd1`.
Certificat : `9ce9577cfcbc8b5afe5bd85451728d868f1cba86b010b27e99bbddb75c4834a3`, inchangé localement.
Le nom MVP003/diagnostic reste intentionnel : aucune qualification native nouvelle.
Les fichiers du kit ont été rehachés. Ce kit n'est pas l'artefact Actions dont
le téléchargement Azure est bloqué ; les identités CI et locales sont séparées.

439 tests JVM/Robolectric +13 tests de métadonnées Python +3 contrôles APK
réussis, builds/Lint exécutés. Un ancien garde exact de nouveaux fichiers est
en échec de périmètre, conservé ; le nouveau garde protège toute la base.
Aucune réponse GGUF, tokenisation JNI ou mesure Android réelle obtenue.
Les protocoles, gabarits et données synthétiques ne prouvent aucune inférence.

GO actuel : revue des préparatifs et outils hors ligne uniquement.
NO-GO : usage conversationnel personnel, qualification du modèle et Android.
Autorisation de téléchargement cloud explicitement demandée et non reçue à la
clôture ; aucun téléchargement présumé autorisé. La levée de ce point ne suffit
pas à fournir un runtime ARM64. Prochaine action : autorisations/dossier modèle
puis laboratoire Android ARM64, sans modifier les protections.
Aucune fusion, installation ou activation personnelle ; aucun effacement.
