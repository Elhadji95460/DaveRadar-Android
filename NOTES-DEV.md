# Dave Radar Android — Note développeur

*Mise à jour : 2026-10-04*

Quatre défauts ont été corrigés et une mise en page tablette ajoutée, le tout
vérifié sur une Oukitel RT1 sous Android 11 : le point GPS n'était jamais retenu
au profit d'un point réseau 33 fois moins précis, la position de l'utilisateur
était indiscernable des appareils détectés, la carte débordait sur la liste
voisine, et les barres de signal s'affichaient à l'envers.

---

## Environnement et chaîne de build

Le projet n'a **pas de wrapper Gradle** (`gradlew` absent) et n'est **pas un
dépôt Git** : la compilation passe par un Gradle et un JDK installés hors du
projet, dont les chemins ne sont inscrits nulle part dans le dépôt.

| Élément | Valeur |
| --- | --- |
| Projet | `~/DaveRadar-Android` — Kotlin + Jetpack Compose |
| `applicationId` | `com.daveradar.app` |
| SDK | `minSdk 26` · `targetSdk 34` · `compileSdk 34` |
| Compose | BOM `2024.06.00`, extension compilateur `1.5.14` (Kotlin 1.9.24) |
| Cartographie | `org.osmdroid:osmdroid-android:6.1.18` (OpenStreetMap, sans clé d'API) |
| JDK | Temurin `17.0.20+8` — `~/android-dev/jdk-17.0.20+8/Contents/Home` |
| Gradle | `8.7` — `~/android-dev/gradle-8.7/bin/gradle` |
| SDK Android | `~/android-dev/sdk` (déclaré dans `local.properties`) |
| `adb` | `~/android-dev/sdk/platform-tools/adb` |
| Appareil de test | Oukitel RT1 — Android 11 (API 30), 1200×1920 à 320 dpi |

Commande de compilation effectivement utilisée :

```bash
export JAVA_HOME=~/android-dev/jdk-17.0.20+8/Contents/Home
~/android-dev/gradle-8.7/bin/gradle assembleDebug
```

L'APK sort dans `app/build/outputs/apk/debug/app-debug.apk` (environ 15 Mo).
Sur la tablette, le débogage USB doit être actif : sans lui l'appareil n'expose
qu'une interface USB de classe 6 (PTP, transfert de photos) et reste invisible
d'`adb`.

---

## Correctifs et causes racines

Aucun des quatre défauts n'était dans la logique métier : trois venaient d'un
comportement par défaut non explicité (ordre des règles, alignement, rognage),
le quatrième d'une icône non personnalisée.

| Symptôme | Cause racine | Correctif | Fichier |
| --- | --- | --- | --- |
| Position affichée jusqu'à 100 m de la réalité | Les deux fournisseurs écrivaient dans le même `LocationListener` sans filtre : la dernière position reçue gagnait, et le réseau est plus bavard que le GPS | Arbitrage `estMeilleure()` avec priorité GPS | `sensors/SensorsManager.kt` |
| Utilisateur introuvable sur la carte | Le marqueur « Moi » utilisait l'icône osmdroid par défaut, identique à celle des appareils, et était recouvert par la grappe | Marqueur dédié dessiné au `Canvas`, ajouté en dernier dans `overlays` | `ui/CarteScreen.kt` |
| La carte recouvrait la liste voisine | Compose place les vues Android natives dans un conteneur en `clipChildren = false` : la `MapView` dessinait hors de ses limites | `Modifier.clipToBounds()` sur le conteneur de la carte | `ui/CarteScreen.kt` |
| Barres de signal inversées | `Row` sans `verticalAlignment` : Compose applique `Alignment.Top`, les barres s'allongeaient vers le bas | `verticalAlignment = Alignment.Bottom` | `ui/CommonUi.kt` |

### Arbitrage des positions — le piège de l'ordre des règles

Mesures relevées sur l'appareil via `dumpsys location` : fournisseur réseau
`hAcc=100`, fournisseur GPS `hAcc=3` avec 16 satellites. Le réseau remplaçait
donc en continu un point à 3 m par un point à 100 m.

Une première version du correctif plaçait la règle de péremption **avant** la
préférence GPS — un point réseau seulement 35 s plus récent chassait encore le
GPS. L'ordre définitif de `estMeilleure(candidate, actuelle)` est donc :

1. Aucune position retenue → accepter.
2. Candidat plus ancien que la position retenue de plus de `PEREMPTION_MS` → rejeter.
3. Candidat GPS, position retenue réseau → **accepter sans autre condition**.
4. Candidat réseau, position retenue GPS → accepter **uniquement** si le point
   GPS dépasse `PEREMPTION_GPS_MS`.
5. Même source → trancher à la précision, puis à la fraîcheur.

Constantes : `PEREMPTION_MS = 20_000` · `PEREMPTION_GPS_MS = 60_000` ·
`TOLERANCE_PRECISION_M = 50f`. Le GPS est désormais demandé en premier et à 1 s
d'intervalle (le réseau à 2 s), ce dernier ne servant plus qu'à fournir un
premier point rapide.

### Rognage des vues natives

Le point à retenir dépasse cette application : **toute `AndroidView` dans
Compose peut dessiner hors de ses limites**, car `AndroidViewsHandler` désactive
le rognage des enfants. Le défaut était invisible tant que la carte occupait
tout l'écran ; il n'est apparu qu'en affichage à deux volets. Tout composant
natif placé à côté d'un autre doit porter `clipToBounds()`.

---

## Mise en page adaptative tablette

L'application était conçue pour un téléphone : aucun marqueur grand écran dans
le code (`WindowSizeClass`, `BoxWithConstraints`, `NavigationRail`, dossier
`values-sw600dp`). Elle s'installait sur tablette, mais l'interface était
simplement étirée.

Un fichier a été ajouté, `ui/WindowSize.kt`, qui expose l'énumération
`LargeurEcran` et la fonction `largeurEcran()`. Elle lit
`LocalConfiguration.current.screenWidthDp` **plutôt que d'ajouter la dépendance
`material3-window-size-class`** : la configuration suit déjà les rotations et le
mode multi-fenêtre, et cela évite une bibliothèque supplémentaire.

| Largeur | Classe | Disposition |
| --- | --- | --- |
| < 600 dp | `COMPACTE` | `NavigationBar` en bas, un écran à la fois — comportement téléphone **strictement inchangé** |
| 600–839 dp | `MOYENNE` | `NavigationRail` latéral, un écran à la fois |
| ≥ 840 dp | `ETENDUE` | `NavigationRail` + liste (38 %) et carte (62 %) côte à côte, séparées d'un filet de 1 dp |

Mesures relevées sur la RT1 (`dumpsys window`) : `sw600dp w960dp h560dp`. Elle
tombe donc en `ETENDUE` en paysage (960 dp) et en `MOYENNE` en portrait
(600 dp) — les deux seuils sont exercés par le même appareil selon son
orientation.

Deux conséquences dans `ui/AppRoot.kt` :

- En `ETENDUE`, les destinations Carte et Appareils sont réunies : le rail
  n'affiche plus que **Radar** et **Paramètres**, puisque les deux écrans sont
  visibles ensemble.
- Les paramètres sont contraints à 720 dp de large et centrés : ce sont des
  lignes de texte, inutile de les étirer sur 1280 dp.

`CarteScreen` a reçu un paramètre `showDeviceList: Boolean = true`, qui masque
son panneau de liste du bas quand la liste occupe déjà son propre volet.
**La valeur par défaut préserve exactement le comportement téléphone**, aucun
appel existant n'a été modifié.

---

## Vérification sur appareil

Compilation réussie, APK installé par `adb install -r` et application lancée sur
la RT1. **En paysage uniquement**, les points suivants ont été constatés à
l'écran :

- Barre d'état affichant `GPS ±3 m` en vert, là où le réseau donnait ±100 m
  avant correctif.
- Position de l'utilisateur en pastille bleue cerclée de blanc, distincte des
  42 appareils détectés.
- Frontière nette entre les deux volets, cartes d'appareils entières (nom,
  distance, barres).
