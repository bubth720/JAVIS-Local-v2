# CORE — PREVIEW-001

Base vérifiée : feature/alpha1-001-text-lab,
`b2ec98238f70e89edc636c1c671f2ddf1b957d74`.
Branche : `release/preview-001-android`. Le SHA du kit réel est fourni dans
APK-INFO.json et le résumé GitHub Actions ; le compte rendu final donne le commit publié.

## Livraison et périmètre

Variante preview, minSdk24/target35/ARM64, identifiant
`com.stark.jarvislocal.preview001`, label JARVIS Preview001, signature debug.
Accueil spécifique sans préparation native. Laboratoire ALPHA1-001 accessible
après confirmation explicite ; sortie n'ouvre pas le mode historique. Voix,
PDF/mémoire/réglages désactivés dans ce parcours, permission micro retirée.
Même moteur LLM local existant ; aucun JNI, admission, budget ou seuil modifié.
La variante historique conserve son applicationId, manifeste et comportement par défaut.

Le kit Actions contient APK, guides, APK-INFO.json, permissions/manifeste,
certificat public et SHA256SUMS.txt. Aucun modèle, audio, secret, clé privée ou
donnée personnelle. Le workflow Preview a contents:read et ne publie aucune
release, ne fusionne rien et n'installe rien. Il s'exécute au push de la branche
dédiée ; l'ancien workflow de téléchargement de modèles n'est pas utilisé.

## Capacités et blocages

Cette livraison teste l'interface et les refus sans modèle. **Elle ne permet pas
une conversation avec le kit livré.** Aucun importeur GGUF dans cette version,
aucun poids redistribué ; chargeur historique strictement dépendant d'un asset
absent. FONCTIONS-ET-MODELE.md décrit les deux voies de fourniture future et les gates.
Athéna, la tokenisation exacte proposée, Binder/2, VOICE et avatar restent inactifs.

Signature de développement temporaire, pas promesse de mise à jour durable.
Nouvel identifiant pour préserver les installations existantes ; si le même package
Preview était déjà installé, arrêter avant toute mise à jour/désinstallation.
Aucun essai de compatibilité physique POCO ou conversation GGUF réalisé.

## Vérifications

Résultats exacts détaillés dans VALIDATION.md pour le cloud ; dans chaque kit,
APK-INFO.json contient les résultats XML des tests réellement exécutés sur le
commit et les contrôles de son APK signé. Les tests de Preview sont Robolectric,
pas une installation Android. Les fichiers JNI/admission/fixtures/IDENTITY sont
comparés à la base exacte par tools/check-preview-001.py.

Les anciens gardes absolus de missions précédentes ne sont pas réécrits pour
accepter ce nouveau périmètre ; leur limite est consignée dans VALIDATION.md.
Le statut distant Actions doit être vérifié sur la page de l'exécution, pas déduit
de la seule présence du workflow ou d'un build local.

## Prochaine décision

Après revue du kit, **autoriser ou refuser son installation manuelle sur le POCO
pour le seul essai d'accueil/laboratoire sans modèle**. Aucun accord d'installation
n'est implicite dans sa publication. Aucun changement de JNI, intégration Athéna,
import de poids ou fusion main n'est autorisé par cette Preview.
Après cet essai : proposer PREVIEW-002 pour import SAF qualifié et choix explicite
d'un GGUF de provenance/licence vérifiées, sans remplacer les protections natives.
