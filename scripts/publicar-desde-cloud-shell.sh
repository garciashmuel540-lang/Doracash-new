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
prepare_github() {
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
}

site=${DORACASH_CHAT_SITE_KEY:-}
key_path=app/src/main/res/values/firebase_applet_config.xml
read_site() {
  python3 -c 'import sys, xml.etree.ElementTree as E; print(next(x.text for x in E.parse(sys.stdin).getroot() if x.get("name") == "recaptcha_site_key"))'
}
if [[ -n $site ]]; then
  [[ $site =~ ^[A-Za-z0-9_-]+$ ]]
  prepare_github
  echo 'Comprobando la clave pública que llevará el APK…'
  git_auth() { git -c credential.helper= -c 'credential.helper=!gh auth git-credential' "$@"; }
  git_auth clone --quiet --depth 1 --branch main "https://github.com/$repo.git" source
  if [[ $(read_site < "source/$key_path") != "$site" ]]; then
    python3 - "$site" "source/$key_path" "source/app/build.gradle.kts" <<'PYTHON'
from pathlib import Path
import re
import sys

site, xml, gradle = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
def replace_once(pattern, fn, text):
    result, count = re.subn(pattern, fn, text, flags=re.M)
    if count != 1:
        sys.exit("No se pudo identificar el recurso o la versión de forma única.")
    return result

xml.write_text(replace_once(r'(<string name="recaptcha_site_key"[^>]*>)[^<]+(</string>)',
                            lambda m: m[1] + site + m[2], xml.read_text()))
text = replace_once(r'^(\s*versionCode = )(\d+)$', lambda m: m[1] + str(int(m[2]) + 1), gradle.read_text())
text = replace_once(r'^(\s*versionName = ")(\d+(?:\.\d+)*\.)(\d+)("$)',
                    lambda m: m[1] + m[2] + str(int(m[3]) + 1) + m[4], text)
gradle.write_text(text)
PYTHON
    [[ $(read_site < "source/$key_path") == "$site" ]]
    git -C source add "$key_path" app/build.gradle.kts
    git -C source -c user.name='Doracash Android' -c user.email='doracash@users.noreply.github.com' commit --quiet -m 'Compilar Doracash con la clave pública para los APK de GitHub'
    git_auth -C source push origin HEAD:main
  fi
  cp source/app/build.gradle.kts build.gradle.kts
else
  fetch "$raw/main/app/build.gradle.kts" build.gradle.kts
fi
version=$(sed -nE 's/^ *versionName = "([0-9]+(\.[0-9]+){1,3})".*/\1/p' build.gradle.kts)
code=$(sed -nE 's/^ *versionCode = ([1-9][0-9]*).*/\1/p' build.gradle.kts)
[[ -n $version && -n $code ]]
tag="android-v$version"
apk="Doracash-$code.apk"
verify_release_site() {
  if [[ -n $site ]]; then
    local revision
    revision=$(jq -er '.target_commitish | select(test("^[a-f0-9]{40}$"))' "$1")
    fetch "$raw/$revision/$key_path" packaged-key.xml
    [[ $(read_site < packaged-key.xml) == "$site" ]] || { echo 'El APK de esta versión corresponde a otra clave.' >&2; return 1; }
  fi
}
confirm_publication() {
  verify_release_site "$1"
  if [[ -n $site ]]; then
    printf '%s\n' "$site" > "${DORACASH_CHAT_RECEIPT:?Falta el comprobante de publicación}"
  fi
}
if curl -fsS "$api/releases/tags/$tag" -o release.json 2>/dev/null &&
   jq -e --arg apk "$apk" '(.draft or .prerelease) == false and any(.assets[]; .name == $apk and .size > 0)' release.json >/dev/null; then
  confirm_publication release.json
  echo "La versión $version ya está publicada: https://doracash-new.vercel.app"
  exit 0
fi

identity="${DORACASH_SIGNING_DIR:-$HOME/.doracash-signing}"
if [[ ! -s "$identity/key.p12" || ! -s "$identity/password" ]]; then
  echo 'Abrí la misma cuenta de Cloud Shell que usaste para firmar Doracash. Hace falta la carpeta original .doracash-signing con key.p12 y password.' >&2
  exit 1
fi
base="firma-local-v$version"
if [[ -n $site ]]; then
  echo "Esperando la compilación de Doracash $version en GitHub. Dejá abierta esta terminal."
  for ((attempt=0; attempt<120; attempt++)); do
    if gh api "repos/$repo/releases/tags/$base" > base.json 2>/dev/null; then break; fi
    if gh api "repos/$repo/releases/tags/$tag" > stable.json 2>/dev/null &&
       jq -e --arg apk "$apk" '(.draft or .prerelease) == false and any(.assets[]; .name==$apk and .size>0)' stable.json >/dev/null; then
      confirm_publication stable.json
      exit 0
    fi
    sleep 15
  done
  [[ -s base.json ]] || { echo 'GitHub todavía no publicó esta compilación. Revisá Actions y volvé a ejecutar el mismo comando.' >&2; exit 1; }
  verify_release_site base.json
else
  fetch "$api/releases/tags/$base" base.json
fi
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

prepare_github
sha256sum "$apk" > SHA256SUMS.txt
gh release create "$tag" "$apk" SHA256SUMS.txt --repo "$repo" --target "$commit" --latest --title "Doracash $version" --notes "Actualización Android firmada de Doracash."
if [[ -n $site ]]; then
  gh api "repos/$repo/releases/tags/$tag" > stable.json
  jq -e --arg apk "$apk" '(.draft or .prerelease) == false and any(.assets[]; .name==$apk and .size>0)' stable.json >/dev/null
  confirm_publication stable.json
fi
echo 'La actualización ya se puede descargar en https://doracash-new.vercel.app'
if [[ -z $site ]] && command -v cloudshell >/dev/null; then
  cloudshell download "$HOME/Doracash/$apk" || echo 'Podés descargar el APK desde el enlace anterior.'
fi
