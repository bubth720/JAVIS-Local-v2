# Clôture locale et publication

Module JARVIS CORE, mission MVP-003.
Branche : `release/mvp-003-android-candidate`.
Commit de fabrication publié : `faf0d4fde9103b039ffd208361ccfccfbe603658`.
Le commit suivant clôture uniquement les preuves ; il ne change pas l’APK.
Publication Git vérifiée avec `git ls-remote` : voir PUBLICATION.json.
Aucune fusion ni PR créée ; revue possible depuis la branche dédiée.

APK locale : `JARVIS-MVP-003.apk`, 18247218 octets,
package `com.stark.jarvislocal.mvp003`, signature debug et statut diagnostic.
SHA-256 : `d82d3a6c4570cc2fbe3c9da021f6684f6c711d1e8af635adbdf9fd677f684dd1`.
Certificat SHA-256 : `9ce9577cfcbc8b5afe5bd85451728d868f1cba86b010b27e99bbddb75c4834a3`.
Les autres empreintes sont dans le fichier SHA256SUMS.txt du kit.

Le kit local est produit depuis un commit propre (source_dirty=false), puis
ses fichiers sont rehachés. Il est distinct d’un éventuel artefact Actions.
Le workflow mvp-003.yml publie un artefact de build s’il réussit ; son état
n’a pas été observé dans cette session. Ne pas présenter l’APK locale comme
un artefact GitHub téléchargé/revérifié. Le fichier APK n’est pas versionné
ni téléversé par cette session ; les preuves et le workflow sont publiés.

439 tests logiciels +3 contrôles APK réussis ; résultats et limites dans
REPORT.md, RESULTS.json, TEST-RESULTS.json et APK-NEGATIVE-TESTS.json.
Six anciens gardes en échec de périmètre demeurent signalés.
Génération native, TTS audible, micro et appareils : NON VALIDÉS.
Prochaine action : revue, choix/autorisation d’un modèle public compatible,
puis qualification Android explicitement autorisée. Aucune installation.
