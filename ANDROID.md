# Actualizar Doracash en Android

La app guarda los datos en la base indicada en `firebase.json`. Cada cuenta Google tiene su propio documento `users/{uid}` y sus subcolecciones `budgets`, `goals`, `pendingTransactions` y `transactions`. Los dos teléfonos deben iniciar sesión con la misma cuenta para compartir los cambios. Una confirmación de pendiente actualiza el saldo y mueve el registro en una sola transacción.

## Preparar Firebase

Desde una sesión con permisos en el proyecto, ejecutá:

```sh
firebase deploy --only firestore:rules --project gen-lang-client-0104471419
```

En Firebase → App Check, verificá el registro de la app Android `com.aistudio.doracash.kmpzqx` con la clave Android de reCAPTCHA Enterprise indicada en `firebase_applet_config.xml`. En la configuración de la aplicación de Firebase, registrá la huella SHA-1 del certificado permanente para el inicio de sesión con Google. El archivo `app/google-services.json` es la configuración cliente de esta aplicación.

## Publicar el APK

Si tu llave está en Cloud Shell, abrí la terminal con la misma cuenta que usaste la primera vez. Este comando firma allí, registra la huella pública en Firebase y publica el APK para todos los teléfonos. La llave privada permanece en tu cuenta. GitHub puede pedirte iniciar sesión para publicar.

```sh
curl -fsSLo doracash.sh https://doracash-new.vercel.app/publicar.sh && bash doracash.sh
```

Para firmar automáticamente las próximas versiones desde GitHub Actions, en GitHub → Settings → Secrets and variables → Actions, configurá una vez los secretos de la firma que ya tiene la app instalada. Si la firmaste con el instalador anterior en Cloud Shell, el almacén está en `~/.doracash-signing/key.p12`, la contraseña en el archivo `password` de esa misma carpeta y el alias es `doracash`. Podés cargar los secretos sin imprimirlos desde una sesión autenticada de GitHub CLI:

```sh
repo=garciashmuel540-lang/Doracash-new
base64 -w0 ~/.doracash-signing/key.p12 | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$repo"
gh secret set ANDROID_STORE_PASSWORD --repo "$repo" < ~/.doracash-signing/password
```

Para otra firma, indicá también `ANDROID_KEY_ALIAS` y, si difiere de la contraseña del almacén, `ANDROID_KEY_PASSWORD`. Conservá la llave original.

Al integrar una versión nueva en `main`, GitHub Actions prueba la app y publica el APK firmado en Releases. Para volver a ejecutar la publicación después de configurar los secretos, usá **Actions → Compilar Doracash → Run workflow** en `main`. Incrementá `versionCode` y `versionName` en cada actualización.

Para pasar de la versión anterior a la 1.2, descargá el APK de la publicación firmada e instalalo sobre Doracash. Desde la 1.2, al abrir la app el teléfono comprueba periódicamente la publicación y ofrece descargar una versión más nueva. Android solicita confirmar la instalación. Las compilaciones de prueba tienen una firma temporal y se distribuyen aparte.
