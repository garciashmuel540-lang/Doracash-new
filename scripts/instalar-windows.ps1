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
$asset = $release.assets | Where-Object { $_.name -match '\.apk$' } | Select-Object -First 1
if (-not $asset) { Write-Error 'La ultima version no contiene un archivo APK.'; exit 1 }
$dest = Join-Path $folder $asset.name
Write-Host "Descargando Doracash $($release.tag_name)..."
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $dest
if ((Get-Item $dest).Length -lt 1024) { throw 'La descarga no parece ser un APK valido.' }
Write-Host "APK descargado: $dest" -ForegroundColor Green
if (Get-Command adb -ErrorAction SilentlyContinue) {
  $devices = @(adb devices | Select-String '\tdevice$')
  if ($devices.Count -gt 0) {
    $answer = Read-Host '¿Instalar en el Android conectado por USB? (s/N)'
    if ($answer -match '^[sSyY]$') { & adb install -r $dest }
  }
}
