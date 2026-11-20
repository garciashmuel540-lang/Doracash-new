#!/usr/bin/env bash
set -euo pipefail
# Descarga el mismo APK firmado que reciben los teléfonos desde la aplicación.
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cd "$work"
api='https://api.github.com/repos/garciashmuel540-lang/Doracash-new/releases/latest'
curl -fsSL --retry 2 "$api" -o release.json
tag=$(jq -er '.tag_name | select(test("^android-v[0-9]+(\\.[0-9]+){1,3}$"))' release.json)
apk=$(jq -er '[.assets[].name | select(test("^Doracash-[1-9][0-9]*\\.apk$"))] | select(length == 1) | first' release.json)
base="https://github.com/garciashmuel540-lang/Doracash-new/releases/download/$tag"
curl -fsSL --retry 2 "$base/$apk" -o "$apk"
curl -fsSL --retry 2 "$base/SHA256SUMS.txt" -o sums
digest=$(awk -v apk="$apk" '$2 == apk {print $1}' sums)
[[ $digest =~ ^[a-fA-F0-9]{64}$ ]]
printf '%s  %s\n' "$digest" "$apk" | sha256sum --check -
install -m 600 "$apk" "$HOME/$apk"
if command -v cloudshell >/dev/null; then cloudshell download "$HOME/$apk"; else echo "$HOME/$apk"; fi
