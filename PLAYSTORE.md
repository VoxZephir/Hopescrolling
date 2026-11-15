# Publier Hopescrolling sur le Play Store

Le dépôt produit deux versions :

| Version | Pour qui | Premium | Fichier |
|---|---|---|---|
| `full` | GitHub, Obtainium | tout débloqué, sans paiement | `hopescrolling.apk` (releases GitHub) |
| `play` | Play Store | gratuit + Premium (Google Play Billing) | `app-play-release.aab` (artefact de la CI) |

**Gratuit dans la version Play Store** : blocage des Shorts YouTube, des Reels Instagram, de TikTok et des vidéos courtes dans le navigateur, et les statistiques.
**Premium** : applis surveillées (pause, limite de scroll, limite quotidienne), jeux, couvre-feu, mode concentration, budget Shorts, limite du fil et saut des pubs Instagram, verrou et protection des réglages.

## 1. Compte et clé d'envoi (une fois)

1. Crée un compte développeur sur <https://play.google.com/console> (25 $ une seule fois, vérification d'identité).
   - Compte **personnel** créé après novembre 2023 : Google exige un **test fermé avec au moins 12 testeurs pendant 14 jours** avant la mise en production. Prévois-le (amis, Reddit, Discord).
2. Génère la clé d'envoi sur ton ordinateur et **garde-la précieusement** (pas dans le dépôt) :
   ```
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
3. Ajoute-la aux secrets GitHub (Settings → Secrets and variables → Actions) :
   - `UPLOAD_KEYSTORE_B64` : le résultat de `base64 -w0 upload.jks`
   - `UPLOAD_STORE_PASSWORD`, `UPLOAD_KEY_ALIAS` (`upload`), `UPLOAD_KEY_PASSWORD`
4. Au prochain push, la CI produit l'artefact `hopescrolling-play-bundle` (le fichier `.aab` signé). Active **Play App Signing** à la première mise en ligne : Google garde la clé de signature finale, toi seulement la clé d'envoi.

## 2. Politique de confidentialité

La page est dans `docs/privacy.html`. Pour la mettre en ligne :

1. Complète l'adresse de contact (`[ADRESSE DE CONTACT À COMPLÉTER]`).
2. GitHub → Settings → Pages → Source : *Deploy from a branch*, branche par défaut, dossier `/docs`.
3. URL à donner à la Play Console : `https://voxzephir.github.io/Hopescrolling/privacy.html` (déjà liée dans l'appli).

## 3. Produits Premium (Play Console → Monétiser)

Les identifiants doivent être exactement ceux-ci (ils sont dans `app/src/play/.../StoreFactory.kt`) :

| Type | ID produit | Forfaits de base | Prix suggéré |
|---|---|---|---|
| Abonnement | `premium` | `monthly` (1 mois), `yearly` (1 an) | 2,99 € / mois, 19,99 € / an |
| Produit unique | `premium_lifetime` | — | 39,99 € |

Offre conseillée : **essai gratuit de 7 jours** sur le forfait `yearly` (Abonnements → premium → yearly → Ajouter une offre). L'appli détecte l'essai et l'affiche toute seule.

Pour tester sans payer : Paramètres → Test de licence → ajoute ton compte Google.

## 4. Fiche Play Store

**Nom** : Hopescrolling : stop au scroll

**Description courte** (80 caractères max) :
> Bloque Shorts, Reels et TikTok sans bloquer tes applis. Reprends ton temps.

