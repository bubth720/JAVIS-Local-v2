# Qualification ultérieure en laboratoire

## Conditions préalables

1. Revoir cette branche sans fusion automatique. Résoudre le contrat d'enrôlement/réconciliation décrit dans REPORT.md dans une mission autorisée ; la génération reste bloquée jusque-là.
2. Fournir un Android ARM64 API29+ dédié. Aucun POCO X8 Pro, Redmi Pad SE ou application personnelle sans autorisation distincte. Ne rien installer automatiquement depuis ce guide.
3. Provisionner hors Git une clé de laboratoire commune aux seules fixtures. Ne pas utiliser une clé historique ni l'empreinte éphémère d'un ancien job CI. La sonde authorized de LLM003E possède le même applicationId que CORE : ne pas installer les deux ni remplacer une installation existante.
4. Pour signer CORE, renseigner par canal sûr `JARVIS_FIXTURE_KEYSTORE`, `JARVIS_FIXTURE_STORE_PASSWORD`, `JARVIS_FIXTURE_KEY_ALIAS`, `JARVIS_FIXTURE_KEY_PASSWORD`. Sans ces variables, l'APK CORE fixture est volontairement non signé. Ne pas consigner leurs valeurs ni transférer le keystore.
5. Vérifier hors ligne les deux APK : commit, contrat, applicationId, certificat, permission signature et absence de réseau. Fournir un modèle public déjà disponible dont le SHA-256 est vérifié. Ne pas télécharger un modèle ou un APK automatiquement.
6. Conserver les protections LLM de ressources et une Activity LLM réellement reprise ; le service seul n'établit pas l'état foreground requis. Prévoir en laboratoire un mode de fenêtre approprié aux deux fixtures, sans changer les protections du serveur.

## Matrice à exécuter après résolution du contrat

| Scénario | Preuve exigée | État dans cette livraison |
|---|---|---|
| LLM absent/incompatible, mauvais certificat/package/UID partagé | Refus avant génération ; permission et UID vérifiés par Android | Logique simulée partiellement ; noyau Android non exécuté |
| Hello valide | Deux processus/UID distincts, version/hash et epoch reçus | Client compilé, pas exécuté inter-APK |
| Deux requêtes concurrentes | Un seul propriétaire et aucun double chargement | Domaine et registre simulés ; Android non exécuté |
| Question synthétique avec SYSTEM distinct | JNI réel, GGUF réel, trace technique corrélée sans texte personnel | Non exécuté |
| Annulation avant/pendant/après, réponse obsolète | Aucun assistant tardif ; drainage attesté | Domaine/client simulés ; JNI Android non exécuté |
| Client tué, service tué, broker tué, ACK perdu | Journal conservé, admission bloquée, pas de fallback | Pertes injectées ; processus Android non exécutés |
| Redémarrage, déchargement inconnu, unload échoué | Réconciliation fraîche ou blocage, aucun reset | Journal/Robolectric et domaine testés ; reprise native bloquée |
| Retour historique | Libération attestée avant sélection ; pas de nouveau moteur sinon | Politique simulée, aucun basculement d'APK implémenté |
| CORE API24–28 | Fonctionnement historique conservé, fixture non installable | Tests historiques Robolectric ; appareil non exécuté |

Journal de qualification : commits, empreintes contrat/APK/certificat/modèle, version Android/ABI, deux PID/UID, session/opération/epoch/corrélation/annulation/autorisation/bail/génération, états, délais, preuves de fin et unload. Ne pas enregistrer SYSTEM, questions, réponses, documents, chemins privés ou secrets. Les PID servent à identifier les processus observés, jamais à prouver la libération.

Une compilation, un hello Binder, une réponse de catalogue ou une simulation ne valide pas l'E2E natif. Réussite uniquement si le chemin fixture CORE → Binder authentifié → LLM003E → JNI/GGUF → réponse corrélée et déchargement confirmé est observé. Sinon décrire le blocage sans effacer le journal ni affirmer une intégration opérationnelle.
