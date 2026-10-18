# PREVIEW-001 — fonctions et modèle

## Disponibilité réelle

| Fonction | État livré |
|---|---|
| Installation côte à côte | APK avec identifiant distinct ; à confirmer physiquement après accord |
| Accueil et état du modèle | Implémentés, testés sous Robolectric ; pas de chargement à l'ouverture |
| Laboratoire texte ALPHA1-001 | Conservé, entrée volontaire confirmée ; contexte RAM et refus sans modèle |
| États préparation/erreur/reprise/sortie | Testés avec modèle absent et graph réel ; annulation/drainage simulés dans les suites existantes |
| GGUF fourni dans le kit | Aucun |
| Import depuis le téléphone | **Non implémenté dans cette Preview** |
| Conversation réelle GGUF | Non exécutée et indisponible avec ce kit sans poids |
| Athéna / tokenizer exact proposé | Désactivée / extension JNI non appliquée |
| VOICE, micro, Whisper et avatar | Inactifs ; permission micro retirée dans cette variante |
| PDF, mémoire personnelle, connecteurs | Inaccessibles dans ce parcours Preview ; fonctions historiques conservées dans la variante normale |
| Cloud/réseau applicatif | Pas de permission Internet/ACCESS_NETWORK_STATE ni service cloud ajouté |

Un message d'accueil ou une erreur d'interface n'est jamais présenté comme une
réponse du modèle. Le laboratoire passe par chatVolatile et non par les réponses
rapides/commandes historiques. Aucun nouveau moteur ou catalogue de réponses.

## Audit du mécanisme GGUF actuel

ModelManager attend un GGUF sous stockage privé `filesDir/models/jarvis.gguf`.
Il vérifie magic GGUF, taille supérieure au seuil historique100MiB et taille
**exactement égale à l'asset embarqué** `models/jarvis.gguf`. Le kit ne contient
ni cet asset ni Whisper. Un fichier arbitraire copié dans Downloads ou même dans
le stockage privé ne peut donc pas être déclaré compatible par ce mécanisme.
Ces contrôles, ModelManager et LlamaLocalEngine restent inchangés.

Le script historique prepare_models.sh indique une référence Qwen3-1.7B Q4_K_M.
Ce n'est pas une preuve d'identité, licence, compatibilité native ou adéquation au
POCO. Il n'a pas été exécuté pour la Preview. Aucun poids téléchargé/redistribué.
La bibliothèque reste `dev.ffmpegkit-maintained:llama-android:0.1.1`, même JNI et
modèle propriétaire unique ; le patch ALPHA1-002 n'est pas inclus.

## Comment fournir un modèle ultérieurement

Deux voies techniques distinctes, toutes deux soumises à une mission/qualification :

1. **Asset contrôlé** : un propriétaire autorise explicitement un poids identifié
   par SHA-256, taille, origine et licence de redistribution. Construire une APK
   distincte contenant cet asset réutiliserait le chargeur actuel, sans importer
   arbitrairement un fichier. Le kit serait volumineux ; aucune autorisation de
   redistribution n'est présumée ici.
2. **Import local SAF** (voie recommandée pour téléphone seul) : le sélecteur
   Android OpenDocument transmet un URI lu en flux vers un fichier temporaire
   privé ; taille/magic/version GGUF, hash attendu et licence/provenance validés ;
   fsync puis renommage sans supprimer une ancienne destination avant succès.
   Un fournisseur de modèle propre à la Preview devrait remplacer la dépendance
   à la taille d'asset par un manifeste d'import vérifié. Ne pas affaiblir le
   ModelManager historique ni lui inventer un asset pour faire accepter un fichier.

Le futur import doit être autorisé seulement avant chargement ou après déchargement
confirmé par le chemin existant ; jamais remplacement d'un fichier utilisé par JNI,
reset d'admission ou second moteur. Interrupted/corrupt/oversized/hash mismatch,
espace insuffisant et retour arrière doivent être testés. Pas de téléchargement
automatique, droit cloud ou permission de stockage global nécessaire pour SAF.
La présence d'un GGUF et d'une licence ne suffit pas : tester architecture,
quantification/template pris en charge par l'AAR, chargement réel, protections,
RAM/température/batterie et contexte sur appareil explicitement autorisé.

Pour PREVIEW-001, l'import et la conversation restent fermés. La prochaine mission
peut préparer cette voie SAF, avec choix de modèle et autorisations distinctes.
La tokenisation exacte demeure nécessaire avant activation d'Athéna, pas remplacée
par un compteur approximatif ni une persona dans le rôle utilisateur.
