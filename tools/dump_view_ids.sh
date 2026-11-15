#!/usr/bin/env bash
# Affiche les resource-id visibles sur l'écran actuel du téléphone.
# Usage : ouvre un Short/Reel sur le téléphone (branché en USB, débogage activé), puis :
#   ./tools/dump_view_ids.sh
# Compare avec la sortie sur un écran "normal" : les ids présents uniquement
# sur le lecteur Shorts/Reels sont ceux à mettre dans Targets.kt.
set -euo pipefail
adb shell uiautomator dump /sdcard/hs_dump.xml >/dev/null
adb shell cat /sdcard/hs_dump.xml | grep -o 'resource-id="[^"]*"' | sort | uniq -c | sort -rn
