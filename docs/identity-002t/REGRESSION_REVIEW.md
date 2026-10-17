# Diff sensible et audit de régression

**Non approuvé, non appliqué, non compilé.** `PROPOSED_JNI_API.patch.txt` est un
diff de signatures contre LlamaJNI.kt du source amont épinglé dans
SOURCE_REFERENCES.json. Il n'est pas un patch installable complet : pas de corps
C++ ni wrapper public livré, et il ne doit pas être appliqué automatiquement.
Aucun dépôt amont, JNI, fichier applicatif ou protection modifié par cette mission.
La consigne utilisateur impose une validation spécifique avant cette partie.

## Surface à approuver avant développement sensible

1. Trois opérations JNI opt-in proposées : préparer/mesurer sans décoder,
   consommer un ticket préparé, abandonner ses seuls buffers. Une évolution
   versionnée de l'AAR doit être épinglée et reproductible ; ne pas écraser
   silencieusement la dépendance 0.1.1 ni accéder à ses internals par réflexion.
2. Implémentation C++ à préparer après accord dans le propriétaire de la
   bibliothèque : registre de handles/générations/tickets sûr contre ABA,
   rendu strict et tokenisation partagés, entrée UTF-8, tailles bornées et
   consommation des mêmes IDs. Ne pas construire un nouveau wrapper autour
   d'un pointeur LlamaCtx supposé à partir du binaire actuel.
3. Wrapper public de transaction scoped dans cette bibliothèque, sans publier
   handle ou table de tokens au CORE ; même worker qualifié et même modèle.
   Les appels existants load/complete/release restent compatibles et inchangés.
4. Dans CORE, futur appel de transaction depuis LlamaLocalEngine/AndroidLlamaBackend
   sous les verrous et l'admission existants ; IdentityComposer exécuté dans
   cette portée. LoadGeneration, invalidation du ticket et contrôle final de
   session sont nécessaires. Cette évolution de la zone protégée est suspendue,
   même si aucune méthode de ProcessInferenceAdmission n'a besoin de changer.

Une décision sur la bibliothèque responsable et la fabrication de l'AAR est
nécessaire : CORE dépend aujourd'hui d'un artefact tiers précompilé. Aucun dépôt
LLM ni dépendance nouvelle modifié ici. La présence de code tokenizer dans LLM
ne justifie ni un second moteur ni l'import d'une fixture /2 dans le runtime.

| Risque | Protection à maintenir / preuve future obligatoire |
|---|---|
| Deux modèles ou deux contextes pour compter | Zéro chargement par mesure ; même handle et même admission commune, compte des allocations réel |
| ABA de handle/rechargement | loadGeneration + ownerEpoch + leaseToken ; vieux ticket refusé même si adresse ou propriétaire réutilisé |
| Modèle/template changé après mesure | Ticket lié aux tokens retenus et paramètres ; refus avant decode, pas remesure silencieuse |
| Commutation de worker | Entrées mesurée/consommée dans la même portée série ; tests physiques du wrapper et de ses dispatchers |
| SYSTEM perdu par fallback amont | TEMPLATE_UNSUPPORTED dans le nouveau chemin, aucune concaténation en user |
| Coût BOS/prefill omis | Comparaison IDs préparés avec IDs réellement décodés, template réel et contexte effectif |
| KV modifié pour mesurer | Aucun decode, clear, embedding ou sampler pendant prepare ; état KV avant/après vérifié |
| Dépassement/overflow/OOM | Boundaries exactes, taille bornée, refus avant decode, quarantaine si état de ressources incertain |
| Annulation/révocation après mesure | Revalidation au point d'admission puis drainage et rejet tardif avant sortie/SQLite/TTS |
| Libération inconnue | Aucun reset de réservation, handle conservé, UNKNOWN/FAILED n'autorise aucun autre moteur |
| Résultat sans promptTokens | Ne pas lancer une génération pour mesurer et ne pas prendre tokensGenerated pour inputTokens |
| Encodage JNI différent | Même conversion des octets aux deux phases ; corpus multilingue et caractères supplémentaires |
| Régression CORE–VOICE | Fin de mesure != fin native != unload. Préserver TTS et ne pas considérer NativeHandoffGate comme une preuve réelle |
| Régression API24 / données | Six vecteurs sur API24/25/35, anciens tests inchangés ; aucune permission, SQLite ou migration modifiée |

## Gate de qualification ultérieur

Après accord précis sur la frontière proposée : construire l'AAR expérimental
hors activation globale et vérifier symboles/API, provenance du modèle et du
binaire, égalité exacte des IDs de tokens préparés/consommés sous même worker/bail.
Tester succès et tous les refus du tableau sur corpus synthétique et petit modèle
local préalablement autorisé, puis recette Android physique séparée (RAM, batterie,
thermique, annulation, historique et absence de second chargement). Ni compilation,
ni Robolectric, ni test natif Linux ne suffisent seuls à qualifier Android.
La personnalité reste désactivée par défaut même après cette qualification ; son
raccordement effectif au chat et l'activation demandent leur gate explicite.
