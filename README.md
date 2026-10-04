# Dave Radar

Radar Bluetooth pour Android : détecte les appareils BLE autour de vous, estime
leur distance et les place sur une carte OpenStreetMap.

## Fonctionnalités

- **Balayage BLE continu** avec identification du constructeur et du type
  (téléphone, écouteurs, montre…)
- **Carte OpenStreetMap** via osmdroid — aucune clé d'API requise
- **Estimation de distance** par atténuation du signal (RSSI)
- **Écran « Localiser »** avec boussole pour s'approcher d'un appareil
- **Renommage et mise en liste noire** des appareils connus
- **Mise en page adaptative** téléphone / tablette

## Mise en page adaptative

L'interface se réorganise selon la largeur disponible, d'après les seuils
Material 3 :

| Largeur | Disposition |
| --- | --- |
| < 600 dp | Barre de navigation en bas, un écran à la fois |
| 600–839 dp | Rail de navigation latéral |
| ≥ 840 dp | Liste et carte côte à côte |

## Compilation

Prérequis : JDK 17, SDK Android 34.

```bash
./gradlew assembleDebug
```

Le projet n'embarque pas de wrapper Gradle ; avec une installation Gradle
externe (8.7 ou plus récent) :

```bash
export JAVA_HOME=/chemin/vers/jdk-17
gradle assembleDebug
```

Créez un fichier `local.properties` à la racine, pointant vers votre SDK :

```properties
sdk.dir=/chemin/vers/android-sdk
```

L'APK sort dans `app/build/outputs/apk/debug/`.

## Permissions

| Permission | Usage |
| --- | --- |
| `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` | Balayage BLE (Android 12+) |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Balayage BLE (Android 8 à 11) |
| `ACCESS_FINE_LOCATION` | Exigée par Android pour le scan BLE, et utilisée
  pour la carte et la boussole |
| `INTERNET` | Téléchargement des tuiles de carte |

Android 8.0 (API 26) minimum.

## Limites connues

**Les directions affichées sur la carte ne sont pas mesurées.** Seule la
distance est estimée, à partir du signal. L'azimut de chaque appareil est
dérivé de son identifiant (`estimatedBearing`) : il est stable d'une session à
l'autre, mais arbitraire. Les appareils forment donc une rosace autour de vous,
et non leurs positions réelles.

La distance elle-même repose sur un modèle d'atténuation en espace libre, très
sensible aux obstacles : en intérieur, les valeurs sont indicatives.

## Architecture

```
app/src/main/java/com/daveradar/app/
├── ble/        Balayage et identification des appareils
├── data/       Persistance des noms personnalisés
├── model/      Modèle Appareil, calcul de distance
├── sensors/    Boussole et position (GPS prioritaire sur le réseau)
└── ui/         Écrans Compose, thème, mise en page adaptative
```

Les détails techniques, les causes racines des correctifs récents et les points
ouverts sont consignés dans [NOTES-DEV.md](NOTES-DEV.md).

## Licence

Aucune licence n'est déclarée à ce jour : tous droits réservés.
