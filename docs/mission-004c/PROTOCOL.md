# Correspondance CORE — LLM 003E

## Transport et confiance

Package client/broker : `fr.jarvis.fixture.core` ; service broker `fr.jarvis.fixture.core.P2ArbiterService`.
Endpoint explicite : `fr.jarvis.fixture.llm/fr.jarvis.fixture.llm.P2EndpointService`.
Permission : `fr.jarvis.fixture.llm.permission.BIND_P2`, définie par l'endpoint, protection signature.

Les AIDL proviennent exactement du commit indiqué dans `fixtures/p2-client/REFERENCE.json`. La version et l'empreinte sont distinctes du contrat interne CORE version1. Le bundle comprend les chemins triés de CONTRACT.txt et des AIDL : chemin UTF-8, NUL, octets exacts, NUL. Le digest d'une requête utilise les longueurs et le nombre de champs uint32 big-endian, tri lexicographique sans normalisation Unicode. Vecteur publié testé depuis les ressources de test.

Avant bind : package/UID du service, export, activation, permission et propriétaire de permission, signature courante unique égale à celle de la fixture CORE. UID partagé, rotation et certificat multiple refusés. Avant décodage de la transaction broker : `Binder.getCallingUid`, package LLM unique, UID attendu et certificat ; taille Parcel bornée. Callback émis par le vrai processus CORE. Le JSON ne constitue jamais une autorité.

## Mapping

| CORE | 003E |
|---|---|
| `sessionId` | `session_id` UUID canonique |
| `requestId` | `operation_id` UUID canonique |
| Hello du serveur | `epoch` UUID réel du processus LLM ; jamais l'epoch local du broker |
| Réservation broker | `correlation_id`, `cancellation_id`, `authorization_ref`, `lease_id`, `generation` |
| `systemPrompt` | `system`, séparé même lorsqu'il est vide |
| `question` | `question`, corpus synthétique fermé |
| `context`, `sources` | Refus si non vides, y compris contexte uniquement blanc ; aucune conversion ni omission |
| `GenerationLimits` | `context_tokens`, `output_tokens` bornés selon 003E |
| Configuration synthétique explicite | SMALL/MAIN, SHA public vérifié, profil autorisé, remaining_ms ; aucun chemin de modèle |

La réservation est persistée avant enregistrement d'une autorisation. La query autorisée doit être identique à la query enregistrée : clé complète, digest, modèle, hash, profil, budgets, durée et UID des deux processus. Une seule réponse positive, expiration monotone au plus tard à la deadline du CORE. Aucun texte n'entre dans ce registre.

`AUTHORIZING` n'est pas une admission. `STOP_REQUESTED` n'est pas une fin. Le résultat n'est consommable qu'avec corrélation exacte, code NONE, native_ever_started, phase UNLOADED, effective_end/model_unloaded/release_confirmed vrais, empreinte modèle attendue, digests de rendu/template présents et budgets natifs compatibles. Annulation ou dépassement de deadline interdit tout texte final. Aucun historique assistant n'est écrit par la fixture.

## Récupération et limites

Perte du client/service, ACK perdu, timeout, receipt inconnu/erroné, corruption du journal ou échec de déchargement conserve la propriété et bloque une nouvelle admission. La mémoire des requêtes autorisées ne survit pas au redémarrage ; elle ne doit jamais être reconstituée à partir d'une ancienne query pour relancer une opération.

`PilotArbiter.reconcile` est une entrée de domaine pour des preuves futures authentifiées. Aucun fournisseur de preuves de production n'est implémenté car 003E ne fournit pas les attestations initiales et de reprise nécessaires. Ne pas brancher cette entrée à un bouton ni utiliser les preuves de test en production. Ni hello ni PID ni mort Binder ne valent preuve.

Les protections CORE004B restent dans le processus historique. Elles ne sont pas remplacées par cet arbitre ; l'APK historique n'est pas enrôlé. Pas de basculement inter-APK, pas de transfert documentaire, pas de garantie d'exclusivité système.
