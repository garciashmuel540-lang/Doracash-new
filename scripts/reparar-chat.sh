#!/usr/bin/env bash
set -euo pipefail
umask 077

python3 - <<'PY'
import json
import subprocess
import sys
from urllib.error import HTTPError
from urllib.request import Request, urlopen

project = "gen-lang-client-0104471419"
number = "651024753495"
app = "1:651024753495:android:f3a3e5492f50358b629412"
package = "com.aistudio.doracash.kmpzqx"
site = "6Lei3iguAAAAANs5_pgnQ4XHyrUsQbYiqBAzmxia"
token = subprocess.check_output(["gcloud", "auth", "print-access-token"], text=True).strip()

def call(url, body=None, missing_ok=False):
    req = Request(url, method="GET" if body is None else "PATCH",
                  data=None if body is None else json.dumps(body).encode(),
                  headers={"Authorization": "Bearer " + token, "Content-Type": "application/json",
                           "x-goog-user-project": project})
    try:
        with urlopen(req, timeout=40) as response:
            return json.load(response)
    except HTTPError as error:
        if missing_ok and error.code == 404:
            return {}
        detail = error.read().decode(errors="replace")
        sys.exit(f"Google respondió HTTP {error.code} en {req.host}: {detail}")

key_url = f"https://recaptchaenterprise.googleapis.com/v1/projects/{project}/keys/{site}"
config_name = f"projects/{number}/apps/{app}/recaptchaEnterpriseConfig"
config_url = f"https://firebaseappcheck.googleapis.com/v1/{config_name}"

key = call(key_url)
settings = key.get("androidSettings")
if settings is None or settings.get("allowAllPackageNames"):
    sys.exit("Se esperaba la clave de Android con restricción de paquetes de Doracash. Revisá la clave del proyecto.")
packages = settings.get("allowedPackageNames", [])
changes = {}
if package not in packages:
    changes["allowedPackageNames"] = packages + [package]
if not settings.get("supportNonGoogleAppStoreDistribution"):
    changes["supportNonGoogleAppStoreDistribution"] = True
if changes:
    mask = ",".join("androidSettings." + field for field in changes)
    call(key_url + "?updateMask=" + mask, {"name": key["name"], "androidSettings": {**settings, **changes}})

if call(config_url, missing_ok=True).get("siteKey") != site:
    call(config_url + "?updateMask=siteKey", {"name": config_name, "siteKey": site})
updated = call(key_url)["androidSettings"]
if (package not in updated.get("allowedPackageNames", []) or updated.get("allowAllPackageNames")
        or not updated.get("supportNonGoogleAppStoreDistribution") or call(config_url).get("siteKey") != site):
    sys.exit("Google no confirmó la configuración de Doracash.")
print("Verificado en Google: App Check usa la clave de Doracash y admite el APK de GitHub.")
PY

publisher=$(mktemp)
trap 'rm -f "$publisher"' EXIT
curl --proto '=https' -fsSL --retry 2 https://doracash-new.vercel.app/publicar.sh -o "$publisher"
bash "$publisher"
