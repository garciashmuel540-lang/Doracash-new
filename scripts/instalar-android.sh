#!/usr/bin/env bash
set -euo pipefail
umask 077

# Personal signing files stay in the signed-in user's Cloud Shell home.
identity="$HOME/.doracash-signing"
mkdir -p "$identity"
chmod 700 "$identity"
exec 9>"$identity/setup.lock"
flock -x 9
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cd "$work"

echo 'Descargando y verificando la actualización de Doracash…'
curl -fsSL --retry 3 https://github.com/garciashmuel540-lang/Doracash-new/releases/download/base-debito-1/Doracash-base.apk -o base.apk
echo 'e317a9f1ecd88529b5c09bc79de21111a4a6c2e54e9df7daea43a7649414de5a  base.apk' | sha256sum -c -
curl -fsSL --retry 3 https://dl.google.com/android/repository/build-tools_r35_linux.zip -o tools.zip
echo '2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc  tools.zip' | sha1sum -c -
unzip -q tools.zip '*/lib/apksigner.jar'
signer=$(find . -path '*/lib/apksigner.jar' -print -quit)
java -jar "$signer" verify base.apk

if [[ ! -e "$identity/key.p12" ]]; then
  # Never replace an existing key. Interrupted setup can reuse the password.
  if [[ ! -s "$identity/password" ]]; then
    openssl rand -hex 32 > "$identity/password"
  fi
  keytool -genkeypair -keystore new.p12 -storetype PKCS12 -storepass:file "$identity/password" -alias doracash -keyalg RSA -keysize 3072 -validity 15000 -dname "CN=Doracash Android"
  mv new.p12 "$identity/key.p12"
fi
java -jar "$signer" sign --ks "$identity/key.p12" --ks-key-alias doracash --ks-pass "file:$identity/password" --out Doracash.apk base.apk
java -jar "$signer" verify --verbose --print-certs Doracash.apk > verified.txt
fingerprint=$(awk '/certificate SHA-1 digest:/ {print toupper($NF); exit}' verified.txt)
[[ "$fingerprint" =~ ^[A-F0-9]{40}$ ]]
printf 'Huella SHA-1 de Doracash: %s\n' "$fingerprint"
echo 'Registrando la firma en el proyecto Firebase de Doracash…'
endpoint='https://firebase.googleapis.com/v1beta1/projects/gen-lang-client-0104471419/androidApps/1:651024753495:android:f3a3e5492f50358b629412/sha'
token=$(gcloud auth print-access-token)
auth=(-H "Authorization: Bearer $token" -H 'x-goog-user-project: gen-lang-client-0104471419')
curl -fsS "${auth[@]}" "$endpoint" -o existing.json
if ! jq -e --arg fp "$fingerprint" 'any(.certificates[]?; (.shaHash|ascii_upcase|gsub(":";"")) == $fp)' existing.json >/dev/null; then
  curl --fail-with-body -sS "${auth[@]}" -H 'Content-Type: application/json' -X POST --data "$(jq -nc --arg sha "$fingerprint" '{shaHash:$sha,certType:"SHA_1"}')" "$endpoint" > registered.json || { cat registered.json >&2; exit 1; }
fi
install -m 600 Doracash.apk "$HOME/Doracash-debito.apk"
echo 'APK verificado y firma registrada. Guardá una copia privada de la carpeta .doracash-signing para futuras actualizaciones.'
cloudshell download "$HOME/Doracash-debito.apk"
