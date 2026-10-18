# ALPHA1-001 — incrément réellement livré

Branche feature/alpha1-001-text-lab, créée depuis71831913 sans fusion.
Statut : tranche textuelle de laboratoire implémentée et testée en simulation ;
conversation locale réelle et Athéna NON QUALIFIÉES.

## Fichiers applicatifs

- AppGraph.kt : unique moteur/factory conservé ; routage opt-in du même
 coordinateur vers le lab ; préparation GGUF seule sans copie/Whisper ; erreurs
 affichées sans log SQLite ; voix/PDF/micro non utilisés dans ce mode.
- JarvisCore.kt : chatVolatile sous le même verrou ; helper inferPrepared partagé
 pour validation réponse/corrélation. Chemin normal garde commandes/RAG/SQLite.
- SessionCoordinator.kt : re-préparation UI à l'arrêt et hook de tour accepté
 avant dispatch. Gate, arrêt natif, drainage et admission moteur inchangés.
- MainActivity.kt : bouton explicite Laboratoire texte, confirmation, sortie et
 annulation ; réutilisation de l'interface et du message adapter ; confidentialité
 de saisie ; mode normal sélectionné par défaut.
- activity_main.xml : deux contrôles ajoutés, annulation visible seulement en lab.
- TextLabConversation.kt (nouveau) : état/historique volatile borné, jeton de tour,
 invalidation des réponses tardives, aucun accès personnel durable ni moteur.

## Activation documentée, aucune commande d'installation exécutée

Sur une future installation autorisée, le bouton Laboratoire texte demande une
confirmation. Il faut finir le travail courant et envoyer/effacer la saisie
historique avant entrée. Sortir efface les messages lab puis re-prépare le mode
historique ; l'ancien historique revient, sans duplication des messages lab.
Le statut cliquable permet de réessayer une préparation échouée.

Entrée explicite de laboratoire également disponible par extra booléen
`com.stark.jarvislocal.TEXT_LAB=true` de MainActivity, consommé au lancement :
les tests l'utilisent pour éviter la préparation/lecture SQLite historique.
Cet extra n'est ni une permission ni une autorisation IDENTITY. Il n'introduit
aucun endpoint ou service. Aucun lancement/install APK effectué par cette mission.

Arrêter une réponse demande l'annulation et maintient busy jusqu'au drainage.
Un moteur résident peut subsister après annulation/retour normal ; le bouton
n'annonce pas un unload et ne permet pas de sélectionner un autre backend.

## Tests nouveaux

- TextLabConversationTest : opt-in, effacement, bornes, historique réussi,
 erreurs, annulation tardive, changement de mode refusé occupé, demande annulée
 avant worker et saisie trop longue refusée avant historique/inférence.
- TextLabCoreTest API24/25/35 : vrai CORE/contrôleur/SQLite, backend simulé ;
 question+relance, une seule factory/charge, pas de mémoire/DB/préférence modifiée,
 commandes transmises comme texte, vide/budget/quarantaine refusés, drainage.
- TextLabActivityTest API24/25/35 : vrai graphe sans poids ; refus modèle absent,
 contrôles désactivés, aucune ouverture DB lab, saved state et anti-capture.

Les tests existants sont intégralement préservés, sans désactivation. Les réponses
synthétiques appartiennent seulement au banc de test, jamais au runtime.

## Préservation et outillage

LlamaLocalEngine, AndroidLlamaBackend, InferenceController, ProcessInferenceAdmission,
InferenceContract, ModelManager, SQLite, SettingsStore, PromptBuilder, TTS/Whisper,
contrats IDENTITY, fixtures004C, dépendances, manifeste et workflows restent identiques.
Aucune API JNI ou règle d'admission native modifiée. Aucune nouvelle permission.
AGENTS.md et PROJECT_KNOWLEDGE.md sont actualisés conformément aux instructions locales.
`tools/check-alpha1-001.py` vérifie les fichiers protégés contre la base exacte,
la liste des fichiers nouveaux et le bundle approuvé, sans modifier les anciens guards.

Le nouveau périmètre autorise cinq modifications applicatives existantes. Les
anciens contrôles absolus « app identique » ne peuvent pas tous rester verts ;
leurs résultats et leur portée sont conservés explicitement dans VALIDATION.md.
