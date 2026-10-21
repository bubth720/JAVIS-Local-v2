# APK traçable et préparation Xiaomi

Aucune installation personnelle autorisée/effectuée. Aucun appareil POCO X8 Pro
ou Redmi Pad SE inspecté. La candidate reste l'application MVP003 distincte
`com.stark.jarvislocal.mvp003`, label diagnostic, signature debug. Ne pas changer
le certificat ou l'ID pour contourner une installation existante. Ne pas créer
une nouvelle version « opérationnelle » tant que l'inférence réelle est bloquée.

La CI MVP003 est effectivement en succès :
https://github.com/bubth720/JAVIS-Local-v2/actions/runs/39634143182
Source `faf0d4fde9103b039ffd208361ccfccfbe603658`, artefact
JARVIS-MVP-003-Android, id12360079506. Digest d'archive **annoncé par GitHub** :
`e20542ee7dc30ae2a667e51f4182744ee3123bcaa1e60db3742d470dbf6e2929`.
Sa redirection vers productionresultssa12.blob.core.windows.net a été refusée
par le proxy ; cette archive et son APK n'ont pas été téléchargées/rehachées.
Ne pas réutiliser le SHA APK ou certificat local pour cette APK Actions.
Les contrôles CI ne constituent pas une preuve JNI/GGUF Android réelle.

Sur téléphone, uniquement après autorisation : ouvrir le run connecté à GitHub,
prendre l'artefact correspondant à cette source, examiner son APK-INFO.json,
certificat et SHA256SUMS.txt, puis vérifier les octets téléchargés. Ne pas
inventer de hash APK à partir du digest ZIP. En cas de signature/version/package
inconnus ou incompatibles : arrêt, aucune désinstallation corrective.

Le kit local de cette mission, s'il est produit, possède son propre manifeste
et son SHA. Les sources applicatives restent byte-for-byte identiques à MVP003.
Ce kit local n'est pas une copie vérifiée de l'artefact Actions. Il ne contient
aucun modèle ni transfert de données entre packages. Le téléchargement de GGUF
et l'installation personnelle nécessitent leurs autorisations respectives.

Profils POCO et Redmi : appliquer les mêmes seuils de MvpSafety et la fiche
ANDROID-PROTOCOL.md ; relever les caractéristiques réelles avant toute décision.
Aucun profil plus permissif pour une machine réputée puissante ; si low-RAM,
ABI/API ou capteurs incompatibles, le résultat est NO-GO. Les interfaces
NEXUS/SERVICES/EVOLUTION et tous les pilotes Binder restent désactivés.

Retour arrière sans effacement : fermer la candidate, conserver son stockage,
reprendre l'ancienne application. Ne désinstaller, réinitialiser, abaisser une
version ou restaurer aucune base d'un autre package. SQLite schema2, souvenirs,
PDF/RAG et anciens imports restent inchangés. Aucun export/restore Android
applicatif n'est disponible ; ne pas promettre récupération après désinstallation.
Pas de sauvegarde ou extraction privée sans accord explicite.
