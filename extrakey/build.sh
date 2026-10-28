#!/usr/bin/env bash
# ExtraKey.apk uretir (sadece surduruculer icin). JDK 17 ve Android SDK build-tools gerekir.
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_SDK:-/opt/homebrew/share/android-commandlinetools}
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
PLAT=$(ls -d "$SDK"/platforms/android-* | sort -V | tail -1)/android.jar
JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}
export PATH="$JAVA_HOME/bin:$PATH"

B=build; rm -rf "$B"; mkdir -p "$B/gen" "$B/cls" "$B/res"
"$BT/aapt2" compile --dir res -o "$B/res"
"$BT/aapt2" link -o "$B/base.apk" -I "$PLAT" --manifest AndroidManifest.xml \
  --java "$B/gen" --min-sdk-version 35 --target-sdk-version 35 "$B"/res/*.flat
javac -source 17 -target 17 -classpath "$PLAT" -d "$B/cls" $(find src "$B/gen" -name '*.java')
"$BT/d8" --release --min-api 34 --lib "$PLAT" --output "$B" $(find "$B/cls" -name '*.class')
cp "$B/base.apk" "$B/unsigned.apk"
( cd "$B" && zip -q -j unsigned.apk classes.dex )
"$BT/zipalign" -f 4 "$B/unsigned.apk" "$B/aligned.apk"
KS="extrakey.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias key -keyalg RSA -keysize 2048 \
  -validity 10000 -dname "CN=Mi9 ROM ExtraKey" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out ExtraKey.apk "$B/aligned.apk"
"$BT/apksigner" verify ExtraKey.apk && ls -l ExtraKey.apk
