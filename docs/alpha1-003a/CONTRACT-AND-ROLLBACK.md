# ALPHA1-003A — contrat de la proposition et retour arrière

## Propriétaire et portée

Source cible : ffmpegkit-maintained/llama-android, commit
823d1de2b3ef85ba9ef4847a03bac423f4aa1c44, gitlink llama.cpp
2da668617612d2df773f966e3b0ee22dc2beef7b. Propriétaire proposé de l'expérimentation :
mainteneur JARVIS CORE sous validation utilisateur, avec coordonnées distinctes
`fr.jarvis.experimental:llama-android-exact:0.1.1-jarvis-alpha1.003a-review.1`.
Aucune publication Maven ni modification de dépôt amont n'est effectuée.

Le nouveau patch est directement relatif à la racine amont : sept chemins
library/, sans préfixes upstream/core. Le composite ALPHA1-002 reste intact comme
référence historique ; ne pas l'appliquer ou le filtrer pour remplacer cette revue.
PATCH_FILES.json donne les SHA avant/après et ceux de la proposition précédente.

## Contrat hôte proposé, pas d'intégration

- loadFrozen reçoit un Context applicatif privé ainsi que SHA et taille du modèle
  provenant d'un manifeste approuvé séparément ; aucun téléchargement ni hash improvisé.
- La bibliothèque construit la destination elle-même dans noBackupFilesDir ; aucun
  répertoire arbitraire n'est traité comme une garantie de confidentialité.
- Le slot du propriétaire existe avant copie/load. Un handle livré appartient au
  caller, avec référence conservée par le propriétaire ; son utilisation exige le
  même slot. Un handle non livré est nettoyé sur le worker, jamais dans le handler
  d'annulation. La livraison annulée ne vaut pas fin d'un appel natif.
- Après chargement, l'hôte garde la responsabilité de sa propre admission et du
  release. Les appels de génération ne donnent aucun droit sur les autres moteurs.
- Les buffers/tickets prepare/consume/discard ne sont pas des autorisations Binder.
  SYSTEM reste distinct du prompt utilisateur ; aucune activation IDENTITY/Athéna.
- Une release inconnue conserve le handle, le fichier et le blocage. Le compteur
  isLoaded est informatif, pas une preuve de fin/unload.
- Une release revenue peut mettre le handle à zéro alors que le nettoyage fichier
  échoue. Le retry du fichier demeure possible : releaseModel pour un modèle livré,
  retryAbandonedFileCleanup pour le seul slot abandonné déjà sans ressource native.
  Ce retry n'appelle pas un reset d'admission CORE et ne réconcilie aucun processus.
- Les anciens appels gardent leurs signatures. Les refus UI/BUSY/réentrée sont
  des restrictions explicites nouvelles à valider, pas une compatibilité silencieuse.

Le snapshot est privé mais n'est pas un export/sauvegarde. Après un crash, aucune
suppression automatique globale, migration de fichiers orphelins ou réconciliation
interprocessus n'est livrée. Une identité de fichier ou un état natif inconnu bloque.

## Qualification nécessaire après autorisation distincte

1. Revoir le SHA exact de PATCH.sha256 et ses sept fichiers ; approuver les restrictions
   d'exécution et le nouveau contrat loadFrozen. Aucune autorisation n'est déduite
   d'un contrôle de types ou d'un message « reprends ».
2. Construire une AAR distincte dans un environnement isolé autorisé. Lier les trois
   nouvelles déclarations JNI et vérifier ARM64/API24 ; une classe JVM compilée seule
   ne prouve pas le lien natif.
3. Avec GGUF autorisé : une allocation modèle/contexte réelle, prepare sans decode/KV,
   mêmes IDs au prefill, contextes/encodages/templates, mauvais tickets et ABA.
4. Tester annulation aux frontières load/delivery, refus de concurrence/réentrée,
   callback bloqué, worker remplacé, handle non livré et échecs release.
5. Tester fichier croissant/rétrécissant, taille constante modifiée, disque plein,
   symlink, inode changé, droits insuffisants, suppression/fsync échoués et retries.
6. Qualifier les méthodes historiques avec l'artefact réel avant tout raccordement
   CORE ultérieur ; les299 tests passés en phase A ne qualifient pas le nouveau SDK.

Les limites de reproductibilité établies par ALPHA1-003 restent inchangées :
reproduction observée au même chemin/hôte, pas entre répertoires ni équivalence
native avec Maven. Aucun build de cette phase A n'est relancé par cette mission.
Les appareils personnels, modèles non autorisés et toute activation requièrent
leurs autorisations propres.

## Retour arrière

Actuellement : rien à restaurer dans le runtime. Les187 fichiers suivis de la base
84571ae sont identiques ; seul le dossier documentaire003A est ajouté. Les sources
natives épinglées, le composite002 et l'AAR de production sont intacts.

Pour une future expérimentation autorisée : conserver sources/AAR/hashes antérieurs,
arrêter/drainer le vrai propriétaire et ne restaurer un binaire cohérent que dans
un processus dont la fin est réellement établie. Aucun reset à chaud du slot, du
bail CORE ou d'un journal Binder pour permettre un nouveau moteur. Une release
incertaine ne permet pas de supprimer le snapshot. Après release confirmée, nettoyer
uniquement le fichier créé/détenu et identifié par son device/inode ; un échec reste
un blocage de nettoyage. Aucun GGUF original, base SQLite, profil ou répertoire
modèles n'est supprimé. Aucune fusion/installation n'est autorisée par ce plan.
