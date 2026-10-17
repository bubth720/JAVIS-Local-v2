# Port exact proposé — soumis à validation, non implémenté

Version de travail interne : `core.identity.measure/0-proposed`.
Ce texte ne modifie ni jarvis.identity/1, ni les contrats UNIFIED v1, ni fixture /2.
IdentityTokenBudgetPort actuel reste indisponible en production, persona désactivée.

## Une transaction dans le propriétaire, pas un compteur externe

Le futur port est créé seulement dans une portée privée du propriétaire d'inférence,
sous le gate InferenceController, le Mutex du moteur et sa réservation commune.
Ni UI, document, modèle, appelant IPC ou JSON ne peut fournir cette portée.
Le port ne charge aucun modèle, ne crée pas de moteur ni d'admission privée et
n'émet aucun reçu de libération. Pas de réflexion pour lire le handle interne,
JNA, dlsym, second context, modèle léger de tokenisation ou fallback texte.

1. Vérifier opt-in, contrat, capacités SYSTEM, snapshot/session/policy actifs,
   état non quarantainé et autorisations CORE avant toute préparation. L'admission
   d'allocation doit déjà être acquise avant chargement du modèle : ne jamais
   différer cette protection pour pouvoir compter. Distinguer cette admission
   existante du contrôle final qui autorise ensuite la génération.
2. Ouvrir une transaction sur le modèle déjà détenu, worker série identifié par
   le propriétaire. Le wrapper actuel Dispatchers.Default ne suffit pas : le
   nouveau chemin opt-in devra exécuter préparation et consommation dans le même
   worker natif/host qualifié, sans libérer les verrous entre ces étapes.
3. Construire la requête finale : politique textuelle hôte, SYSTEM configurable
   historique et fragment persona dans SYSTEM ; question et contexte inchangés.
   Le contexte contient l'historique et les passages sélectionnés. La provenance
   structurée reste séparée et incluse dans l'empreinte, même si ses identifiants
   ne sont pas tous rendus au modèle. Pas de réinterprétation d'un document comme
   permission, rôle système ou consentement ; pas de troncature silencieuse.
4. Dans le même propriétaire natif : sélectionner le template du modèle chargé,
   refuser absence/incompatibilité/erreur, produire le rendu complet (y compris
   BOS, marqueurs de rôles, préfixe assistant/prefill, séparateurs et éventuel EOS
   imposé par le template). Tokeniser UNE fois avec le vocabulaire et les flags
   exacts que consommera la génération. Ne pas additionner les comptes séparés :
   une frontière de fragments peut changer la tokenisation.
5. Conserver temporairement les IDs de tokens préparés avec un ticket opaque
   à usage unique dans le propriétaire. La préparation ne fait ni llama_decode,
   ni effacement KV, ni sampler, ni émission de réponse. Capacité insuffisante,
   allocation échouée, rendu non borné ou résultat incomplet : refus fermé.
6. Revalider juste avant admission du calcul la session, annulation, expiration,
   révocation/policy, état de ressources et tous les liens du reçu ci-dessous.
   Le contrôle doit être sérialisé avec le point d'admission du propriétaire.
   L'annulation concurrente après ce point peut arriver pendant JNI : drainer
   l'opération, invalider sa sortie, ne pas prétendre empêcher tout calcul tardif.
7. Consommer le ticket et les MÊMES IDs de tokens, sans rerendre le prompt ni
   changer modèle/template/flags. La limite de sortie est celle liée au ticket.
   Le ticket ne peut être réutilisé, échangé entre handles ou après rechargement.
   Un reçu de test ou des champs JSON valides ne doivent jamais créer un ticket.
8. Après retour réel, revalider epoch/session/snapshot avant SQLite, UI ou TTS.
   Une réponse annulée/révoquée n'est pas persistée ni lue. Détruire les buffers
   préparés devenus inutiles ; cela ne décharge PAS le modèle. Sur état natif
   inconnu, conserver les verrous/réservations selon les protections existantes.

## Reçu interne et liens obligatoires

