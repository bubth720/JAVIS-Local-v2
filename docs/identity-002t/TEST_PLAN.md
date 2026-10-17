# Recette et statut de preuve ID-002T

| Critère issue #8 | Vérification réalisée ici | Limite / gate restant |
|---|---|---|
| Six vecteurs | IdentityRuntimeTest, canonical bytes/SHA sur API24/25/35 | Robolectric, pas appareil |
| Dépassement/frontière exacte | Tests arithmétiques existants avec mesure simulée | Compte natif réel non exécuté |
| Prompt injection | Nouveau IdentityTokenizerBoundaryTest : charges hostiles conservées en user/contexte, aucune promotion SYSTEM ; port refuse | Pas de preuve de résistance sémantique d'un LLM |
| Provenance concurrente | Nouveau test de mutation de la liste source pendant refus de mesure ; copie immuable conservée | Pas un reçu natif ni une inférence |
| Annulation et révocation | Suites Session/Inference/LlamaLifecycle et IDENTITY existantes | Contrôles host/doubles ; pas stop JNI réel |
| Modèle/template changé entre mesure et génération | Protocole de ticket/loadGeneration et matrice de refus documentés | **NON EXÉCUTÉ / BLOQUÉ** : opérations mesurées natives absentes |
| Libération incertaine | Suites InferenceController/ProcessInferenceAdmission/LlamaLifecycle existantes | Doubles natifs ; pas Android physique |
| Un seul moteur | Admission historique et factories testées ; aucun code runtime modifié | Compteur des allocations natives du nouveau port non exécuté |
| Même worker/réservation | Audit Dispatchers.Default et contrat de portée | **NON EXÉCUTÉ / BLOQUÉ** : extension scoped non approuvée |
| Tokenizer/template natifs | javap/nm et lecture du source officiel épinglé | Inspection statique, aucune tokenisation réelle |
| Historique/build/Lint | Relancés, détails dans RESULTS.md et TEST_RESULTS.json | Modèles absents, aucun APK installé |

Les deux nouveaux scénarios sont exécutés sur trois SDK, soit six cas nouveaux.
Leur port d'observation appelle UnavailableIdentityTokenizer et refuse toujours :
aucun nombre exact de tokens, modèle natif ou reçu de calcul n'y est simulé.
Les anciens tests contenant des mesures simulées restent étiquetés comme tels.
Aucun test existant modifié/désactivé et aucun test skipped ajouté pour les gates
bloqués : ils restent des scénarios non exécutés, pas des succès ou ignores JUnit.

## Critères reproductibles après approbation (non exécutés ici)

1. Préparer le même SYSTEM, contexte/historique, question et sources sur le même
   handle que la génération ; comparer rendu et séquence exacte de tokens, BOS
   et préfixe assistant inclus. Charger le modèle une seule fois.
2. Tester input+output+marge == capacité, puis +1 ; +1 refuse avant premier decode.
   Tester les trois capacités demandée/chargée/autorisée et overflow explicite.
3. Changer modèle, template, contexte, loadGeneration, lease, workerEpoch, policy,
   session, requête ou provenance après mesure : zéro consommation du vieux ticket.
4. Répéter/expirer/falsifier/échanger un ticket ; refuser sans lancer de travail.
5. Révoquer/annuler avant admission : aucun decode ; pendant JNI : drainer,
   réserver jusqu'à fin réelle, jeter la réponse, pas d'écriture/TTS tardif.
6. Rendu absent/incompatible, erreur de tokenisation, UTF-8 invalide ou encodeur
   différent : refus sans fallback ni modèle de substitution. Inclure emoji,
   NFC/NFD, marqueurs spéciaux, NUL refusé et documents manipulateurs.
7. Échec de cleanup, unload inconnu et réponse tardive : pas de reset, de seconde
   admission ou de fausse preuve DONE_UNLOADED, y compris pour VOICE.
8. Rejouer l'historique API24, SQLite et six vecteurs sans changer les références.
