# ALPHA1-003 — revue du patch non appliqué

Référence unique : PATCH-A-VALIDER.patch ALPHA1-002, SHA-256
`94f06d0179b95c332f5842b73b69d103e09ca319befc319a0aeca341735c6df7`.
Les constats portent sur les corps du patch et les sources exactes, pas sur un
moteur candidat exécuté. Le runtime CORE reste inchangé.

## Invariants présents dans le candidat, non qualifiés nativement

| Surface | Lecture vérifiée | Preuve manquante |
|---|---|---|
| Modèle/contexte | Registre liveModels, refus second load, même LlamaCtx, n_seq_max1 | Comptage allocations réelles, échecs/OOM |
| Worker | Executor single-thread partagé, owned vérifie thread::id | Concurrence, worker remplacé, JNI Android |
| Verrouillage | Gate CORE → Mutex moteur → worker → measuredMutex ; release attend le worker | Réentrée, UI, attente du callback, deadlocks réels |
| Pointeurs/générations | Recherche dans registre avant déréférencement ; génération monotone ; consume vérifie génération/KV | ABA forcé, handles invalides, corruption et échecs de destruction |
| prepare | Rôles SYSTEM/user distincts, rendu strict, IDs retenus, aucun decode/clear/load/contexte créé dans ce corps | Template du modèle réel, BOS/préfixe, mêmes IDs réellement décodés |
| consume | Ticket brûlé avant validation, pas de rerendu/retokenisation ; prefill depuis le vecteur retenu | Capture indépendante du prefill natif, batches, erreurs partielles |
| discard | Buffers seuls, pas unload ; mauvais ticket refusé si préparation présente | Erreurs JNI, zéroisation et fuite de buffers |
| Annulation CORE | Admission synchrone puis drainage et rejet session/epoch/stop | Aucun arrêt JNI attesté ; le nouveau chemin ne promet pas d'arrêt instantané |
| Quarantaine | Erreurs consume/cleanup conservatrices, bail CORE retenu | Qualification des erreurs connues/inconnues ; aucune sortie de quarantaine ajoutée |
| Anciennes méthodes | Signatures conservées, worker/registre changent leur exécution | Compatibilité comportementale à rejouer sur AAR candidate réelle |

La séparation prepare/consume a été vérifiée statiquement en mémoire. Les IDs
transmis au wrapper ne reviennent pas comme entrée de consume : le vecteur natif
est conservé. Cela ne prouve pas encore le contenu réel de ce vecteur au decode.

## Constats et décisions avant phase B

