# ALPHA1-003 — provenance vérifiée, phase A

Date : 18 octobre 2026. Dépôt autorisé : `bubth720/JAVIS-Local-v2`.
Base exacte : `d2a677bb35e63ee94db9dfa4edcaa97036b45dce`, branche distante
`design/alpha1-002-jni-review` vérifiée avant travail. Branche de livraison :
`feature/alpha1-003-native-validation`. Le laboratoire reste celui de
`b2ec98238f70e89edc636c1c671f2ddf1b957d74` ; aucune modification applicative.

## Sources et responsabilité

- Source officielle : [ffmpegkit-maintained/llama-android@823d1de2](https://github.com/ffmpegkit-maintained/llama-android/tree/823d1de2b3ef85ba9ef4847a03bac423f4aa1c44).
  Tree Git : `f5c2026376c07dbac2609b865491207d70547585`.
- Gitlink `llama.cpp` : `2da668617612d2df773f966e3b0ee22dc2beef7b`, revérifié
  dans l'objet Git amont. Sources [ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp/tree/2da668617612d2df773f966e3b0ee22dc2beef7b).
- AAR historique : `dev.ffmpegkit-maintained:llama-android:0.1.1`.
  Mainteneur déclaré : Jokobee / Luc Côté. Cela n'attribue pas à JARVIS la propriété
  de la bibliothèque amont ni une autorité de publication dans son namespace.
- L'artefact expérimental proposé appartient à la responsabilité du mainteneur
  JARVIS CORE, sous validation utilisateur, avec coordonnées distinctes
  `fr.jarvis.experimental:llama-android-exact:0.1.1-jarvis-alpha1.002-review.1`.
  Aucun dépôt amont, registre Maven, AAR de production ou moteur installé modifié.

Le tag annoté `v0.1.1` (objet `574b5731b4e0605f2b31f4fb6713f18f0ccf25b8`)
est **non signé** : `git verify-tag` retourne 1, `no signature found`.
Un hash identifie les octets observés ; il ne remplace pas une signature du producteur.

## Empreintes recalculées

| Entrée | SHA-256 |
|---|---|
| Archive source llama-android au commit exact | `0e386b0f79ac29ffffdc4b3a9061ca2467c2b1b872420a3c1a8f4a7f04555fde` |
| Archive source llama.cpp au commit exact | `927d1ab5daa353ec18d415355fd491f9def014f2157d7b1527553f60effd6671` |
| AAR publiée 0.1.1 | `fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501` |
| Patch ALPHA1-002 intact | `94f06d0179b95c332f5842b73b69d103e09ca319befc319a0aeca341735c6df7` |
| Licence llama-android | `e10e3d37bbc2731fe88c808d998f6b5d840bd94d30b4651dd79381f69c93730a` |
| Licence llama.cpp | `94f29bbed6a22c35b992c5c6ebf0e7c92f13b836b90f36f461c9cf2f0f1d010d` |

Les archives proviennent de codeload GitHub sous TLS, commits exacts. Le checksum
Maven de l'AAR a été consulté et correspond. Les 31 fichiers amont et 22 headers
llama.cpp du lock ALPHA1-002 ont été revérifiés, ainsi que les sept composants
de l'AAR référencés dans ce lock. Les deux licences sont MIT ; leurs avis doivent
être conservés lors d'une future redistribution. La notice LLVM/libc++ du NDK
doit également accompagner la vérification de redistribution ; aucun binaire
n'est publié par cette mission.

## Patch : douze fichiers, aucune application

`tools/alpha1-003/verify_phase_a.py audit` analyse les hunks et reconstruit leurs
résultats **en mémoire uniquement**. Les douze hashes avant/après correspondent
à `docs/alpha1-002/PATCH_FILES.json`. Aucune écriture de candidat, aucun `git apply`,
typecheck du candidat, chargement JNI ou génération n'est exécuté en phase A.
Le manifeste complet est repris dans `RESULTS.json/provenance/sensitive_files`.

Les 167 fichiers suivis de la base exacte sont byte-identiques. Les documents
ID-002N préexistants et non suivis ont été préservés, exclus de cette livraison.

**Conclusion limitée :** provenance source et identité de l'AAR établies ;
équivalence des binaires natifs publiés avec notre fabrication non démontrée.
Voir BUILD-REPRODUCIBILITY.md et RESULTS.json pour les preuves actuelles.
