#!/usr/bin/env bash
set -euo pipefail
mkdir -p dist
cp public/index.html dist/
cp app/src/main/res/drawable/img_doraemon_mascot.png dist/doraemon.png
cp app/src/main/res/font/fredoka.ttf dist/fredoka.ttf
cp scripts/publicar-desde-cloud-shell.sh dist/publicar.sh
cp scripts/reparar-chat.sh dist/chat.sh
