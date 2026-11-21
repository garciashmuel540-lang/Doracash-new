#!/usr/bin/env bash
set -euo pipefail
umask 077
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
identity="${DORACASH_SIGNING_DIR:-$HOME/.doracash-signing}"
if [[ ! -s "$identity/key.p12" || ! -s "$identity/password" ]]; then
  echo 'Abrí la cuenta de Cloud Shell que contiene tu firma original de Doracash.' >&2
  exit 1
fi
exec 9>"$identity/chat.lock"
flock -n 9 || { echo 'La actualización ya está en curso en otra terminal.' >&2; exit 1; }

configure_google() {
python3 - "$@" <<'PY'
import json
import re
import subprocess
import sys
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

project = "gen-lang-client-0104471419"
number = "651024753495"
app = "1:651024753495:android:f3a3e5492f50358b629412"
package = "com.aistudio.doracash.kmpzqx"
label = "doracash-apk"
keys_url = f"https://recaptchaenterprise.googleapis.com/v1/projects/{project}/keys"
config_name = f"projects/{number}/apps/{app}/recaptchaEnterpriseConfig"
config_url = f"https://firebaseappcheck.googleapis.com/v1/{config_name}"
token = subprocess.check_output(["gcloud", "auth", "print-access-token"], text=True).strip()

def call(url, method="GET", body=None, missing_ok=False):
    req = Request(url, method=method, data=None if body is None else json.dumps(body).encode(),
                  headers={"Authorization": "Bearer " + token, "Content-Type": "application/json",
                           "x-goog-user-project": project})
    try:
        with urlopen(req, timeout=40) as response:
            return json.load(response)
    except HTTPError as error:
        if missing_ok and error.code == 404:
            return {}
        sys.exit(f"Google respondió HTTP {error.code} en {req.host}: {error.read().decode(errors='replace')}")

def compatible(key):
    settings = key.get("androidSettings", {})
    return (package in settings.get("allowedPackageNames", [])
            and settings.get("supportNonGoogleAppStoreDistribution") is True
            and not settings.get("allowAllPackageNames") and not key.get("testingOptions")
            and not key.get("wafSettings"))

def key_id(key):
    value = key["name"].split("/")[-1]
    if not re.fullmatch(r"[A-Za-z0-9_-]+", value):
        sys.exit("Google devolvió un identificador de clave inesperado.")
    return value

def select_key(active):
    if active and compatible(call(f"{keys_url}/{active}", missing_ok=True)):
        return active
    page = ""
    while True:
        query = urlencode({"pageSize": 100, "pageToken": page})
        result = call(f"{keys_url}?{query}")
        for key in result.get("keys", []):
            if key.get("labels", {}).get("app") == label and compatible(key):
                return key_id(key)
        page = result.get("nextPageToken")
        if not page:
            break
    new_key = call(keys_url, "POST", {
        "displayName": "Doracash Android - APK de GitHub",
        "labels": {"app": label},
        "androidSettings": {"allowedPackageNames": [package], "supportNonGoogleAppStoreDistribution": True},
    })
    value = key_id(new_key)
    if not compatible(call(f"{keys_url}/{value}")):
        sys.exit("La clave nueva no tiene la configuración Android requerida.")
    return value

active = call(config_url, missing_ok=True).get("siteKey")
if sys.argv[1] == "prepare":
    print(select_key(active))
elif sys.argv[1] == "activate":
    selected = sys.argv[2]
    if not compatible(call(f"{keys_url}/{selected}")):
        sys.exit("La clave de la actualización no es válida para este paquete.")
    if active != selected:
        call(config_url + "?updateMask=siteKey", "PATCH", {"name": config_name, "siteKey": selected})
    if call(config_url).get("siteKey") != selected:
        sys.exit("Firebase no confirmó la clave de la actualización. Volvé a ejecutar el mismo comando.")
    print("Firebase confirmó la clave de la versión publicada.")
PY
}

echo 'Comprobando la clave para instalaciones de Doracash desde GitHub…'
site=$(configure_google prepare)
curl --proto '=https' -fsSL --retry 2 https://doracash-new.vercel.app/publicar.sh -o "$work/publisher"
DORACASH_CHAT_RECEIPT="$work/receipt" DORACASH_CHAT_SITE_KEY="$site" bash "$work/publisher"
if [[ ! -s "$work/receipt" || $(cat "$work/receipt") != "$site" ]]; then
  echo 'No se pudo confirmar la publicación de un APK con la clave nueva. Firebase sigue con su configuración anterior.' >&2
  exit 1
fi
configure_google activate "$site"
echo 'Instalá la actualización desde https://doracash-new.vercel.app'