- Barres de signal montant de gauche à droite, et décroissant avec la distance
  (3 barres à 0,8 m, 2 barres à 2,5 m) — le niveau suit bien le RSSI.
- Identification fonctionnelle : « Galaxy Buds3 (6B73) — Écouteurs · Samsung »
  avec son icône.

**Ce qui n'a pas été vérifié** : la disposition en portrait (`MOYENNE`, rail
latéral + écran unique) n'a jamais été exercée sur l'appareil, et le rendu
téléphone (`COMPACTE`) n'a pas été retesté sur un téléphone — il est inchangé
par construction, mais c'est un raisonnement, pas une observation.

Un phénomène à connaître pour ne pas le prendre pour une régression : après
plusieurs réinstallations rapprochées, l'application a affiché « 0 appareil ».
C'est la **limitation de fréquence des scans BLE d'Android** (environ 5
démarrages de scan par fenêtre de 30 s). Les 42 appareils sont revenus seuls
après 35 s d'attente, sans la moindre modification de code.

---

## Points ouverts et limites connues

### Les directions affichées sur la carte sont inventées

C'est la limite la plus trompeuse pour un lecteur de l'interface. Dans
`ui/CommonUi.kt`, la direction d'un appareil est dérivée de son identifiant :

```kotlin
fun estimatedBearing(stableId: String): Double =
    (abs(stableId.hashCode()) % 360).toDouble()
```

