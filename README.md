# Hopescrolling

Bloque **YouTube Shorts**, **Instagram Reels** et **TikTok**, dans les applis comme dans le
navigateur (Brave, Chrome, Firefox, DuckDuckGo), et limite le scroll du fil Instagram.
Le reste des applis marche normalement.
C'est une app Android minimaliste, sans pub et sans réseau : elle n'a même pas la permission INTERNET.

## Comment ça marche

Les applis du type « bloqueur de Shorts » utilisent toutes la même technique : un
**service d'accessibilité** Android.

1. Android prévient le service à chaque changement d'écran dans YouTube ou Instagram
   (la liste des applis surveillées est fixée dans `res/xml/accessibility_service.xml`).
2. Le service parcourt l'arbre de vues affiché et cherche un `resource-id` qui n'existe
   que sur le lecteur Shorts/Reels (par ex. `com.google.android.youtube:id/reel_recycler`
   ou `com.instagram.android:id/clips_viewer_view_pager`).
3. S'il le trouve, il simule le bouton **Retour**. TikTok étant fait entièrement de scroll
   infini, il renvoie simplement à l'écran d'accueil.

Deux cas particuliers :
- **Navigateurs** : le service lit l'URL dans la barre d'adresse et fait Retour sur
  `youtube.com/shorts`, `instagram.com/reel(s)` et `tiktok.com`.
- **Fil Instagram et Explorer** : 10 swipes sont autorisés, puis retour à l'écran d'accueil.
  Le compteur repart à zéro après 15 minutes sans scroller le fil. Ces valeurs se règlent
  dans `Targets.kt` (`FEED_SWIPE_BUDGET`, `FEED_RESET_MS`).

Tout se trouve dans `app/src/main/java/app/hopescrolling/` :
- `Targets.kt` : les applis visées et les IDs de vue à détecter
- `BlockerService.kt` : la détection et l'action Retour
- `MainActivity.kt` : l'écran d'état, les interrupteurs et le compteur

## Installer sur un Pixel

1. Ouvre l'onglet **Actions** du dépôt GitHub, puis le dernier run *Build APK* réussi.
   Télécharge l'artefact `hopescrolling-apk` (un zip qui contient `app-debug.apk`).
2. Ouvre le zip sur le téléphone et installe l'APK. Autorise l'installation depuis le
   navigateur ou l'appli Fichiers si Android le demande.
3. Ouvre Hopescrolling, puis **Activer dans Accessibilité** → Hopescrolling → activer.
   - Si l'option est grisée (Android 13 et plus, pour une appli installée hors du Play Store) :
     **Paramètres → Applis → Hopescrolling → ⋮ (en haut à droite) → Autoriser les paramètres
     restreints**, puis recommence.

Les mises à jour s'installent par-dessus l'ancienne version, parce que l'APK est toujours
signé avec la même clé (`app/debug.keystore`).

Pour compiler en local avec le SDK Android : `./gradlew assembleDebug`.

## Quand une mise à jour de YouTube ou d'Instagram casse la détection

Les IDs de vue peuvent changer d'une version à l'autre.

**Sans ordinateur** : active le **Mode diagnostic** dans l'appli, ouvre l'écran qui n'est pas
bloqué pendant quelques secondes, reviens dans Hopescrolling et appuie sur **Copier le
diagnostic**. Le diagnostic liste les IDs des vues visibles, sans le texte de tes messages.

**Avec un ordinateur (adb)** :

1. Active le **débogage USB** et branche le téléphone (`adb devices` doit l'afficher).
2. Ouvre un Short ou un Reel, puis lance `./tools/dump_view_ids.sh`.
3. Relance le script sur un écran normal et compare : ajoute dans `Targets.kt` les IDs qui
   n'apparaissent que sur le lecteur, puis pousse. La CI recompile l'APK.

## Limites

- Les Reels de Facebook et Snapchat Spotlight ne sont pas encore gérés : leurs vues n'ont
  pas d'IDs stables.
- On peut toujours désactiver le service dans les réglages. C'est une barrière de friction,
  pas une prison.
