$ErrorActionPreference = 'Stop'
$repo = 'garciashmuel540-lang/Doracash-new'
$folder = Join-Path $HOME 'Downloads\Doracash'
New-Item -ItemType Directory -Force -Path $folder | Out-Null
try {
  $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/latest" -Headers @{ 'User-Agent' = 'Doracash-Installer' }
} catch {
  Write-Host 'No hay un APK publicado en GitHub Releases todavía.' -ForegroundColor Yellow
  Write-Host "Revisa: https://github.com/$repo/actions"
  exit 1
}
if ($release.draft -or $release.prerelease -or $release.tag_name -notmatch '^android-v[0-9]+(\.[0-9]+){1,3}$') {
  Write-Host 'La nueva version de Doracash todavia espera su firma de distribucion.' -ForegroundColor Yellow
  exit 1
}
$apks = @($release.assets | Where-Object { $_.name -cmatch '^Doracash-[1-9][0-9]*\.apk$' })
if ($apks.Count -ne 1) { throw 'La publicacion debe contener un APK firmado.' }
$asset = $apks[0]
$dest = Join-Path $folder $asset.name
$download = [System.IO.Path]::GetTempFileName()
try {
  $baseUrl = "https://github.com/$repo/releases/download/$($release.tag_name)"
  $manifest = (Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/SHA256SUMS.txt").Content
  if ($manifest -is [byte[]]) { $manifest = [Text.Encoding]::UTF8.GetString($manifest) }
  $pattern = '(?im)^([0-9a-f]{64}) [ *]' + [regex]::Escape($asset.name) + '\r?$'
  if ($manifest -notmatch $pattern) { throw 'Falta la suma de verificacion del APK.' }
  $expectedHash = $Matches[1]
  Write-Host "Descargando Doracash $($release.tag_name)..."
  Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/$($asset.name)" -OutFile $download
  if ((Get-FileHash $download -Algorithm SHA256).Hash -ne $expectedHash) { throw 'No coincide la firma de integridad de la descarga.' }
  Move-Item -Force $download $dest
} finally {
  if (Test-Path $download) { Remove-Item -Force $download }
}
Write-Host "APK descargado: $dest" -ForegroundColor Green
if (Get-Command adb -ErrorAction SilentlyContinue) {
  $devices = @(adb devices | Select-String '\tdevice$')
  if ($devices.Count -gt 0) {
    $answer = Read-Host '¿Instalar en el Android conectado por USB? (s/N)'
    if ($answer -match '^[sSyY]$') { & adb install -r $dest }
  }
}
