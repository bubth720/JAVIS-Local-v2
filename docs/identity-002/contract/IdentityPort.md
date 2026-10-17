# IdentityPort minimal proposé pour ID-002 — lecture seule

**Candidat001B.1, à accepter avant implémentation CORE.** Aucun transport ni code
CORE ajouté. Deux opérations logiques, pas API Binder/HTTP :

- readTechnicalManifest(id,expectedRevision,expectedDigest,hostContext) : retourne
  le manifeste technique approuvé et vérifié ou erreur fermée. hostContext vient
  du CORE de confiance, jamais d’un JSON/document ; vérifie pin de contrat et
  constitution. Lecture embarquée hors ligne possible, pas NEXUS/PC obligatoire.
- readTechnicalSnapshot(sessionId,manifestRef,policyRevision,now) : CORE construit
  une vue immuable purement technique pour la session active, sans écriture ni
  préférence personnelle ; consent_revision0 et trois listes de références vides.
  Vérifie manifeste/version/composer/style, session/annulation et expiration.

Aucun save/propose/approve/apply/export/restore ni import personnel sur ce port.
Schémas consent/préférences/propositions/décision/application servent à figer la
séparation des types et les vecteurs, pas à ouvrir leurs écritures en ID-002.
IdentityPolicy est autorité **hors données du manifeste** : service/capacité de
l’hôte CORE, non un JSON de permissions manipulable par le modèle. Une policy_revision
dans snapshot identifie la politique vérifiée de l’hôte, elle ne la remplace pas.

Après révocation/politique nouvelle : invalidation de snapshot, arrêt des sorties
concernées, pas mutation pendant un calcul. Changement non urgent à frontière de
session, validation des réponses contre snapshot/session courants ; aucun texte
assistant/TTS tardif autorisé. Utiliser la même garde SessionCoordinator et les
preuves de drainage existantes, pas un second coordinateur lourd.

Erreurs proposées : INCOMPATIBLE, MANIFEST_UNAVAILABLE, INVALID_CONTRACT,
UNAUTHORIZED, REFERENCE_MISMATCH, SNAPSHOT_MISMATCH, EXPIRED_OR_REVOKED,
CONTEXT_EXCEEDED, TOKENIZER_UNAVAILABLE, CAPABILITY_UNSUPPORTED, CANCELLED,
STALE_SESSION, CLEANUP_UNCONFIRMED. Le vérificateur offline expose des détails de
validation en plus ; aucune enum de contrat existant modifiée. Identité d’un
appelant et consentement doivent être établis hors de la représentation JSON.

Pour ID-002 : sélection d’une base CORE compatible après revue du dossier, mode
opt-in, manifeste embarqué technique, API24 et paramètres historiques inchangés.
Pas d’accès au Guard.Context d’un autre APK, nouvelle permission, moteur ou base.
Futur IdentityPort privé ID-003 (SERVICES initial candidat/NEXUS cible) = proposition
et autorisation séparées ; aucun port d’écriture « temporaire » pour contourner
ce périmètre. Les wrappers /2 de bootstrap restent totalement hors du parcours.
