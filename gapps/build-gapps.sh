#!/usr/bin/env bash
# Ozel GApps zip'i uretir: tam NikGapps paketinden sadece packages.txt'teki ogeleri tutar,
# debloater.config'i ekler ve config'i zip icinden okunacak sekilde ayarlar.
# Kullanim: ./build-gapps.sh <NikGapps-full.zip> [cikti.zip]
set -euo pipefail
CALLER=$PWD
cd "$(dirname "$0")"
SRC=${1:?"Kullanim: $0 <NikGapps-full.zip> [cikti.zip]"}
OUT=${2:-$PWD/../files/Mi9-GApps-crdroid16.zip}
case "$OUT" in /*) ;; *) OUT="$CALLER/$OUT" ;; esac
case "$SRC" in /*) ;; *) SRC="$CALLER/$SRC" ;; esac

W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
echo "==> Aciliyor: $(basename "$SRC")"
unzip -q "$SRC" -d "$W"

section() { awk -v s="[$1]" '/^\[/{on=($0==s); next} on && !/^[[:space:]]*(#|$)/' packages.txt; }
section config > "$W/.enable"
section files  > "$W/.keep"

echo "==> nikgapps.config guncelleniyor"
awk -v ef="$W/.enable" '
  BEGIN { while ((getline k < ef) > 0) en[k]=1
          split("WipeDalvikCache WipeRuntimePermissions ExecuteBackupRestore OverwriteWithZipConfig GmsOptimization GenerateLogs", st, " ")
          for (i in st) setting[st[i]]=1 }
  /^(>>)?[A-Za-z0-9]+=[01][[:space:]]*$/ {
    split($0, kv, "="); key=kv[1]; plain=key; sub(/^>>/, "", plain)
    if (key == "UseZipConfig") { print "UseZipConfig=1"; next }
    if (plain in setting)      { print; next }
    print key "=" ((key in en) ? 1 : 0); next }
  { print }' "$W/afzc/nikgapps.config" > "$W/afzc/nikgapps.config.new"
mv "$W/afzc/nikgapps.config.new" "$W/afzc/nikgapps.config"
# UseZipConfig satiri 0/1 degilse (olasi bicim farki) ekle
grep -q '^UseZipConfig=1' "$W/afzc/nikgapps.config" || echo "UseZipConfig=1" >> "$W/afzc/nikgapps.config"

echo "==> debloater.config ekleniyor"
cp debloater.config "$W/afzc/debloater.config"

echo "==> Kullanilmayan paketler siliniyor"
removed=0
while IFS= read -r f; do
  rel=${f#"$W"/}
  grep -qxF "$rel" "$W/.keep" || { rm -f "$f"; removed=$((removed+1)); }
done < <(find "$W/AppSet" -name '*.tar.xz')
find "$W/AppSet" -type d -empty -delete
echo "    silinen paket: $removed"
# tutulmasi istenen ama zip'te olmayan paket var mi?
while IFS= read -r rel; do [ -f "$W/$rel" ] || { echo "HATA: zip'te yok: $rel" >&2; exit 1; }; done < "$W/.keep"
rm -f "$W/.enable" "$W/.keep"

cp gpio-keys.kl "$W/gpio-keys.kl"
cp ../extrakey/ExtraKey.apk "$W/ExtraKey.apk"
cp privapp-permissions-extrakey.xml "$W/privapp-permissions-extrakey.xml"
cp sysconfig-extrakey.xml "$W/sysconfig-extrakey.xml"
cp overlay-config.xml "$W/overlay-config.xml"
awk '/^    \. "\$COMMONDIR\/install\.sh"/ {
  print "    unpack \"overlay-config.xml\" \"$TMPDIR/overlay-config.xml\""
  print "    oc_dir=\"${product:-/product}/overlay/config\""
  print "    mkdir -p \"$oc_dir\" && cp -f \"$TMPDIR/overlay-config.xml\" \"$oc_dir/config.xml\" && chmod 644 \"$oc_dir/config.xml\" && ui_print \"- Varsayilan hareketle gezinme ayarlandi\""
  print "    unpack \"gpio-keys.kl\" \"$TMPDIR/gpio-keys.kl\""
  print "    kl_dir=\"${product:-/product}/usr/keylayout\""
  print "    mkdir -p \"$kl_dir\" && cp -f \"$TMPDIR/gpio-keys.kl\" \"$kl_dir/gpio-keys.kl\" && chmod 644 \"$kl_dir/gpio-keys.kl\" && ui_print \"- Ekstra tus eslemesi kuruldu\""
  print "    unpack \"ExtraKey.apk\" \"$TMPDIR/ExtraKey.apk\""
  print "    unpack \"privapp-permissions-extrakey.xml\" \"$TMPDIR/privapp-permissions-extrakey.xml\""
  print "    unpack \"sysconfig-extrakey.xml\" \"$TMPDIR/sysconfig-extrakey.xml\""
  print "    ek_dir=\"${system_ext:-/system_ext}\""
  print "    mkdir -p \"$ek_dir/etc/sysconfig\" && cp -f \"$TMPDIR/sysconfig-extrakey.xml\" \"$ek_dir/etc/sysconfig/sysconfig-extrakey.xml\" && chmod 644 \"$ek_dir/etc/sysconfig/sysconfig-extrakey.xml\""
  print "    mkdir -p \"$ek_dir/priv-app/ExtraKey\" \"$ek_dir/etc/permissions\" && cp -f \"$TMPDIR/ExtraKey.apk\" \"$ek_dir/priv-app/ExtraKey/ExtraKey.apk\" && cp -f \"$TMPDIR/privapp-permissions-extrakey.xml\" \"$ek_dir/etc/permissions/privapp-permissions-extrakey.xml\" && chmod 755 \"$ek_dir/priv-app/ExtraKey\" && chmod 644 \"$ek_dir/priv-app/ExtraKey/ExtraKey.apk\" \"$ek_dir/etc/permissions/privapp-permissions-extrakey.xml\" && ui_print \"- Ekstra tus uygulamasi kuruldu\""
  print $0; done=1; next } {print} END { exit !done }' "$W/customize.sh" > "$W/customize.sh.new" || { echo "HATA: customize.sh icinde kanca noktasi bulunamadi" >&2; exit 1; }
mv "$W/customize.sh.new" "$W/customize.sh"

echo "==> Paketleniyor: $OUT"
mkdir -p "$(dirname "$OUT")"; rm -f "$OUT"
( cd "$W" && zip -r -X -q "$OUT" . )
ls -l "$OUT" | awk '{printf "    boyut: %.0f MB\n", $5/1048576}'
if command -v md5sum >/dev/null; then md5sum "$OUT"; else md5 "$OUT"; fi
