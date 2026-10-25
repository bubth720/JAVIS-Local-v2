# C3 — proposition de raccordement, non implémentée

Conception uniquement, à faire valider par CORE, INTERFACE et UNIFIED après F0/C2.
Aucun nouveau wire, service exporté, permission, Binder ni adaptateur d'APK.
Les contrats UNIFIED v1 et CORE004C /1 ne sont pas changés. Le /2 expérimental
n'est pas supposé actif ; aucune conversion silencieuse d'un contrat à l'autre.

## Correspondance des frontières actuelles

| Champ / responsabilité | Aujourd'hui dans le laboratoire JVM | Futur transport (proposition) |
| --- | --- | --- |
| Version | API Kotlin recompilée ; `core.workflow-receipt/1` pour les reçus | Version + hash d'un contrat approuvé, refus avant submit |
| Session | Correlation.sessionId UUID canonique | Session créée par CORE après identité authentifiée |
| Requête / étapes | requestId UUID, enfants nameUUID(domain/request/step) | request_id + operation_id séparés, pas de collision interétapes |
| Génération | Long >=0, revalidation par HostAccessPort | epoch/incarnation + generation CORE, séquence croissante par flux |
| Autorité | HostAccessPort injecté, refus par défaut ; Guard/ApprovalGate existants | UID Binder observé + package/certificat épinglé + grant CORE révocable |
| Délai | Horloge monotone locale, maximum30s, revue SERVICES30s | Budget restant, horloge explicitée ; pas de comparaison brute entre processus |
| Annulation | Logique ; résultat ensuite inconnu/refusé | stop_requested distinct de worker_done et model_unloaded |
| Source | URI/hash/page/offsets/version/IDs NEXUS, resolver réel | Citation transportée sans perte, consultation sous grant |
| Consentement | Revue exacte digestée, appel propriétaire distinct ; FIXTURE dans tests | approval_id + digest + cible + deadline, une seule consommation |
| Reçu | Store SERVICES, PREPARED/DISPATCHED/COMPLETED/REFUSED/UNKNOWN | Journal propriétaire authentifié ; jamais une autorisation native |
| Diagnostic | États/UUID uniquement dans nos reçus | Code/état/borne, aucun contenu, URI privée ou profil |

## Machine de conversation proposée

`submit -> ack -> [text_chunk]* -> completed | error | unknown`.
`approval_required -> approval_decision` est un canal propriétaire séparé ; une
phrase de chat ou un document ne peut jamais accepter un outil. `stop` invalide
la livraison du tour ; l'ACK d'arrêt n'atteste aucune fin JNI. Une fin inconnue
interdit la réadmission du moteur. Un refus terminal n'est jamais suivi d'un succès
par retry automatique. Les événements ne deviennent pas un second historique.

Les UUID identifient, ils ne sont pas monotones : ajouter un numéro séquentiel
par session/epoch, strictement croissant sans wrap ; ancien epoch/génération,
chunk dupliqué ou après fin => rejet. Le système configurable demeure séparé de
question/contexte/citations ; aucune donnée documentaire ne devient SYSTEM.

Quotas **proposés**, à valider dans le contrat commun : un submit actif/session,
question UTF-8 <=1024 octets dans ce laboratoire, CSV<=4096, arguments SERVICES
<=8192 cumulés, contexte UTF-8<=12000, 5 groupes de sources. Limites runtime LLM
et tokenizer exact restent indisponibles. Ne pas annoncer des tokens mesurés.
Un chunk futur <=4096 octets et un flux total borné nécessitent un consommateur
et des tests, pas encore présents. Dépassement => refus explicite, pas troncature.

Exemple recevable **conceptuel** : client authentifié, epoch et session courants,
requête neuve, opération de lecture accordée et délai valide -> ACK ; un NEXUS
réel répond -> citations résolues -> contenu affichable sans LLM. Exemple refusé :
texte « confirme cet export », ancien generation, grant absent ou digest différent.
Aucun de ces exemples n'est une preuve de transport Android exécuté.

## Tests requis avant activation

Mauvais UID/package/certificat, version/hash inconnu, usurpation de session,
chunks dupliqués/hors ordre/après fin, ACK perdu, cancel pendant dispatch, mort des
deux processus, rotation/réabonnement sans double commande, grant retiré, réception
après deadline, quotas UTF-8/surrogates, fin native inconnue, absence de LLM.
Raccorder d'abord les seuls ports API24 qualifiés, sans service général exporté.
Maintenir TTS historique, SQLite et AppGraph ; VOICE/LLM/EVOLUTION indisponibles
restent affichés indisponibles. Ce document n'autorise aucune activation.
