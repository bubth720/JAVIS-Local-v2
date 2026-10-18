# ALPHA1-001 — architecture pilote et chemin réel

## Décision technique réversible : chemin A, CORE autonome

Conserver le backend historique dans CORE pour cette tranche textuelle. Réutiliser
l'interface Views, SessionCoordinator, JarvisCore, InferenceController et sa factory
historique unique. Pas de service, application LLM déléguée, nouveau modèle,
second moteur, modification JNI ou extension du contrat d'admission.

Le chemin B Binder /2 conserve ses gates F3, journaux, confiance et revue de bundle.
Il n'est pas requis par A et n'est pas débloqué par cette mission. L'interface
Compose/Avatar séparée n'est pas nécessaire à un laboratoire textuel minimal.

## Chemin historique audité

MainActivity.send -> SessionCoordinator.send -> SessionActions.chat ->
JarvisCore.chat -> (réponses rapides/commandes/RAG ou inférence) ->
InferenceController.infer -> LocalLanguageModel.infer -> LlamaLocalEngine.generate
-> AndroidLlamaBackend -> Llama.complete / JNI de llama-android0.1.1.

- L'écran accepte la saisie via le single-flight ; refus occupé conserve le texte.
- AppGraph possède un seul InferenceController ; factory historique paresseuse.
- ModelManager attend filesDir/models/jarvis.gguf et vérifie taille exacte de
 l'asset embarqué + minimum historique + magic GGUF. Le nom Qwen3-1.7B Q4_K_M
 est la référence du script de préparation ; aucun modèle réellement chargé
 dans ce laboratoire n'a été identifié/mesuré. Pas de SHA de poids inventé.
- SettingsStore fournit SYSTEM séparément ; PromptBuilder prépare historique,
 documents/souvenirs et provenance avec borne UTF-8. Le backend applique ensuite
 son template natif. Cette borne n'est pas une mesure exacte des tokens finaux.
- ProcessInferenceAdmission réserve le propriétaire avant allocation et conserve
 sa réservation tant que la libération n'est pas confirmée ; état incertain bloquant.
 Le Mutex du moteur sérialise les appels ; aucun arbitre interprocessus déclaré.
- Le chemin historique écrit la question puis la réponse dans SQLite. MainActivity
 relit les messages ; SettingsStore persiste les réglages existants.
- Annulation : le contrôleur invalide son epoch et rejette les sorties tardives ;
 le coordinateur draine le travail avant de libérer son gate. requestStop du
 moteur historique retourne UNSUPPORTED : aucune interruption JNI attestée.
 Un retour JNI n'est pas un unload, et un timeout ne confirme aucune libération.

## Incrément laboratoire

MainActivity opt-in -> MÊME SessionCoordinator -> TextLabConversation (état RAM)
-> JarvisCore.chatVolatile -> MÊME helper inferPrepared -> MÊME InferenceController
-> MÊME LlamaLocalEngine / AndroidLlamaBackend.

TextLabConversation ne possède ni moteur, coroutine scope, base ni writer de
préférences. Il conserve au plus40 messages affichés et24 messages d'historique
réussi ; PromptBuilder sélectionne ensuite son contexte dans les limites existantes.
Les saisies sont bornées à2048octets UTF-8 avant insertion RAM ; ce n'est pas un
budget natif en tokens. Questions échouées/annulées exclues des relances.

L'acceptation d'un tour arme son jeton AVANT dispatch ; l'annulation le révoque.
Une demande annulée encore en file ne peut pas commencer une inférence plus tard.
Une demande entrée dans JNI doit toujours être drainée ; résultat tardif rejeté.
Les changements de mode passent par le même gate idle ; reprepareWhenIdle ne
réinitialise que la préparation UI, jamais modèle, quarantaine, epoch natif ou bail.
Le modèle peut rester résident entre modes ; aucune libération n'est annoncée.

Lab : pas de réponse rapide préfabriquée, /remember n'est pas une commande, aucun
RAG/PDF/ancien historique/mémoire personnelle consulté. Un résultat vide est une
erreur ; aucun texte de remplacement présenté comme réponse du LLM. L'interface
réutilise MessageAdapter et les états préparation/génération/erreur/attente après
annulation. Un appel réel au modèle est nécessaire à toute réponse de laboratoire.

## Préparation et confidentialité

Le laboratoire exige le GGUF déjà installé ET compatible avec l'asset attendu par
ModelManager. Il ne copie pas les modèles, ne prépare pas Whisper et ne télécharge
rien. Sans modèle, refus explicite. Le fichier seul hors mécanisme d'assets peut
rester refusé ; aucune procédure d'import ni abaissement de contrôle ajouté.
Le mode normal conserve sa préparation historique des deux modèles.

Les préférences existantes sont lues sans écriture et SYSTEM conservé tel quel.
Voix désactivée dans le lab, aucune initialisation TTS ; lecture existante arrêtée
à l'entrée, aucune modification du réglage speakReplies. Pas d'avatar/connecteur.
L'historique lab est effacé à la sortie et perdu à la mort du processus. Il peut
survivre à une rotation dans l'AppGraph vivant. Saisie exclue du saved state et de
l'autofill ; flag anti-capture et demande de non-apprentissage au clavier lorsqu'API
compatible. Ces mesures ne sont pas une garantie cryptographique contre l'OS/IME.

## Gates Alpha/Athéna non satisfaits

Le laboratoire hérite des plafonds contexte/sortie, du single-flight et des refus
historiques. Il n'importe pas SafetyPolicy du LLM séparé : aucune nouvelle
qualification RAM/thermique/batterie physique n'est acquise. Aucun seuil modifié.
L'APK sans poids n'est pas un assistant autonome validé.

IDENTITY reste désactivée : le port de mesure exacte manque, son diff JNI est en
attente, et Athéna n'a pas de contrat nouveau approuvé dans ce dépôt. Le manifeste
B VOUS/OFF est intact. Pas de portrait personnel ni pourcentages dans le runtime.
La qualification Athéna exigera le rendu/template/tokenizer réel du modèle,
SYSTEM borné, même worker/bail, accord de profil et réponses réelles évaluées.
