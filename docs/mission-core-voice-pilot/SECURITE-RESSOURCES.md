# C2 — stop de sécurité : déchargement réel non qualifié

## Constat sur cette branche, pas sur les branches004A/B/C

Sources inchangées : `LlamaLocalEngine.kt`, `WhisperLocalEngine.kt`, `AppGraph.kt`, `MainActivity.kt` à la base115dd3f/226e2e8.

- `warmUp/ensureLoaded` conserve LlamaModel entre requêtes. Une réponse rapide ou une fin de chat ne signifie pas que le modèle est déchargé.
- `releaseInternal` est privé ; `runCatching { Llama.releaseModel(it) }` masque une éventuelle exception puis efface le handle. Ni l'effacement ni `isLoaded=false` ne constituent une preuve de libération réussie.
- Aucun arrêt natif attesté n'est exposé par le wrapper applicatif. `withTimeout` annule l'attente coroutine, pas forcément JNI. Appeler release en parallèle serait dangereux ; aucune telle modification n'est faite.
- Whisper charge/transcrit/libère dans finally, sans protocole partagé d'admission/résidence/attestation ni drain global avec Llama. Un finally ou un timeout ne garantit pas un nettoyage réussi.
- CORE main n'expose aucun broker lourd authentifié interprocessus. Les verrous d'autres branches ne sont pas intégrés ici ; leur présence ailleurs ne prouve pas la sécurité de cette base.

## Décision d'implémentation

C1 livré ; **C2 natif bloqué, C3 non commencé**, conformément à MISSION.md : « Si le wrapper actuel ne peut fournir déchargement sûr, livraison C1 + documentation et tests C2 simulés, puis stop gate explicite. » Le pilote de production reste DisabledVoicePilot, sans flag d'activation. Aucune demande ni autorité globale envoyée à VOICE. Aucun fallback automatique.

`NativeHandoffGate` est un modèle local testable, sans connexion au JNI ni à Binder et sans allocation de RAM. Son fournisseur par défaut ne produit **aucune preuve**. Les snapshots injectés dans les tests sont des simulations de confiance, jamais des booléens IPC. Réservation unique, epoch/génération/session/corrélation/bail, deadline et tombstones bornées ; stop n'est pas release, perte/timeout/erreur conserve le slot UNKNOWN et aucune méthode reset n'existe. Une confirmation simulée exige indépendamment nativeDone, audioStopped et modelUnloaded. Le modèle ne fournit pas de réconciliation interprocessus ou de persistance de baux et n'est pas un arbitre global opérationnel.

Ne pas remplacer le fournisseur par `!llm.isLoaded()`, une valeur issue du payload, un délai, un PID disparu ou un retour de coroutine. Un futur fournisseur nécessite des moteurs contrôlables, drainage/arrêt observables, libération qui propage les erreurs, confinement après échec, preuves fraîches pour LLM et Whisper et persistance/récupération authentifiées. Cela doit faire l'objet d'une mission dédiée/revue ; ne pas cherry-pick les branches précédentes sans examiner leurs dépendances.

## Tests et limites

10 tests C2 isolés couvrent absence de preuve de production, états des deux participants, stop/done avec résidence, anciennes epoch/génération, exclusivité, libération unique et ancienne réservation, annulation/résultats obsolètes, timeout/décès/nettoyage échoué, preuves de fin audio/natif/unload séparées, redémarrage bloqué et anti-rejeu.

Pas de modèle réel, JNI, mesure PSS/thermique/batterie ou Android Binder exécuté pour cette barrière. Aucun changement des seuils, moteurs, voix, données ou dépendances pour contourner le blocage.
