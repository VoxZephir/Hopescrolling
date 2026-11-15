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
- **Pubs et suggestions Instagram** : dans le fil Accueil, un post marqué « Sponsorisé »,
  « Suggestions pour vous » ou avec un bouton « Suivre » est sauté : le service simule un
  swipe pour le sortir de l'écran. La détection se fait sur ces textes (`SUGGESTED_MARKERS`).

Tout se trouve dans `app/src/main/java/app/hopescrolling/` :
- `Targets.kt` : les applis visées et les IDs de vue à détecter
- `BlockerService.kt` : la détection et l'action Retour
- `MainActivity.kt` : l'écran d'état, les interrupteurs et le compteur

## Applis surveillées

N'importe quelle appli installée peut être surveillée (onglet Blocages → Ajouter une appli).
Des suggestions mettent en avant les applis addictives : LinkedIn, Facebook, X, Reddit,
Snapchat, Pinterest, Threads, Twitch, Vinted, Temu, Tinder… Pour chaque appli :
- **Pause** à l'ouverture
- **Limite de scroll** : N swipes sur les grandes listes (réglable : 5, 10, 20 ou 40), puis
  retour à l'écran d'accueil. Le compteur repart à zéro après 15 min.
- **Couvre-feu et concentration** : l'appli est fermée pendant ces périodes

LinkedIn, Instagram et YouTube sont surveillées par défaut.

## Autres fonctions

- **Pause consciente** : avant d'ouvrir Instagram ou YouTube, 8 secondes de respiration et
  la question « Pourquoi tu ouvres… ? ». Ensuite tu choisis de fermer ou de continuer.
  Une fois la pause passée, l'appli reste libre tant que tu l'utilises, plus 3 minutes.
- **Couvre-feu** (par défaut de 23:00 à 07:00) et **mode concentration** (30 min à 4 h) :
  Instagram, YouTube et TikTok sont entièrement bloqués.
- **Budget Shorts/Reels** : 0, 5, 10, 15 ou 30 minutes par jour avant le blocage.
- **Verrou** : pour assouplir une règle, il faut attendre 30 s et recopier
  « Je choisis de perdre mon temps ».
- **Protection des réglages** : empêche d'ouvrir la page d'accessibilité de Hopescrolling,
  ses infos d'appli (forcer l'arrêt) et sa désinstallation.
- **Stats** : temps récupéré (estimation) et ses équivalences (livres lus, km de marche,
  films, séances de sport), **temps d'écran mesuré** par appli surveillée comparé à ta
  moyenne, graphique sur 7 jours, série de jours sans atteindre une limite, et un widget
  pour l'écran d'accueil.
- **Message de blocage** : une carte discrète en haut de l'écran rappelle le temps récupéré
  du jour.

## Installer sur un Pixel

1. Télécharge **hopescrolling.apk** depuis la
   [dernière version](https://github.com/VoxZephir/Hopescrolling/releases/latest).
   Lien direct : https://github.com/VoxZephir/Hopescrolling/releases/latest/download/hopescrolling.apk
2. Installe l'APK. Si Play Protect le bloque (« Appli bloquée pour protéger votre
   appareil »), désactive temporairement l'analyse : Play Store → profil → Play Protect →
   ⚙️ → « Analyser les applications avec Play Protect ».
3. Ouvre Hopescrolling, puis **Activer dans Accessibilité** → Hopescrolling → activer.
   - Si l'option est grisée (Android 13 et plus, pour une appli installée hors du Play Store) :
     **Paramètres → Applis → Hopescrolling → ⋮ (en haut à droite) → Autoriser les paramètres
     restreints**, puis recommence.

Les mises à jour s'installent par-dessus l'ancienne version, parce que l'APK est toujours
signé avec la même clé (`app/debug.keystore`).

**Mises à jour automatiques** : installe [Obtainium](https://github.com/ImranR98/Obtainium),
appuie sur « Ajouter une appli » et colle `https://github.com/VoxZephir/Hopescrolling`.
Chaque push publie une nouvelle version sur GitHub, et Obtainium te la propose.

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

## Play Store

Une version Play Store (`play`) avec un abonnement Premium est prête à publier : voir
[PLAYSTORE.md](PLAYSTORE.md). La version GitHub/Obtainium (`full`) reste entièrement gratuite.
Politique de confidentialité : [docs/privacy.html](docs/privacy.html).
