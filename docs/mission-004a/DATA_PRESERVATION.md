# Préservation et stratégie de sauvegarde/restauration — 004A

## État livré

- Base `databases/jarvis_local_v2.db`, version cible 2 inchangée, quatre tables
  inchangées. Migration connue 1 → 2 sans transformation. Refus des autres
  transitions et downgrade, sans suppression ni réinitialisation de secours.
- `SQLiteOpenHelper` possède la transaction de migration. L'interruption
  synthétique par exception après une écriture annule les lignes et le numéro
  de version. Ce test ne simule pas physiquement une coupure électrique.
- `insertChecked` refuse les erreurs et id -1. Un import défaillant annule
  document et passages nouveaux, conserve les anciennes collections et leurs
  séquences. Réouverture d'un nouvel helper vérifiée ; ce n'est pas un essai
  de mort/reprise du processus Android.
- `/remember` reste explicite et conserve son texte de confirmation. Le
  souvenir, la question et la réponse sont des écritures séparées. Un échec
  après la première peut laisser le souvenir sans confirmation ; consulter la
  liste avant de répéter. Aucune déduction ou mémorisation automatique ajoutée.
- Préférences : fichier `shared_prefs/jarvis_settings_v2.xml`, clés `prompt`,
  `ctx`, `max`, `topk`, `speak`. La lecture applique les plafonds historiques
  sans écrire ; sauver écrit ces cinq clés, sans supprimer les autres.
  `apply()` ne fournit pas de résultat de durabilité disque. Les tests vérifient
  la recréation du store et la conservation logique, pas une coupure brutale.
- Modèles : noms `files/models/jarvis.gguf` et `whisper.bin` conservés. Copie
  exclusive par instance, `.part` voisin, contrôle longueur exacte de l'asset
  non compressé et magic GGUF/ggml, synchronisation du fichier puis renommage
  atomique Android/Linux sans supprimer l'ancienne destination. Une erreur
  conserve cette destination. Une nouvelle tentative recommence la copie
  entièrement, sans réutiliser aveuglément une `.part` laissée par crash.
  Les deux modèles sont installés indépendamment, pas dans une transaction
  commune. Aucun modèle utilisateur alternatif n'est validé par ce protocole.
- La validation légère n'est pas une empreinte authentifiée : même taille et
  magic ne prouvent ni tous les tenseurs ni l'origine d'un modèle. Absence
  d'asset de référence signifie disponibilité non démontrée, donc refus.
  Aucun checksum appris d'un fichier potentiellement tronqué n'est utilisé.
  Le renommage local protège la visibilité atomique ; la durabilité après
  perte d'alimentation et fsync du répertoire n'est pas revendiquée.
- Les nouveaux chunks PDF ont tous leur page ; aucun ancien texte n'est modifié
  ou supprimé. Les PDF originaux ne sont pas copiés par l'import actuel.
- Le WAV est temporaire, validé puis supprimé après la fin réelle du traitement.
  Le nettoyage de démarrage se limite aux noms réservés `jarvis_input*.wav`
  définis précisément par `WavFiles`, pas à tous les fichiers du cache.

## Tests de conservation

`LocalDatabaseTest` conserve la fixture historique indépendante. Scénarios :
création/réouverture, migration v1, ouverture v2, IDs/timestamps/nulls et
séquences, échecs message/souvenir/document/second passage, reprise, migration
interrompue, refus upgrade inconnu/downgrade/base existante sans version.
Les versions non historiques utilisées pour le refus sont synthétiques.

`ModelInstallationTest` : copie partielle levée en erreur, ancien fichier
conservé, nouvelle instance/reprise, renommage refusé, `.part` abandonnée,
asset tronqué/trop long/mauvaise signature, fichiers complets conservés.
`ModelManagerConcurrencyTest` : un seul writer par instance et verrou libéré
après erreur. Petits faux modèles, aucun chargement natif.

`SettingsStoreTest`, `JarvisCoreConcurrencyTest`, `MainActivityLifecycleTest`
et `PdfImporterTest` complètent les vérifications des préférences, des
confirmations, des pages et de leur relecture. Toutes les données sont
synthétiques. Aucune donnée privée d'un appareil utilisateur n'a été ouverte.

## Stratégie recommandée, non implémentée

**Il n'existe pas d'export/import Android applicatif dans cette livraison.**
`allowBackup=false` est conservé. Ne pas conseiller désinstallation, effacement
Android, copie à chaud d'un seul fichier SQLite ou `adb backup` comme sauvegarde
validée. Ne pas promettre de restauration de données déjà supprimées par une
ancienne migration sans sauvegarde externe.

Pour un futur mécanisme, à développer et autoriser séparément :

1. Demander explicitement le consentement, la destination via SAF et le
   périmètre (historique, souvenirs, documents/passages, préférences). Afficher
   que l'archive contient des données personnelles. Aucun envoi réseau.
2. Mettre les écritures en pause et attendre la fin réelle des opérations
   natives/imports. Produire un snapshot cohérent par une API SQLite adaptée
   à API 24, ou fermer complètement les helpers avant copie privée. Si la
   configuration emploie des journaux/WAL, les traiter selon SQLite ; ne pas
   ignorer arbitrairement les fichiers auxiliaires. Le mode journal n'est pas
   modifié par la présente mission.
3. Attendre la persistance des préférences par une méthode à résultat vérifiable.
   Exporter ensemble snapshot et réglages avec version de format/schéma,
   application, taille, empreintes et inventaire. Modèles reproductibles
   facultatifs, WAV/cache exclus. Les originaux PDF ne sont pas présents par
   défaut : conserver le corpus extrait, ne pas promettre les originaux.
4. Choisir une protection locale de l'archive et une gestion de clé explicites ;
   ne pas présenter le stockage privé comme un chiffrement applicatif.
5. Restaurer d'abord en espace de staging : vérifier format, tailles, empreintes,
   schéma connu, `PRAGMA integrity_check`, comptages/IDs/relations et réglages.
   Refuser les versions futures sans toucher à l'installation courante.
6. Garder une sauvegarde de l'état précédent, fermer les utilisateurs des
   fichiers puis remplacer de façon atomique avec stratégie de rollback. Une
   restauration DB + préférences exige un protocole de cohérence spécifique,
   pas deux copies indépendantes supposées atomiques.
7. Tester coupures à chaque étape, espace insuffisant, archive invalide et
   réouverture ; comparer exactement les collections et paramètres avant/après.
   Qualifier sur appareil avec données de test et restauration démontrée.

Sur un appareil de développement seulement, une extraction privée contrôlée
peut être étudiée après autorisation explicite et selon le caractère debuggable
de l'APK. Cette possibilité n'a pas été exécutée et n'est pas un export livré.
