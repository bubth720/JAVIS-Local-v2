# MVP-002 — guide expérimental

Aucune installation personnelle autorisée par cette livraison. Le package
`.mvp002` est distinct des versions précédentes ; ne désinstaller aucune application,
ne copier aucune base privée ni profil B. Signature debug : pas d'identité de
mise à jour stable promise entre environnements.

## Texte, données et avatar

Après accord distinct pour un appareil de laboratoire : importer un GGUF public
fiable via « Modèles et paramètres », avec provenance/licence, SHA externe de
confiance et taille exacte, comme MVP001 (>100Mio/<=512Mio). Aucun téléchargement.
Charger explicitement puis envoyer le texte. SYSTEM distinct, contexte2048 et
réponse128 tokens maximum, garde ressources conservé. Certaines commandes CORE
sont déterministes : elles ne prouvent pas qu'une génération native a eu lieu.
Un appel JNI/timeout annulé reste drainé avant toute nouvelle opération.

L'historique persiste dans la SQLite propre à ce package, avec affichage40 messages.
Un souvenir passe uniquement par « Ajouter un souvenir confirmé » ou `/remember`
confirmé. Aucun apprentissage automatique. En cas d'erreur après écriture mémoire,
lire les souvenirs avant de réessayer : mémoire et confirmation ne sont pas une
transaction commune dans CORE. Changer de modèle ne supprime pas l'historique.
L'avatar de démonstration est statique ; aucune personnalité Athéna activée.

## TTS local, manuel et refus explicites

1. Couper données/Wi-Fi et activer le mode avion avant la recherche de voix.
2. Dans « Voix, souvenirs et capacités », rechercher une voix française locale.
   Une voix réseau, absente ou marquée non installée est refusée, sans installation.
3. Lire la phrase test générique. Confirmer seulement si réellement entendue sans
   réseau. Le logiciel vérifie le flag avion, pas l'absence exhaustive de Wi-Fi.
4. Lire manuellement la dernière réponse : <=300 caractères UTF16, aucun texte
   tronqué. Les textes trop longs restent lisibles. Aucune lecture automatique
   d'une conversation ancienne ni bascule vers JARVIS-VOICE ou un moteur réseau.
5. Arrêter/désactiver ou quitter l'application révoque le test. Rotation conserve
   la composition, sans refaire de travail modèle. Si le service/état réseau
   change, recommencer le test ; une acceptation SDK n'est pas une preuve audible.

La confirmation est une attestation utilisateur future, pas une qualification
physique effectuée par Codex. En cas d'absence de voix, la conversation textuelle
reste active.

## Whisper et extensions

Import local Whisper GGML : provenance/licence, SHA et taille >50Mio/<=160Mio,
réserve disque256Mio, LLM déchargé requis. L'import ne demande pas de permission
micro et ne démarre pas Whisper. **Le bouton microphone reste désactivé**, car
le modèle, le WAV, l'admission unique et la fin native doivent être qualifiés.
Aucune transcription simulée. Ne pas activer un drapeau pour contourner ce refus.
NEXUS, SERVICES et EVOLUTION restent indisponibles. Aucun Binder pilote ni IPC intermodules JARVIS,
profil privé ni accès à un autre APK. Les versions CORE/VOICE/INTERFACE existantes
ne sont pas remplacées par cette application expérimentale.
