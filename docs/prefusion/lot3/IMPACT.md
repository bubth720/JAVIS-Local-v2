# Impact CORE-PF-02

Ajouts : ReadOnlyCoreUiLabPort.kt et son test JUnit, garde scope/inventaire,
workflow CI lot3, documentation. Instructions complétées en append-only.
Tous les fichiers de la mission source af6fe97 sont byte-identiques hors ces
appendices ; le garde le vérifie. Aucun code SessionCoordinator, AppGraph,
manifest, build/dépendance, SQLite, préférence, modèle, JNI ou fixture modifié.

L'instanciation OFF ne lit pas le bus. Le prototype synthétique utilise au plus
64 métadonnées d'instantané et 256 IDs en RAM, pas de texte utilisateur ni stockage.
Il ne lance aucun thread ni coroutine ; lecture à la demande uniquement. Ces bornes
ne sont pas une mesure PSS/batterie/thermique. Pas d'émulateur ou appareil qualifié
par l'exécution JVM. Pas d'effet de libération native, export SAF ou admission.
