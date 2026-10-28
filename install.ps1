# Mi 9 (cepheus) - crDroid 16 + ozel GApps kurucusu (Windows PowerShell)
# Telefondaki TUM veriyi siler. Bootloader'in acik olmasi gerekir.
# Calistirmak icin install.bat dosyasina cift tiklayin.
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$Files = Join-Path $PSScriptRoot 'files'
$Tools = Join-Path $PSScriptRoot 'tools'
$Logs  = Join-Path $PSScriptRoot 'logs'
New-Item -ItemType Directory -Force -Path $Files, $Tools, $Logs | Out-Null
$Log = Join-Path $Logs ("install-{0}.log" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))

# manifest.conf: KEY="value" satirlari
$M = @{}
Get-Content -Path (Join-Path $PSScriptRoot 'manifest.conf') | ForEach-Object {
  if ($_ -match '^\s*([A-Z0-9_]+)="(.*)"\s*$') { $M[$Matches[1]] = $Matches[2] }
}

function Say($t)  { Write-Host "==> $t" -ForegroundColor Cyan;   Add-Content $Log "==> $t" }
function Warn($t) { Write-Host "!!  $t" -ForegroundColor Yellow; Add-Content $Log "!!  $t" }
function Die($t)  { Write-Host "HATA: $t" -ForegroundColor Red; Add-Content $Log "HATA: $t"; Write-Host "Kayit dosyasi: $Log"; exit 1 }

# --- 1. platform-tools ---
$PT = Join-Path $Tools 'platform-tools'
if (-not (Test-Path (Join-Path $PT 'adb.exe'))) {
  Say 'Android platform-tools indiriliyor (adb, fastboot)'
  $zip = Join-Path $Tools 'pt.zip'
  Invoke-WebRequest -Uri $M['PT_URL_WINDOWS'] -OutFile $zip
  Expand-Archive -Path $zip -DestinationPath $Tools -Force
  Remove-Item $zip
}
$env:PATH = "$PT;$env:PATH"

# --- 2. Dosyalar ---
function Fetch($name, $url, $md5, $hint) {
  $path = Join-Path $Files $name
  if (-not (Test-Path $path)) {
    if (-not $url) { Die "'$name' files\ klasorunde yok ve otomatik indirilemiyor. $hint" }
    Say "Indiriliyor: $name"
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $url -OutFile $path
  }
  if ($md5) {
    Say "Dogrulaniyor: $name"
    $h = (Get-FileHash -Algorithm MD5 -Path $path).Hash.ToLower()
    if ($h -ne $md5) { Die "'$name' MD5 degeri uyusmuyor (bozuk ya da farkli surum). Silip tekrar indirin." }
  }
}
Fetch $M['ROM_NAME']   $M['ROM_URL']   $M['ROM_MD5']   ("crDroid zip'i: " + $M['ROM_URL'])
Fetch $M['FW_NAME']    $M['FW_URL']    $M['FW_MD5']    "Firmware'i https://xmfirmwareupdater.com/firmware/cepheus/stable/V12.5.6.0.RFACNXM/ adresinden indirip files\ icine koyun."
Fetch $M['REC_NAME']   $M['REC_URL']   $M['REC_MD5']   ("Recovery'yi " + $M['REC_SOURCE'] + " adresinden indirip files\ icine koyun.")
# Ozel GApps: resmi NikGapps'ten bu bilgisayarda uretilir (Google dosyalari dagitilmaz)
if (-not (Test-Path (Join-Path $Files $M['GAPPS_NAME']))) {
  Fetch $M['NIKGAPPS_NAME'] $M['NIKGAPPS_URL'] $M['NIKGAPPS_MD5'] ("NikGapps zip'i: " + $M['NIKGAPPS_URL'])
  Say 'Ozel GApps paketi uretiliyor'
  & (Join-Path $PSScriptRoot 'gapps\build-gapps.ps1') -Source (Join-Path $Files $M['NIKGAPPS_NAME']) -Output (Join-Path $Files $M['GAPPS_NAME'])
}

