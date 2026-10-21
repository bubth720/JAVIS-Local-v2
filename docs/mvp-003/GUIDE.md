# Guide de livraison MVP-003

**CANDIDATE DE DIAGNOSTIC — NON OPÉRATIONNELLE AU SENS DE LA QUALIFICATION.**
Aucune génération native réelle, voix audible ou qualification physique acquise.
Aucune installation n'a été effectuée. Attendre une autorisation explicite
avant essai sur POCO X8 Pro ou Redmi Pad SE.

## Vérifier le kit

Le kit contient JARVIS-MVP-003.apk, APK-INFO.json, CERTIFICAT.txt,
PERMISSIONS.txt, MANIFEST.txt, ZIPALIGN.txt, SHA256SUMS.txt et les guides.
Comparer le SHA-256 de l'APK et du certificat au manifeste **de ce kit**.
Ne pas utiliser une empreinte MVP001/MVP002 ou celle d'une autre exécution CI.
Sous Linux : `sha256sum -c SHA256SUMS.txt`. Sur Android, utiliser un outil de
hachage local déjà approuvé ; aucune application supplémentaire imposée.
Une empreinte publiée dans le même ZIP ne constitue pas seule une provenance
indépendante : rapprocher commit, branche et preuve de publication.

## Installation manuelle ultérieure, après accord

1. Vérifier Android >=7/API24, espace libre, ABI arm64-v8a et la fiche appareil.
2. L'application doit se présenter comme **JARVIS MVP 003 diagnostic**,
   package `com.stark.jarvislocal.mvp003`. Si ce package existe déjà, comparer
   son certificat et sa version avant toute mise à jour ; arrêter si inconnus.
3. Ne désinstaller aucune version pour résoudre un conflit de signature.
   Ne changer ni signature, ni identifiant, ni base utilisateur pour contourner.
4. Après accord seulement, ouvrir cette APK depuis Fichiers, autoriser
   temporairement cette source si nécessaire, puis révoquer l'autorisation.
   Ne pas désactiver les protections Android. Rien n'est installé par les scripts.
5. Premier lancement sans modèle : aucun moteur chargé ; saisie conservée,
   conversation ordinaire refusée explicitement. Ne pas confondre `/docs` ou
   `/remember` avec une génération native réussie.

## Modèle local

Aucun GGUF ni modèle Whisper choisi, téléchargé ou inclus. MODEL-PROVENANCE.json
est un **gabarit non qualifié**, pas une attestation. Faire approuver un modèle
public compatible avec la bibliothèque historique avant une qualification réelle.
Documenter dépôt/version, fichier, licence, taille exacte, SHA-256 obtenu par
une source de confiance et compatibilité. Le texte saisi « licence/provenance »
n'est pas vérifié juridiquement par l'application.

Copier manuellement un fichier déjà autorisé dans le stockage local accessible
au sélecteur. Mode avion, Wi-Fi/données désactivés. Le SAF demande local-only,
mais un fournisseur tiers n'est pas une preuve réseau. Ne pas choisir une source
cloud ou un document personnel. Remplir les quatre champs, importer puis charger
explicitement. Toute erreur doit conserver les imports précédents. Aucun
contournement des seuils pour faire accepter un modèle trop gros/petit.

## Usage de diagnostic

SYSTEM <=1024 octets UTF-8, question <=2048 octets, sortie demandée128 tokens,
contexte CORE <=2048. Le budget de prompt reste conservateur, pas exact.
Surveiller l'état affiché ; ne jamais forcer un nouveau chargement pendant busy
ou quarantaine. Annuler ne décharge pas. Utiliser « décharger » après fin réelle.
Une erreur de libération impose l'arrêt des essais, pas une remise à zéro cachée.

Historique et souvenirs sont privés à ce package. Mémoire seulement via
confirmation explicite ; une erreur de confirmation peut suivre une mémoire
déjà enregistrée, car les écritures sont séparées. Pas d'apprentissage automatique.
PDF/RAG conservé dans CORE mais aucun bouton d'import PDF dans cette UI ;
ne pas le présenter comme un parcours utilisateur qualifié.

La lecture TTS est manuelle : mode avion et Wi-Fi/données coupés, voix locale
installée, phrase publique effectivement entendue, confirmation. READY ou speak
accepté n'est pas preuve acoustique. Sans test réussi, ne pas lire de texte privé.
Microphone désactivé : aucun essai de transcription dans cette livraison.

## Retour arrière et conservation

Fermer la candidate et reprendre l'application précédente, sans la modifier.
Conserver la candidate et son stockage si l'on veut garder son historique.
Ne pas désinstaller, effacer les données, abaisser une version ou restaurer une
base d'un autre package. Aucune copie automatique vers les anciennes versions.
Aucun export/restore applicatif Android livré ; allowBackup=false, modèles dans
noBackupFilesDir. Ne pas promettre récupération après perte/désinstallation.
Les tests de réouverture utilisent uniquement des données synthétiques.

Pour une future sauvegarde, concevoir d'abord un export explicite, cohérent
SQLite/préférences/manifeste, vérifié sur copie et sans modèle privé implicite.
Ne pas extraire des données d'un appareil pour cette mission. Un backup système
OEM éventuel n'est ni un mécanisme applicatif ni une garantie de restauration.