**Description complète** :
> Hopescrolling referme les vidéos courtes à ta place, sans bloquer le reste de tes applis. Tu gardes YouTube pour les vidéos que tu cherches, Instagram pour tes amis et tes messages.
>
> GRATUIT
> • YouTube Shorts refermés automatiquement
> • Reels Instagram refermés
> • TikTok fermé dès l'ouverture
> • Shorts, Reels et TikTok bloqués aussi dans le navigateur
> • Temps récupéré et ses équivalences : livres lus, kilomètres de marche, films
>
> PREMIUM
> • Pause consciente avant d'ouvrir n'importe quelle appli
> • Limite de scroll : quelques swipes dans un fil, puis retour à l'accueil (LinkedIn, Facebook, X, Reddit…)
> • Limites quotidiennes par appli, jeux compris
> • Couvre-feu le soir et mode concentration
> • Publicités et suggestions Instagram sautées automatiquement
> • Verrou anti-triche et protection des réglages
> • Widget et tuile de réglages rapides
>
> RESPECT DE LA VIE PRIVÉE
> Aucune publicité, aucun traceur. Rien ne quitte ton téléphone.
>
> ACCESSIBILITÉ
> Hopescrolling utilise l'API AccessibilityService pour reconnaître les écrans de vidéos courtes et appuyer sur Retour à ta place. Aucune donnée n'est collectée ni partagée.

**Catégorie** : Productivité (ou Santé et remise en forme). **Visuels à fournir** : icône 512×512, bannière 1024×500, au moins 2 captures d'écran téléphone.

## 5. Déclaration d'utilisation de l'accessibilité

Play Console → Contenu de l'appli → Accessibilité (API AccessibilityService).

- **L'appli est-elle un outil d'accessibilité ?** Non (`isAccessibilityTool="false"` dans le manifeste).
- **Fonctionnalité principale qui utilise l'API** :
  > Hopescrolling est une appli de bien-être numérique. Le service d'accessibilité détecte quand l'utilisateur ouvre un écran de vidéos courtes (YouTube Shorts, Instagram Reels, TikTok, ou leurs URL dans un navigateur) ou une publication sponsorisée, et effectue une action globale Retour ou Accueil, ou un geste de défilement, pour l'en sortir. Il applique aussi les règles choisies par l'utilisateur (pause avant d'ouvrir une appli, limites de temps, couvre-feu) et mesure le temps passé dans les applis surveillées.
- **Données lues** : contenu de l'écran (structure de la fenêtre, identifiants de vues, textes d'interface), appli au premier plan, URL de la barre d'adresse des navigateurs.
- **Collecte ou partage** : aucun. Toutes les données sont traitées sur l'appareil.
- **Information bien visible** : l'écran « Utilisation de l'accessibilité » s'affiche avant chaque ouverture des réglages d'accessibilité tant que l'utilisateur n'a pas appuyé sur « J'accepte » (`ui/Disclosure.kt`).
- **Vidéo** (lien YouTube non répertorié, ~1 min) à tourner ainsi :
  1. Ouvrir Hopescrolling, appuyer sur « Activer », montrer l'écran d'information, appuyer sur « J'accepte ».
  2. Activer le service dans les réglages d'accessibilité.
  3. Ouvrir YouTube, toucher un Short : il se referme et la carte « Short refermé » apparaît.
  4. Ouvrir Instagram, aller sur Reels : retour automatique.

## 6. Sécurité des données (Data safety)

- L'appli collecte-t-elle ou partage-t-elle des données utilisateur ? **Non.**
- Les données sont-elles chiffrées en transit ? Sans objet (aucune transmission).
- L'utilisateur peut-il demander la suppression ? Les données sont seulement locales : désinstaller l'appli les supprime.
- Les achats passent par Google Play, qui est responsable de ces données.

## 7. Autres rubriques de « Contenu de l'appli »

- **Classification du contenu** : questionnaire IARC → aucune violence, aucun contenu sensible → PEGI 3.
- **Public cible** : 13 ans et plus (évite les exigences des applis pour enfants).
- **Publicités** : non.
- **Applis gouvernementales, financières, santé** : non.

## 8. Mise en ligne

1. Tests → Test interne : envoie le `.aab`, installe-le depuis le lien de test, vérifie l'achat avec un compte de test de licence.
2. Test fermé (12 testeurs, 14 jours) si le compte est personnel.
3. Production → Créer une version → envoyer le `.aab` → déploiement progressif (20 %, puis 100 %).

Chaque push sur GitHub produit un nouveau `.aab` avec un numéro de version plus grand.