| ID / gravité | Preuve et périmètre | Action demandée |
|---|---|---|
| R01 — élevée, contrat non garanti | LlamaMeasured.loadFrozen utilise `source.parentFile` pour createTempFile. Pas de Context/dataDir ni validation du caractère privé du répertoire. Le chemin CORE prévu est privé, mais l'API publique n'impose pas cette propriété. | Exiger une destination privée détenue explicitement par l'hôte, bornée, sans chemin arbitraire promu en preuve. |
| R02 — élevée, risque public reproduit sur primitive | loadFrozen retourne depuis withContext(worker), sans transfert protégé contre l'annulation. Son `transferred=true` précède la livraison au caller. Le probe Kotlin reproduit une allocation factice non livrée après annulation. | Assurer transfert/nettoyage dans une portée propriétaire résistante à l'annulation ; échec release = handle/bail conservé et quarantaine. Le CORE appelle déjà sous NonCancellable : ce probe ne démontre pas une fuite dans ce chemin CORE protégé. |
| R03 — élevée, copie non bornée | Vérification initiale usableSpace/source.length puis boucle jusqu'à EOF sans borne cumulative. Une source qui croît peut dépasser la réserve initiale ; le hash final ne borne pas le travail. | Figer une limite de copie, refuser croissance/dépassement et préserver l'original. Tester disque plein, source mutable et interruptions. |
| R04 — élevée, périmètre | Le composite inclut cinq hunks CORE, dont app/build.gradle.kts qui remplace obligatoirement la dépendance de production par une AAR fournie. | Ne pas appliquer le composite pour une première fabrication bibliothèque seule. Préparer un patch7fichiers amont distinct à revoir, cinq hunks CORE différés. Aucun nouveau hash approuvé dans cette phase. |
| R05 — moyenne, risque UI/réentrée | nativeGetSystemInfo prend le même mutex global que toute génération ; getSystemInfo reste synchrone. releaseModel bloque également en attendant l'executor. | Tester appels depuis UI et réentrée du callback ; garder hors UI et définir un refus de réentrée avant attente bloquante. Aucun deadlock CORE confirmé par l'audit. |
| R06 — moyenne, nettoyage | releaseModel met handle0 après unload, puis peut échouer en supprimant le snapshot. Un retry voit handle0 et ne retente pas cette suppression. | Distinguer unload confirmé et nettoyage fichier ; préserver la quarantaine CORE sans transformer un échec de fichier en fausse résidence native. Nettoyage uniquement du fichier détenu. |
| R07 — qualification bloquée | Rendu réussi + hash template ne démontrent pas la sémantique SYSTEM. parse_special=true et ancien fallback restent deux chemins différents. | Qualifier chaque template autorisé avec corpus synthétique ; jamais concaténer SYSTEM en user sur refus. |
| R08 — reproductibilité limitée | Builds A/B natifs différents ; A/A identiques. Classes identiques à Maven, pas les cinq ELF compilés. | Fixer/revoir le contrat de fabrication avant de déclarer l'AAR reproductible ou équivalente à Maven. |

Le probe est un **reproducteur de contrôle Kotlin avec ressource factice**, pas
une application du candidat. Les lectures R01/R03/R04/R06 sont confirmées dans le
patch ; leurs effets matériels n'ont pas été provoqués sur Android.

## Autres risques à conserver dans les tests

- Callback final public : code hôte de confiance uniquement, sans I/O/suspension,
  chargement, release ou réentrée. Le contrat documentaire seul n'empêche pas tout abus.
- Le registre Llama est local à cette bibliothèque/processus, pas un arbitre Android.
  Il ne couvre ni Whisper ni VOICE ni LLM externe ; ProcessInferenceAdmission inchangé.
- Une génération native monotone protège consume contre un ticket d'un autre load.
  Les anciennes JNI utilisent un handle brut : conserver les tests ABA/alias,
  sans considérer un pointeur comme une capacité interprocessus sécurisée.
- Les exceptions JNI, UTF-8 incomplet, OOM et erreurs de prefill doivent rester des
  refus ; aucune réponse partielle promue en succès. Une annulation n'est pas nativeDone.
- read-only ne garantit pas l'immuabilité contre le même UID/root. Ni zeroisation
  volontaire ni suppression de fichier ne sont un effacement cryptographique attesté.
- Même package/classes/SONAME natifs : coordonnées Maven différentes ne permettent
  pas une coexistence sûre des deux AAR. Un seul ensemble Llama par processus cible.
- minSdk24/ARM64 et cinq exports vérifiés seulement sur la bibliothèque **amont**.
  Aucun résultat de cette inspection ne qualifie les trois JNI encore absentes.

## Non-régressions et frontière des modules

Les167 fichiers de base, AppGraph, contrôleur, admission, moteurs, SQLite, réglages,
prompts et laboratoire ALPHA1-001 sont identiques. Les299 tests réussissent sur
l'ancienne AAR, pas sur la candidate. Aucun budget RAM/thermique/batterie augmenté.
Contrats IDENTITY/manifeste B et fixtures Binder conservés ; pas d'adoption `/2`.
Le ticket de mesure/discard n'est ni un bail VOICE ni un reçu F3. CORE004D reste
bloquée ; aucune preuve review-only/synthétique n'est convertie en autorisation.
Les APK VOICE, INTERFACE, PREVIEW et les données privées ne sont pas utilisés.

**Avis : NO_GO pour le patch initial en l'état.** Révision documentaire et nouveau
hash à soumettre ; aucune correction native appliquée sous couvert de cette revue.