Sur la carte, **seule la distance est réelle** (estimée par atténuation du
signal) ; l'azimut est arbitraire mais stable. Les appareils forment donc une
rosace régulière autour de l'utilisateur, et non leurs positions véritables. Un
relèvement réel exigerait soit une calibration manuelle (le champ
`referenceBearings` du `ViewModel` le prévoit déjà), soit plusieurs mesures
depuis des points distincts.

La distance elle-même vient d'un modèle de perte en espace libre,
`10^((txPower - rssi) / (10 × n))`, très sensible aux obstacles : à l'intérieur,
les valeurs sont indicatives.

### Points mineurs

| Point | Détail |
| --- | --- |
| Portrait non testé | La disposition `MOYENNE` (600–839 dp) n'a pas été exercée sur l'appareil |
| API osmdroid dépréciée | `setFillColor`, `setStrokeColor`, `setStrokeWidth` sur `Polygon` : fonctionnels, mais dépréciés en 6.1.18 |
| Avertissement préexistant | Paramètre `scanning` jamais utilisé dans `TopBar` (`ui/CarteScreen.kt`), antérieur à ces modifications |
| Pas de wrapper Gradle | `gradlew` absent : la compilation dépend de chemins absolus hors du projet (voir Environnement) |

---

## Fichiers modifiés

| Fichier | Nature |
| --- | --- |
| `app/src/main/java/com/daveradar/app/ui/WindowSize.kt` | **nouveau** — seuils de largeur Material 3 |
| `app/src/main/java/com/daveradar/app/ui/AppRoot.kt` | réécrit — trois dispositions selon la largeur |
| `app/src/main/java/com/daveradar/app/ui/CarteScreen.kt` | marqueurs, halo de précision, recentrage, état GPS, `clipToBounds` |
| `app/src/main/java/com/daveradar/app/sensors/SensorsManager.kt` | priorité GPS et arbitrage des positions |
| `app/src/main/java/com/daveradar/app/ui/CommonUi.kt` | alignement des barres de signal |
