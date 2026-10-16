# Matrice de compatibilité et qualification — 004A

La colonne « tests » décrit l'exécution locale du 16 octobre 2026, détaillée
dans [VALIDATION.md](VALIDATION.md). « Android simulé » signifie Robolectric
sur JVM, pas émulateur/device. Toutes les fonctions de la première colonne
restent dans le code ; leur présence ne constitue pas une recette terrain.

| Fonction | Code implémenté / conservé | Tests réussis disponibles | Validation Android réelle / reste à qualifier |
|---|---|---|---|
| Chat LLM | Core → moteur llama local, contexte borné | Core avec faux moteur : ordre, exclusion, erreurs, annulation ; budget UTF-8 | Non : modèles absents, qualité, latence, RAM et vrai nombre de tokens |
| Commandes et réponses rapides | Salutations, merci, identité, heure, `/remember`, `/docs` | Heure 7 ; mémoire succès/échec ; pas de couverture exhaustive de toutes les chaînes de commandes | Non : recette de chaque commande, accents/locale, liste documents |
| Historique | SQLite, affichage 100 derniers messages | Migration, réouverture, valeurs/IDs et ordre conversationnel ; rotation activité simulée | Non : fermeture réelle, arrêt forcé, stockage plein, erreur de lecture dans l'UI |
| Mémoire | Ajout explicite commande/dialogue, recherche lexicale | Insertions refusées/ignorées, absence de faux succès ; dialogue activité ; limite de confirmation démontrée | Non : volume, persistance après arrêt brutal ; pas de mémorisation automatique |
| PDF textuels | PDFBox, découpage, page dans chaque nouveau passage | Vrai PDF synthétique 2 pages importé/réouvert sur SDK 24/35 ; PDF vide refusé ; rollback SQL | Non : sélecteurs/providers/OEM, vrais PDF volumineux ou malformés |
| RAG / sources | Recherche lexicale, preuve directe, contexte GGUF | Sources des passages réellement inclus conservées ; contexte/persistance PDF testés | Non : pertinence, faux positifs, citations sémantiques, ancien corpus sans page complète |
| Whisper | Chargement/transcription/libération locale | Backend simulé, erreurs, WAV refusé, concurrence et annulation | Non : vrai modèle, JNI ARM64 et taux d'erreur transcription française |
| Enregistrement | AudioRecord, PCM mono 16 kHz 16 bits, WAV finalisé | Worker réel sur JVM/faux micro, permission/IO/stop/release, nettoyage, quarantaine | Non : matériel, interruptions système, permission révoquée sur appareil |
| TTS | Moteur Android recréable et voix déclarée locale française | Doubles moteur, callbacks tardifs, arrêt/recréation ; métadonnées Voice simulées | Non : disponibilité et comportement hors ligne du fournisseur installé |
| Modèles locaux | Installation depuis assets, taille exacte/signature, renommage | Copies interrompues, retry après recréation, `.part`, destination conservée, refus fichiers incomplets | Non : gros assets réels, chargement tenseurs, disque plein, coupure électrique |
| Paramètres | SharedPreferences et plafonds historiques conservés | Recréation logique du store sur SDK 24/25/35, prompt/voix et clés inconnues préservés | Non : durabilité d'`apply` après mort du processus, valeurs corrompues |
| Cycle de vie | Coordination applicative et observateurs STARTED | Activity recréée sous Robolectric SDK 24/35 ; préparation/génération conservées | Non : OEM, retour arrière réel, destruction système, reprise après mort du processus |
| Sauvegarde/restauration | Aucun export/import applicatif présent | Tests SQL de conservation uniquement | Non implémenté : stratégie dans DATA_PRESERVATION ; ne pas désinstaller pour tester |

## Plateforme et packaging

| Cible / contrôle | Résultat | Limite |
|---|---|---|
| `minSdk=24`, SDK cible/compilation 35 | Gradle et manifeste de l'APK vérifiés ; Lint sans erreur NewApi | Ne valide pas le chargement natif sur tous les appareils |
| API 24 | SQLite natif Robolectric, Core, Activity, PDF, paramètres et voix simulée | Pas d'émulateur/device |
| API 25 | SQLite natif Robolectric, paramètres et métadonnées Voice | Pas de suite Activity/PDF/Core complète sur 25 ; Lint couvre les références API |
| API 35 | SQLite natif Robolectric, Core, Activity, PDF et paramètres | Pas de test des contraintes OEM réelles |
| ARM64 | 7 bibliothèques présentes dans l'APK | Build hôte x86/JVM : JNI métier pas exécuté |
| Autres ABI | Absentes selon configuration historique | Aucune compatibilité x86/ARM32 revendiquée |
| Réseau | `INTERNET` / `ACCESS_NETWORK_STATE` absents de l'APK ; manifeste inchangé | TTS est un moteur externe ; contrôler ses réglages et son comportement |
| Signature | APK debug vérifié par apksigner | Pas une qualification de signature de production ou d'update depuis un autre certificat |
| Modèles APK | Absents de cette construction locale | APK non autonome pour IA ; aucun gros téléchargement ou APK publié |
| Variante release | Configurée, inchangée | Compilation/minification et exécution non effectuées |

## Protocole Android restant à exécuter

Avec appareil de test ARM64 API 24/25 puis récent, modèles publics autorisés
et données exclusivement synthétiques :

1. Vérifier le certificat de l'installation et conserver son état avant toute
   mise à jour. Ne pas désinstaller ni effacer les données. Préparer une
   sauvegarde/restauration démontrable sur cet appareil de test, selon les
   possibilités de développement autorisées ; aucun export applicatif n'est livré.
2. Migrer une base synthétique v1 avec les quatre collections et IDs connus,
   rouvrir plusieurs fois, comparer exactement contenu, version et séquences.
   Répéter erreurs IO/SQL et refus de version future sans suppression.
3. Préparer les modèles, interrompre une copie, redémarrer puis réessayer.
   Vérifier absence de disponibilité prématurée et conservation de destination.
4. Envoyer deux questions rapprochées, alterner PDF/voix, attendre un délai
   dépassé puis retenter ; aucun second calcul ne doit commencer avant fin réelle.
5. Tourner l'écran pendant copie, génération, capture/transcription ; mettre
   en arrière-plan, revenir, fermer puis rouvrir ; vérifier saisie/historique,
   arrêt de la capture, TTS et nettoyage du cache. Ne pas supposer la reprise
   d'un calcul après mort du processus.
6. Refuser/révoquer le micro, simuler indisponibilité, vérifier WAV incomplet
   rejeté ; tester TTS sans voix française locale et avec cette voix hors ligne.
7. Mesurer mémoire, espace, latence UI (StrictMode/profilage), vraie limite du
   prompt et pertinence/sources avec corpus de référence. Tester les échecs de
   lecture de base et paramètres invalides avant de déclarer la recette complète.

Aucune de ces étapes sur appareil n'a été exécutée pendant la mission.
