# C-VOICE-001 — C1 livrée, C2 natif bloqué, C3 arrêtée

ModuleCORE, dépôt `bubth720/JAVIS-Local-v2`, branche `feat/core-voice-pilot`.
Base réelle115dd3fe9b8e82b295b076dc045601a04d21a06f, main226e2e8cc3eebd479805b89d035eb5c1cbcad540. Ticket#7 fourni, API de lecture refusée (`Forbidden`) ; MISSION.md du dépôt lu comme instruction. C1 code05c087b ; commit C2 et clôture à consulter dans l'historique de cette branche.

## Ce qui est réellement livré

C1 : port SpeechOutputPort et session/corrélation/annulation testables, composition par défaut vers le même LocalTts, passage exact du texte historique y compris long. Pilote désactivé, API24/25 refusée pour VOICE sans retirer le TTS existant. Aucun fallback automatique, aucune dépendance ou permission supplémentaire.

C2 : audit démontrant l'absence de déchargement natif attesté et modèle local de barrière avec10 tests simulés. Le fournisseur de production est indisponible ; aucun bail réel n'est émis. Perte/timeout/erreur reste UNKNOWN, jamais une libération. Stop explicite selon MISSION.md ; C3, service/client Binder CORE et activation UI **non implémentés**.

Le dépôtVOICE sœur possède une fixture distincte d'authentification/refusV1, commit242eb65. Contrat `voice.bridge.fixture/1`, SHA256 cfbdff471ed0403113803a0f4c86ec75c57f723cc828f5af3c13fb3ef8042cf2, identique des deux côtés. Les AIDL CORE sont seulement documentaires ; le client de test est dans le dépôtVOICE et ne prétend pas être le CORE installé. Source005K9c018ecf… et UNIFIED inchangés.

## Résultats de cette session

- 57 tests CORE réussis,0 échec/erreur/ignoré :40 historiques (33 SQLite API24/25/35 +7 heure),7 C1,10 C2.
- Compilation APK et Lint réussis ;0 erreur/fatal,34 avertissements Lint restant dans cette base. Aucun compteur187 d'une autre branche n'est repris comme référence.
- APK inspecté : minSdk24, ABIarm64-v8a, permissions INTERNET et ACCESS_NETWORK_STATE absentes ; aucune nouvelle permission/service Binder. Les grands modèles ne sont pas présents dans cet APK local : assistant autonome non validé.
- Contrôle de préservation et contrat croisé réussi. LocalTts, Llama/Whisper, JarvisCore, SQLite, RAG/PDF, commandes, souvenirs, paramètres, WAV, manifeste et dépendances byte-identiques à la base. AppGraph ajoute seulement le port ; MainActivity ne change que la délégation de lecture.

Détails : TESTS.md, TEST-RESULTS.json, PRESERVATION.json. Aucun utilisateur/base privée, profilB, modèle réel ou appareil personnel manipulé. Aucun arrêt JNI ni génération native testé ; aucune qualification Binder entre APK ou audibilité. ADB du laboratoire est bloqué par son répertoire standard en lecture seule malgré autorisation ciblée ; aucun émulateur/KVM provisionné. Tous les résultats Android simulés/JVM sont distingués des validations physiques non exécutées.

## Limites et prochaine action

Les modèles historiques conservent leurs limites : releaseInternal masque l'erreur avant effacement du handle, Whisper n'a pas d'admission globale, timeout ne prouve pas la fin JNI. Aucun correctif hors périmètre, cherry-pick004A/B/C ou release concurrent n'a été tenté pour « débloquer » VOICE.

Une mission dédiée doit fournir drainage et déchargement réellement observables des deux moteurs, preuve d'échec propagée, persistance/récupération des réservations et autorité Binder qualifiée. Puis revoir le complément profil/session/bail avec VOICE et exécuter les fixtures Android sur émulateur. Garder le pilote inactif tant que ces conditions ne sont pas remplies.

PR **brouillon uniquement** vers main, sans fusion ; publication Git et création PR vérifiées séparément. Aucun workflow manuel de téléchargement/publication de modèles déclenché, aucune installation Xiaomi.
