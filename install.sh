#!/usr/bin/env bash
# Mi 9 (cepheus) - crDroid 16 + ozel GApps kurucusu (macOS / Linux)
# Telefondaki TUM veriyi siler. Bootloader'in acik olmasi gerekir.
set -euo pipefail
cd "$(dirname "$0")"

FILES=./files
TOOLS=./tools
LOGS=./logs
mkdir -p "$FILES" "$TOOLS" "$LOGS"
LOG="$LOGS/install-$(date +%Y%m%d-%H%M%S).log"

# shellcheck disable=SC1091
. ./manifest.conf

say()  { printf '\033[1;36m==> %s\033[0m\n' "$*" | tee -a "$LOG"; }
warn() { printf '\033[1;33m!!  %s\033[0m\n' "$*" | tee -a "$LOG"; }
die()  { printf '\033[1;31mHATA: %s\033[0m\n' "$*" | tee -a "$LOG"; echo "Kayit dosyasi: $LOG"; exit 1; }

md5of() { if command -v md5sum >/dev/null; then md5sum "$1" | cut -d' ' -f1; else md5 -q "$1"; fi; }

need() { command -v "$1" >/dev/null || die "'$1' bulunamadi. Lutfen kurun ve tekrar deneyin."; }

# --- 1. Arac kontrolu ve platform-tools ---
need curl; need unzip
case "$(uname -s)" in
  Darwin) PT_URL=$PT_URL_DARWIN ;;
  Linux)  PT_URL=$PT_URL_LINUX ;;
  *) die "Bu betik macOS ve Linux icindir. Windows icin install.bat kullanin." ;;
esac
if [ ! -x "$TOOLS/platform-tools/adb" ]; then
  say "Android platform-tools indiriliyor (adb, fastboot)"
  curl -L --fail -sS -o "$TOOLS/pt.zip" "$PT_URL"
  unzip -q -o "$TOOLS/pt.zip" -d "$TOOLS" && rm -f "$TOOLS/pt.zip"
fi
export PATH="$PWD/$TOOLS/platform-tools:$PATH"

# --- 2. Dosyalar ---
fetch() { # ad url md5 aciklama
  local name=$1 url=$2 md5=$3 hint=$4 path="$FILES/$1"
  if [ ! -f "$path" ]; then
    [ -n "$url" ] || die "'$name' files/ klasorunde yok ve otomatik indirilemiyor. $hint"
    say "Indiriliyor: $name"
    curl -L --fail -C - -o "$path" "$url"
  fi
  if [ -n "$md5" ]; then
    say "Dogrulaniyor: $name"
    [ "$(md5of "$path")" = "$md5" ] || die "'$name' MD5 degeri uyusmuyor (bozuk ya da farkli surum). Silip tekrar indirin."
  fi
}
fetch "$ROM_NAME"   "$ROM_URL"   "$ROM_MD5"   "crDroid zip'i: $ROM_URL"
fetch "$FW_NAME"    "$FW_URL"    "$FW_MD5"    "Firmware'i https://xmfirmwareupdater.com/firmware/cepheus/stable/V12.5.6.0.RFACNXM/ adresinden indirip files/ icine koyun."
fetch "$REC_NAME"   "$REC_URL"   "$REC_MD5"   "Recovery'yi $REC_SOURCE adresinden indirip files/ icine koyun."
# Ozel GApps: resmi NikGapps'ten bu bilgisayarda uretilir (Google dosyalari dagitilmaz)
if [ ! -f "$FILES/$GAPPS_NAME" ]; then
  need zip; need awk
  fetch "$NIKGAPPS_NAME" "$NIKGAPPS_URL" "$NIKGAPPS_MD5" "NikGapps zip'i: $NIKGAPPS_URL"
  say "Ozel GApps paketi uretiliyor"
  ./gapps/build-gapps.sh "$FILES/$NIKGAPPS_NAME" "$FILES/$GAPPS_NAME" 2>&1 | tee -a "$LOG"
fi