# --- 3. Cihaz ---
function FbDevices { (& fastboot devices 2>$null) | Where-Object { $_ -match '\S' } }
function Getvar($k) {
  $out = (& fastboot getvar $k 2>&1 | Out-String)
  if ($out -match "(?m)^$k\s*:\s*(\S+)") { return $Matches[1] }
  return ''
}
Say 'Telefon araniyor (USB hata ayiklama acik ya da fastboot modunda olmali)'
& adb start-server 2>$null | Out-Null
$nAdb = @((& adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" }).Count
$nFb  = @(FbDevices).Count
if (($nAdb + $nFb) -gt 1) { Die "Birden fazla cihaz bagli. Sadece Mi 9'u birakin." }
if ($nAdb -eq 1) {
  $lvl = [int](((& adb shell dumpsys battery) | Select-String 'level:' | Select-Object -First 1).ToString().Split(':')[1].Trim())
  if ($lvl -lt 50) { Die "Sarj %$lvl. Kurulumdan once telefonu en az %50'ye sarj edin." }
  Say "Telefon fastboot'a aliniyor"; & adb reboot bootloader
}
for ($i = 0; $i -lt 30 -and -not (FbDevices); $i++) { Start-Sleep 2 }
if (-not (FbDevices)) { Die "Telefon fastboot'ta bulunamadi. Kapatin, ses kisma + guc tusuna basili tutup acin, sonra tekrar deneyin." }

if ((Getvar 'product') -ne $M['DEVICE_CODENAME']) { Die "Bu cihaz Mi 9 ($($M['DEVICE_CODENAME'])) degil. Kurulum durduruldu." }
if ((Getvar 'unlocked') -ne 'yes') { Die 'Bootloader kilitli. Once kilidi acin.' }

Warn 'TELEFONDAKI TUM VERI SILINECEK (fotograflar, uygulamalar, hesaplar).'
$ans = Read-Host 'Devam etmek icin EVET yazin'
if ($ans -ne 'EVET') { Die 'Iptal edildi.' }

# --- yardimcilar ---
function WaitRecovery { & adb wait-for-recovery; Start-Sleep 8 }
function CheckLog($step) {
  $out = Join-Path $Logs "recovery-$step.log"
  (& adb shell cat /tmp/recovery.log 2>$null) | Set-Content $out
  if (Select-String -Path $out -Pattern 'script aborted|Installation failed|Error installing zip|E:Error in' -Quiet) {
    Die "'$step' adimi hata verdi. Ayrintilar: $out"
  }
}
function Sideload($step, $name) {
  Say "Kuruluyor: $step"
  & adb shell twrp sideload 2>$null | Out-Null
  Start-Sleep 5
  & adb sideload (Join-Path $Files $name)
  if ($LASTEXITCODE -ne 0) { Die "'$step' sideload basarisiz." }
  WaitRecovery
  CheckLog $step
}

# --- 4. Format (en basta: NikGapps 'dirty flash' sanip bazi paketleri atlamasin) ---
Say 'Veri bolumu formatlaniyor'
& fastboot format:f2fs userdata
& fastboot format:ext4 metadata
& fastboot format:ext4 cache

# --- 5. Recovery ---
Say 'Recovery yukleniyor'
& fastboot flash recovery (Join-Path $Files $M['REC_NAME'])
& fastboot boot (Join-Path $Files $M['REC_NAME'])
WaitRecovery

# --- 5. Eski GApps yedegini sil: ROM kurulumu once yedekleyip sonra geri yukluyor, biz GApps'i sifirdan kuruyoruz ---
Say 'Eski sistemden kalan yedek betikleri temizleniyor'
& adb shell 'twrp mount system >/dev/null 2>&1; rm -rf /system_root/system/addon.d/* /system/addon.d/* 2>/dev/null; true' 2>&1 | Out-Null

# --- 5. Firmware, ROM ---
Sideload 'firmware' $M['FW_NAME']
Sideload 'rom'      $M['ROM_NAME']

# --- 6. GApps ---
Say 'Recovery yeniden baslatiliyor'
& adb reboot recovery
WaitRecovery
Sideload 'gapps' $M['GAPPS_NAME']

# --- 7. Acilis ---
Say 'Sistem baslatiliyor'
& adb reboot

Say 'Bitti. Ilk acilis 5-10 dakika surebilir; logo uzun sure donerse beklemeye devam edin.'
Write-Host "Kayit dosyasi: $Log"
