# Protocole Android ARM64 — non exécuté

## Gate préalable

Autorisation explicite du téléchargement du GGUF public et du laboratoire.
Pour POCO X8 Pro / Redmi Pad SE, autorisation d'installation/essai distincte.
Ni appareil personnel ni émulateur n'ont été utilisés pour cette mission.
N'accepter aucune preuve issue d'un double de moteur, de Linux natif ou de
Robolectric comme inférence JNI Android. Aucun remplacement de JNI historique.

1. Vérifier package/certificat/version/SHA du kit choisi. Conserver les autres
   applications et données. Utiliser de préférence un Android ARM64 de laboratoire
   autorisé et propre, pas un téléphone personnel connecté par défaut.
2. Relever Android API>=24, ABI arm64-v8a, page size, capacité disque et mesures
   réelles. Ne pas déduire les capacités des noms POCO/Redmi. Pas de profil aux
   seuils réduits pour Redmi ; un appareil low-RAM est refusé.
3. Conserver le modèle hors dépôt/APK, après téléchargement séparément autorisé.
   Calculer SHA complet avec le lecteur hors ligne, examiner la licence épinglée,
   architecture/quantification/template/tokenizer, puis importer par SAF local.
4. Couper Wi-Fi/données et activer mode avion avant essais. TTS/micro/Binder/
   persona restent désactivés pour cette qualification textuelle. Pas de cloud SAF.
5. Vérifier avant et pendant le travail : RAM disponible>=1458548704 octets,
   pas lowMemory/lowRam ; batterie connue>20% hors charge, température batterie
   connue1..399 dixièmes°C, thermal NONE/LIGHT sur API29+. Les API24–28 utilisent
   seulement le proxy température batterie. Ne pas simuler des capteurs sûrs.
   Contexte2048, sortie128, budgets et threads historiques inchangés.

## Corrélation et deux questions inédites

Générer hors ligne un identifiant d'essai aléatoire et deux request IDs, les
consigner avec PID/incarnation du processus et SHA APK/GGUF. Aucune réponse
préenregistrée. Exemple de construction de questions publiques à renouveler :

- Q1 : « Essai {nonce}. Un bocal contient {a} billes ; on ajoute {b} billes.
  Combien contient-il de billes ? Réponds en une phrase française. /no_think »
- Q2 : « Pour le même essai, on enlève {c} billes du bocal précédent.
  Combien reste-t-il ? Réponds en une phrase française. /no_think »

Choisir a,b,c nouveaux avec c<a+b, sans rejouer un catalogue. `/no_think` est
une instruction de question, pas une fonctionnalité JNI garantie. SYSTEM reste
le champ configurable distinct ; ne pas activer Athéna ni changer les anciens
prompts enregistrés. Les réponses doivent être effectivement générées et
corrélées aux deux requêtes/session. Vérifier le suivi du contexte, pas seulement
la présence d'une chaîne. Ne pas remplir EVIDENCE-TEMPLATE.json avec ces exemples
comme si le test était déjà exécuté.

## Observer le chemin natif sans modifier la production

Lors d'un futur essai de laboratoire autorisé, un observateur natif approprié
(debugger/profiler de laboratoire compatible avec l'APK debug) doit attester
l'entrée et le retour de nativeLoadModel/nativeComplete/nativeReleaseModel,
la présence des SO historiques et les PID/horodatages corrélés. Un breakpoint
peut suspendre la sécurité temporelle ; aucune opération de debugger n'est
exécutée ou automatiquement prescrite sur appareil personnel. Valider d'abord
ce moyen d'observation sur laboratoire, sans changer les protections CORE.
Si aucune observation fiable ne peut être obtenue, conserver le blocage au lieu
de modifier JNI ou d'assimiler un message UI à une attestation native.

Le rendu du template/tokenisation réels et les IDs peuvent nécessiter une
observation native supplémentaire approuvée. Ne pas instrumenter silencieusement
la bibliothèque ni charger un deuxième tokenizer/moteur résident. Aucun test
exact prepare/consume/discard expérimental n'est autorisé par ce protocole.

## Séquence et mesures à conserver

| Étape | Action / preuve | Condition d'arrêt |
|---|---|---|
| Avant import | PID/API/ABI/page size, capteurs, RAM/disque, hashes | Source/hash/licence/autorisation inconnus |
| Import | Fichier local exact ; ancien modèle conservé si erreur | Hash/taille/header/espace refusé |
| Chargement explicite | Trace JNI entrée/retour + état contrôleur, RAM/PSS | Erreur, ressources dégradées, quarantaine |
| Q1 puis Q2 | IDs/session/horaires, texte public réel, retour JNI | Vide, réponse tronquée, fallback/template non qualifié |
| Travail concurrent | Seconde demande pendant calcul refusée avant nativeLoad | Deux admissions/charges observées |
| Annulation courte | stopRequested, travail encore actif, absence de réponse tardive | Ne jamais prétendre à un stop natif immédiat |
| Fin effective | Retour JNI observé, gate encore fermé jusque-là | Timeout/client fermé ne vaut pas fin |
| Déchargement explicite | Retour release JNI + modèleUnloaded confirmé, mesures après | Unknown/failed : aucune réadmission |
| Rotation/arrière-plan | Un runtime, annulation logique/drainage conservés | Relance native implicite ou doublon |
| Réouverture | Historique synthétique conservé, aucun warmup automatique | Erreur SQLite ; ne pas réinitialiser |

Mesurer avant/après chargement, chaque réponse, annulation et déchargement : RAM
available/lowMemory/lowRam, PSS/RSS propres à l'application si observables,
latence, batterie, charge, température et thermal. Les temps/coûts de chargement
peuvent dépasser un timeout ; le verrou doit rester fermé jusqu'à la fin effective.
Ne pas provoquer de surchauffe, épuisement de batterie ou pression mémoire pour
obtenir une panne. Aucun log système global, identifiant personnel ou donnée
privée à publier. Limiter les traces aux questions publiques de l'essai.

## GO / NO-GO

GO exige fichier réellement vérifié, licence identifiée, runtime Android ARM64,
deux réponses natives corrélées, prérequis sûrs, concurrence refusée, fin/release
observées et aucun contournement. Arrêt natif non disponible reste une limitation
explicite ; ne pas promettre une interruption thermique instantanée. Unknown ou
échec conserve le blocage ; ne pas effacer journal ou changer admission/signature.
Toute correction sensible doit faire l'objet d'un diff et d'une validation séparés.
