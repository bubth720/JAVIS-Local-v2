# Matrice de risques résiduels

| Risque | État / preuve actuelle | Décision avant qualification |
|---|---|---|
| Règles ProGuard absentes | Référence corrigée dans le diff seulement ; vérificateur préparé et tests positifs synthétiques/négatif B1 réussis | Construire l'AAR corrigée après accord, vérifier proguard.txt puis DEX R8 ; pas encore corrigé dans un binaire |
| Constructeur stream après open | Constructeurs supprimés dans la proposition ; types valides | Vérifier lectures/écritures POSIX et les erreurs sur Android |
| Double fermeture / perte d'erreur | Propriétaire unique proposé ;15 tests helper JVM factices réussis | Confirmer Os.close Android, ne pas retry un numéro de FD après échec ambigu |
| Ouverture réussie, fstat destination échoué | FD fermé par finally ; identité insuffisante interdit suppression | Attendre blocage conservateur ; qualifier récupération du fichier orphelin sans reset aveugle |
| Lecture/écriture courte, EINTR, disque plein, fsync | Déroulement revu ; écritures partielles bornées et annulation vérifiée dans la boucle | Tests Android de défaillance non exécutés, pas de garantie de délai pour syscall bloqué |
| Nettoyage/unlink/close en erreur | Erreur primaire conservée ; nettoyage actuel non remplacé | Observer les erreurs supprimées et l'état retenu, ne pas assimiler absence de résultat à absence de fichier |
| Native incertain | Quarantaine et code natif inchangés, garde de préservation réussi | JNI réel reste non qualifié ; ni B2 ni intégration CORE autorisée |
| Concurrence/UI/BUSY | Aucun changement du worker/admission, résultats B1 historiques conservés | Nouvelle compilation puis nouvelle qualification ciblée après autorisation |
| Reproductibilité | Les observations A/C identiques et A/B différents de B1 restent historiques | Ne pas attribuer à ce diff une reproductibilité binaire non construite |
| Profil/SQLite/VOICE/IDENTITY/Binder |224 fichiers de base inchangés ; patch réduit à2 fichiers bibliothèque | Maintenir leur exclusion des étapes suivantes |

Les garanties syscall sont conditionnelles au fonctionnement d'Android et du
stockage. Root/même UID, défaut matériel, kill du processus et erreur de fermeture
ne sont pas résolus par le contrôle de types ou un simulateur. Aucun second
moteur ni nouveau droit d'admission n'est introduit.
