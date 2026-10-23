# ID-01 — profils éditoriaux locaux livrés, runtime non activé

CORE `bubth720/JAVIS-Local-v2`, branche `feat/id-01-intensive`, parent
`e86107b8213b0aeaf9d22025cf6bef91a8e544a5` (CORE-02), référence de fiche
`a6f13dd101990c6b5e3992be506c9a5b99873621` et planning `1b7ac9b35b5e6e133167fcaf1873e58b9db7288e`.

## Code utilisable

Dans `identity/profiles/` : ProfileDraft/IdentityProfile/ProfileReference,
ProfileRepository sur vraie SQLite privée noBackup de laboratoire,
création/sélection de deux presets, révisions avec conflit optimiste, SHA strict,
provenance et restauration par nouvelle révision. Les versions précédentes ne
sont pas réécrites.16 profils/64 révisions maximum ; corruption refusée, y compris
fichier SQLite invalide, sans suppression/réinitialisation automatique.

ProfilePreviewEngine expose six scénarios synthétiques de configuration,
DiffService et l'aperçu du SYSTEM technique B. Le formatter B a été extrait
**à l'identique** d'IdentityComposer, sans toucher politiques, mesures, budgets,
provenances ou requête USER. Le garde impose précisément ce seul refactor.
PersonaExpressionPort ne donne que contexte/style/tutoiement : aucun nom privé,
texte libre, souvenir, droit ou promesse de capacité acoustique.

Le repository n'est **pas composé dans AppGraph**, ne crée aucune base au
lancement de l'application et n'importe/migre aucune donnée historique. Il
s'utilise explicitement via create/read/list/revise/select/restore/history/close.
UI-02 peut préparer un port hôte sur cette API ; aucun IPC n'est activé.
La SQLite privée n'est pas présentée comme un chiffrement applicatif.

## Contrat et blocage maintenu

Les pourcentages et variantes sont un **format local d'éditeur proposé**, pas
jarvis.identity/1. proposedProfileSupport renvoie CONTRACT_EXTENSION_REQUIRED.
L'adaptateur compose seulement la baseline B approuvée avec l'ancien composer,
qui refuse toujours sans tokenizer natif exact. Aucune conversion en rôle USER,
aucun champ v1 ajouté et aucun nouveau modèle. Athéna reste inactive ; les
prévisualisations ne sont pas des réponses générées. L'évolution de contrat et
la qualification native restent nécessaires avant son activation réelle.

## Vérifications (23 octobre 2026)

Commande exacte dans VALIDATION-ID-01.json : tests debug + build debug + Lint +
fixture. Résultat final : `BUILD SUCCESSFUL in 2m 18s`,83 tâches15 exécutées/68
up-to-date. **334 tests debug réexécutés,0 échec/erreur/ignoré** ; dont30 nouveaux
cas (10 scénarios Robolectric SQLite NATIVE sur API24/25/35). La fixture /2 reste
36 résultats UP-TO-DATE, non comptés comme réexécutés. Lint :0 erreur,30 warnings
(résultat rapport partiellement réutilisé). Aucun test Android physique/JNI/LLM.

Première exécution :3 échecs sur331 tests ont révélé des clés de contexte
uppercase incompatibles avec le codec strict. Correction uniquement du format
éditeur en lowercase Locale.ROOT ; aucun codec ou test assoupli. La suite finale
ajoute3 tests de fichier SQLite corrompu conservé. Tests de fermeture/réouverture,
révision/conflit/restauration, historique SQLite intact, aperçus non génératifs,
SYSTEM égal au vrai candidat du composer (sonde de test qui refuse la mesure),
refus du tokenizer indisponible et validation des paramètres passent.

Démo sans installation :

```sh
bash tools/identity-cloud-env.sh gradle :app:testDebugUnitTest --tests '*IdentityProfilesTest*' --offline --no-daemon --max-workers=2
python3 tools/check-id-01.py
```

Le garde protège320 fichiers, vérifie la correspondance exacte avec l'ancien
renderer et l'absence de modification des contrats/manifestes B/JNI/admission ;
`git diff --check` passe. CI dédiée préparée, pas présumée réussie. Aucun merge,
installation, donnée personnelle dans Git ou migration de l'utilisateur.

Suite : MEM-01 isolée, puis ID-02 ; UI-02 et VOICE-02 peuvent réutiliser les
configurations/intentions de style livrées tout en refusant activation, transport
ou effet acoustique non disponibles.