# --- 3. Cihaz ---
say "Telefon araniyor (USB hata ayiklama acik ya da fastboot modunda olmali)"
adb start-server >/dev/null 2>&1 || true
n_adb=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
n_fb=$(fastboot devices | wc -l | tr -d ' ')
[ $((n_adb + n_fb)) -le 1 ] || die "Birden fazla cihaz bagli. Sadece Mi 9'u birakin."
if [ "$n_adb" -eq 1 ]; then
  lvl=$(adb shell dumpsys battery | tr -d '\r' | awk '/level:/{print $2; exit}')
  [ "${lvl:-0}" -ge 50 ] || die "Sarj %${lvl:-?}. Kurulumdan once telefonu en az %50'ye sarj edin."
  say "Telefon fastboot'a aliniyor"
  adb reboot bootloader
fi
for _ in $(seq 1 30); do [ -n "$(fastboot devices)" ] && break; sleep 2; done
[ -n "$(fastboot devices)" ] || die "Telefon fastboot'ta bulunamadi. Kapatin, ses kisma + guc tusuna basili tutup acin, sonra tekrar deneyin."

getvar() { fastboot getvar "$1" 2>&1 | awk -F': *' -v k="$1" '$1==k {print $2; exit}'; }
[ "$(getvar product)" = "$DEVICE_CODENAME" ] || die "Bu cihaz Mi 9 ($DEVICE_CODENAME) degil. Kurulum durduruldu."
[ "$(getvar unlocked)" = "yes" ] || die "Bootloader kilitli. Once kilidi acin."

warn "TELEFONDAKI TUM VERI SILINECEK (fotograflar, uygulamalar, hesaplar)."
printf "Devam etmek icin EVET yazin: "; read -r ans
[ "$ans" = "EVET" ] || die "Iptal edildi."

# --- yardimcilar ---
wait_recovery() { adb wait-for-recovery; sleep 8; }
check_log() { # adim
  local out="$LOGS/recovery-$1.log"
  adb shell cat /tmp/recovery.log > "$out" 2>/dev/null || true
  if grep -qE "script aborted|Installation failed|Error installing zip|E:Error in" "$out"; then
    die "'$1' adimi hata verdi. Ayrintilar: $out"
  fi
}
sideload() { # adim dosya
  say "Kuruluyor: $1"
  adb shell twrp sideload >/dev/null 2>&1 || true
  sleep 5
  adb sideload "$FILES/$2" 2>&1 | tee -a "$LOG" || die "'$1' sideload basarisiz."
  wait_recovery
  check_log "$1"
}

# --- 4. Format (en basta: NikGapps 'dirty flash' sanip bazi paketleri atlamasin) ---
say "Veri bolumu formatlaniyor"
fastboot format:f2fs userdata 2>&1 | tee -a "$LOG"
fastboot format:ext4 metadata 2>&1 | tee -a "$LOG"
fastboot format:ext4 cache    2>&1 | tee -a "$LOG"

# --- 5. Recovery ---
say "Recovery yukleniyor"
fastboot flash recovery "$FILES/$REC_NAME" 2>&1 | tee -a "$LOG"
fastboot boot "$FILES/$REC_NAME" 2>&1 | tee -a "$LOG"
wait_recovery

# --- 5. Eski GApps yedegini sil: ROM kurulumu once yedekleyip sonra geri yukluyor, biz GApps'i sifirdan kuruyoruz ---
say "Eski sistemden kalan yedek betikleri temizleniyor"
adb shell 'twrp mount system >/dev/null 2>&1; rm -rf /system_root/system/addon.d/* /system/addon.d/* 2>/dev/null; true' >/dev/null 2>&1 || true

# --- 5. Firmware, ROM ---
sideload "firmware" "$FW_NAME"
sideload "rom"      "$ROM_NAME"

# --- 6. ROM sonrasi recovery'yi yeniden baslat, GApps ---
say "Recovery yeniden baslatiliyor"
adb reboot recovery
wait_recovery
sideload "gapps" "$GAPPS_NAME"

# --- 7. Acilis ---
say "Sistem baslatiliyor"
adb reboot

say "Bitti. Ilk acilis 5-10 dakika surebilir; logo uzun sure donerse beklemeye devam edin."
echo "Kayit dosyasi: $LOG"
