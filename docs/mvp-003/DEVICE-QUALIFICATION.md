# Qualification physique à autoriser ultérieurement

| Appareil | État | Profil initial proposé |
|---|---|---|
| POCO X8 Pro | NON TESTÉ ; caractéristiques non relevées | Un modèle autorisé, un tour court, sortie128, aucun autre modèle lancé |
| Redmi Pad SE | NON TESTÉ ; ABI/RAM/API non relevées | Même seuil de sécurité, un tour court ; refus définitif si low-RAM/ABI incompatible |

Ces profils sont des procédures, pas des certifications ni des budgets majorés.
Ne pas déduire RAM/ABI/API du nom commercial. Si Redmi ne satisfait pas les
critères, le résultat est « incompatible/refusé », sans diminuer les protections.
Aucun accès aux appareils ni commande adb n'a été réalisé.

## Préconditions et mesures publiques

Après autorisation spécifique seulement : relever modèle exact, version Android,
ABI64 et taille de pages, RAM disponible, indicateur low-RAM, stockage libre,
batterie/température/thermal et versions des services TTS. Consigner uniquement
ces mesures techniques, aucun identifiant personnel, numéro de série ou texte privé.
Interrompre si modèle/contrat/hash manquant ou si signature inconnue.

GGUF >100Mio <=512Mio ; réserve disque256Mio après copie ; RAM disponible
>=max(1Gio, 3×tailleGGUF+256Mio), ni lowMemory ni lowRam. Batterie connue >20%
hors charge, température connue <40°C et >0 ; thermal NONE/LIGHT API29+.
API24–28 utilise la température batterie, pas une mesure du SoC. Aucun gain de
budget pour le POCO. Charge secteur ne lève pas les protections thermiques.
Contexte2048/sortie128 ; tests publics courts ; aucun moteur lourd concurrent.
Le verrou CORE est local au processus, pas un arbitre global entre applications.

## Fiche à remplir pour chaque appareil/kit

Noter commit, SHA APK/certificat, package et version ; SHA/provenance/licence du
GGUF ; mesures avant/après, résultat et preuve sans contenu personnel. Toutes
les cases ci-dessous sont actuellement **NON EXÉCUTÉES PHYSIQUEMENT**.

| Parcours | Résultat attendu | Preuve nécessaire |
|---|---|---|
| Premier lancement sans modèle | Aucun warmup, refus de génération | Capture synthétique + état/mesures |
| Import valide/invalide/annulé | Taille/hash/espace ; sélection précédente intacte | Manifeste public et erreurs, sans document privé |
| Vraie conversation | Deux questions inédites synthétiques ; SYSTEM distinct ; réponse non cataloguée | Moteur natif local dans le processus CORE : trace JNI contrôlée et mesures |
| Génération → annuler → envoyer | Pas de réponse tardive ; nouveau travail refusé avant fin réelle | Chronologie état JNI/contrôleur sans prompt privé |
| Déchargement | Libération confirmée avant nouvelle admission | Preuve observable du backend ; jamais déduite du timeout |
| Rotation/arrière-plan | Pas de deuxième moteur ; réponse annulée écartée | Étapes et état avant/après |
| Fermeture/réouverture/mort OS | Historique/souvenirs confirmés conservés, pas de warmup | Comparaison de données synthétiques uniquement |
| Voix Android | Phrase entendue hors réseau puis réponse courte sur demande | Version moteur/voix + audition humaine ; statut READY insuffisant |
| Microphone | Désactivé et permission absente | Aucun accès micro ; pas de revendication Whisper |
| Réseau | Mode avion + Wi-Fi/données coupés ; aucune sollicitation réseau | Inspection autorisée des UID/services ; pas seulement permission statique |
| Ressources | Refus sans contourner RAM/thermique/batterie | Mesures, retour contrôlé ; ne pas provoquer surchauffe/épuisement |
| Coexistence | Anciennes applications/données intactes | Vérification manuelle sans extraction ni suppression |

Ne pas lancer de crash thermique, de test destructif ou de changement de journal.
Si calcul natif ne finit pas après annulation, arrêter les essais : le stop JNI
historique n'est pas attesté. Ne pas déclarer nativeDone/modelUnloaded par délai.
Un chargement réussi seul n'est pas une preuve de réponse pertinente/compatible.

## GO ultérieur

GO personnel seulement après autorisation, identité/signature établies, modèle
public autorisé, preuve JNI réelle, parcours ci-dessus et confidentialité qualifiés.
Toute absence de preuve reste explicitement bloquante ; une APK compilée ne suffit pas.