Le reçu reste une structure du propriétaire, non une autorisation transportable.
Une représentation JSON JNI ne remplace pas l'identité du ticket dans son registre.

| Champ proposé | Autorité/validation |
|---|---|
| contractRevision | Révision interne exacte négociée, sinon refus |
| sessionId / requestId / requestDigest | Requête finale complète ; sources et limites incluses |
| controllerEpoch / policyRevision / manifestRef / snapshotRef | État CORE actuel, jamais accordé par le modèle |
| ownerEpoch / leaseToken | Propriétaire admis + réservation exacte ; pas un PID |
| loadGeneration | Incrément privé à CHAQUE nouveau chargement/context ; évite ABA même si leaseToken reste identique |
| modelSha256 / tokenizerBuildSha256 | Artefact modèle réellement chargé et binaire tokenizer qualifiés ; fichier figé pendant usage, pas simple hash tardif du chemin |
| templateSha256 / rendererRevision / encodingRevision | Template réel, paramètres du rendu et encodage exacts |
| addSpecial / parseSpecial / addAssistant | Flags réellement employés, pas supposés à partir du nom GGUF |
| renderedSha256 / tokenIdsDigest | Octets rendus et IDs réellement préparés/consommés, bornés |
| inputTokens / loadedContextTokens | Taille réelle de ces IDs et llama_n_ctx effectif du même contexte |
| admittedContextTokens / requestedContextTokens | Limites CORE en vigueur ; aucune hausse des seuils |
| outputReserve / marginReserve | Réserve de sortie complète et marge explicite liées à la requête |
| workerEpoch / preparedTicket / expiry | Portée locale série, ticket non forgeable/réutilisable, expiration monotone |
| kvEpoch / freshTurn | Pas de réemploi de cache implicite : préremplissage complet comme le mode historique |

Condition exacte :

`inputTokens + outputReserve + marginReserve <= min(requestedContextTokens, loadedContextTokens, admittedContextTokens)`

Vérifier bornes et additions sans overflow, compte valide et sortie > 0. Une
marge ne remplace jamais le coût réel du template. Aucun chiffre de marge ou
paramètre thermique/batterie n'est nouvellement qualifié ici. La limite existante
UTF-8 reste une protection historique ; elle n'est pas une preuve de tokens.
Les contrôles JSON, octets et taille de fragment persona restent également actifs.

## Encodage et compatibilité historique

Le diff propose ByteArray pour le nouveau chemin UTF-8 strict : octets validés,
conversion unique, aucune normalisation NFC/NFD, rejets explicites. L'ancien
jstring/GetStringUTFChars fait du UTF-8 modifié JNI. Ne pas changer ce comportement
historique implicitement ; tester BMP, emoji, paires de surrogates et NUL/refus,
puis qualifier le même encodage entre mesure et génération nouvelle. Pas de
comptage Kotlin UTF-8 couplé à une génération native en UTF-8 modifié.

Le fallback amont en concaténation est refusé dans le SEUL chemin mesuré proposé ;
l'ancienne entrée nativeComplete reste intacte tant qu'une migration distincte
n'est pas autorisée. La préparation échouée ne relance pas ce chemin historique
pour contourner le refus persona. Aucun changement au contrat du chat existant.

## Fermeture, erreurs et secrets

TOKENIZER_UNAVAILABLE, TEMPLATE_UNSUPPORTED, MEASUREMENT_MISMATCH,
STALE_MODEL, STALE_SESSION, CANCELLED, EXPIRED_OR_REVOKED, CONTEXT_EXCEEDED,
CLEANUP_UNCONFIRMED sont des refus internes proposés, pas de nouvelles enums v1.
Un échec de mesure ne crée aucun faux nativeDone/unload. Une perte de ticket ou
un état inconnu n'autorise pas un reset d'admission. Aucun journal de prompt,
source, mémoire, token IDs ou ticket dans Git/logs. La suppression des buffers
n'est pas une garantie d'effacement cryptographique de toutes copies Android.
