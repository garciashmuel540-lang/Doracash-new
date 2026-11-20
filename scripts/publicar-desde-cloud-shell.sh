#!/usr/bin/env bash
set -euo pipefail
umask 077

repo='garciashmuel540-lang/Doracash-new'
api="https://api.github.com/repos/$repo"
raw="https://raw.githubusercontent.com/$repo"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cd "$work"
fetch() { curl --proto '=https' -fsSL --retry 2 "$1" -o "$2"; }
fetch "$raw/main/app/build.gradle.kts" build.gradle.kts
version=$(sed -nE 's/^ *versionName = "([0-9]+(\.[0-9]+){1,3})".*/\1/p' build.gradle.kts)
code=$(sed -nE 's/^ *versionCode = ([1-9][0-9]*).*/\1/p' build.gradle.kts)
[[ -n $version && -n $code ]]
tag="android-v$version"
apk="Doracash-$code.apk"
if curl -fsS "$api/releases/tags/$tag" -o release.json 2>/dev/null &&
   jq -e --arg apk "$apk" '(.draft or .prerelease) == false and any(.assets[]; .name == $apk and .size > 0)' release.json >/dev/null; then
  echo "La versión $version ya está publicada: https://doracash-new.vercel.app"
  exit 0
fi

identity="${DORACASH_SIGNING_DIR:-$HOME/.doracash-signing}"
if [[ ! -s "$identity/key.p12" || ! -s "$identity/password" ]]; then
  echo 'Abrí la misma cuenta de Cloud Shell que usaste para firmar Doracash. Hace falta la carpeta original .doracash-signing con key.p12 y password.' >&2
  exit 1
fi
base="firma-local-v$version"
fetch "$api/releases/tags/$base" base.json
commit=$(jq -er '.target_commitish | select(test("^[a-f0-9]{40}$"))' base.json)
archive="Doracash-para-firmar-$code.zip"
digest=$(jq -er --arg name "$archive" '.assets[] | select(.name == $name) | .digest | capture("^sha256:(?<sha>[a-f0-9]{64})$").sha' base.json)
fetch "https://github.com/$repo/releases/download/$base/$archive" base.zip
echo "$digest  base.zip" | sha256sum --check -
unzip -p base.zip app-release-unsigned.apk > unsigned.apk

fetch 'https://dl.google.com/android/repository/build-tools_r35_linux.zip' tools.zip
echo 'bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88  tools.zip' | sha256sum --check -
unzip -p tools.zip '*/lib/apksigner.jar' > apksigner.jar
java -jar apksigner.jar sign --ks "$identity/key.p12" --ks-key-alias doracash --ks-pass "file:$identity/password" --out "$apk" unsigned.apk
java -jar apksigner.jar verify --print-certs "$apk" > verified.txt
cert=$(awk '/certificate SHA-256 digest:/ {print tolower($NF); exit}' verified.txt)
expected=$(keytool -exportcert -keystore "$identity/key.p12" -storepass:file "$identity/password" -alias doracash | sha256sum | cut -d' ' -f1)
[[ $cert == "$expected" ]]
mkdir -p "$HOME/Doracash"
install -m 600 "$apk" "$HOME/Doracash/$apk"
echo "APK firmado y verificado en $HOME/Doracash/$apk"

# Confirmar que Google reconoce el certificado que acaba de firmar este APK.
project=gen-lang-client-0104471419
android_id='1:651024753495:android:f3a3e5492f50358b629412'
sha_api="https://firebase.googleapis.com/v1beta1/projects/$project/androidApps/$android_id/sha"
sha1=$(awk '/certificate SHA-1 digest:/ {print toupper($NF); exit}' verified.txt)
[[ $sha1 =~ ^[A-F0-9]{40}$ ]]
gcloud auth print-access-token > google-token
firebase_sha() {
  { printf 'header = "Authorization: Bearer '; tr -d '\r\n' < google-token; printf '"\n'; } |
    curl --config - --fail --silent --show-error -H "x-goog-user-project: $project" "$@" "$sha_api"
}
firebase_sha -o fingerprints.json
if ! jq -e --arg fp "$sha1" 'any(.certificates[]?; (.shaHash | ascii_upcase | gsub(":";"")) == $fp)' fingerprints.json >/dev/null; then
  jq -n --arg sha "$sha1" '{shaHash:$sha,certType:"SHA_1"}' > fingerprint.json
  firebase_sha -X POST -H 'Content-Type: application/json' --data-binary @fingerprint.json -o registered.json
  jq -e --arg fp "$sha1" '(.shaHash | ascii_upcase | gsub(":";"")) == $fp' registered.json >/dev/null
fi
rm -f google-token

# En una sesión de Cloud Shell sin gh, usar el binario oficial solo en el directorio temporal.
if ! command -v gh >/dev/null; then
  case "$(uname -m)" in x86_64) arch=amd64 ;; aarch64) arch=arm64 ;; *) exit 1 ;; esac
  fetch 'https://api.github.com/repos/cli/cli/releases/latest' gh.json
  ghname=$(jq -er --arg arch "$arch" '.assets[].name | select(endswith("_linux_"+$arch+".tar.gz"))' gh.json)
  ghtag=$(jq -er '.tag_name | select(test("^v[0-9]+(\\.[0-9]+){2}$"))' gh.json)
  ghdigest=$(jq -er --arg name "$ghname" '.assets[] | select(.name==$name) | .digest | capture("^sha256:(?<sha>[a-f0-9]{64})$").sha' gh.json)
  fetch "https://github.com/cli/cli/releases/download/$ghtag/$ghname" gh.tgz
  echo "$ghdigest  gh.tgz" | sha256sum --check -
  tar -xOzf gh.tgz "${ghname%.tar.gz}/bin/gh" > gh
  chmod 700 gh
  export PATH="$work:$PATH"
fi
if ! gh auth status --hostname github.com >/dev/null 2>&1; then
  gh auth login --hostname github.com --git-protocol https --web
fi
sha256sum "$apk" > SHA256SUMS.txt
gh release create "$tag" "$apk" SHA256SUMS.txt --repo "$repo" --target "$commit" --latest --title "Doracash $version" --notes "Actualización Android firmada de Doracash."
echo 'La actualización ya se puede descargar en https://doracash-new.vercel.app'
if command -v cloudshell >/dev/null; then cloudshell download "$HOME/Doracash/$apk"; fi
