# Xiaomi Mi 9 (cepheus) - temiz crDroid 16 / clean crDroid 16

[Türkçe](#türkçe) | [English](#english)

---

## Türkçe

Xiaomi Mi 9 (`cepheus`) için, resmi **crDroid 16 (Android 16)** üzerine kurulan sade bir yapılandırma ve tek komutla çalışan kurucu. ROM dosyası **değiştirilmez**; resmi crDroid zip'i indirilir. Ek işler, kurulum sırasında çalışan özel bir GApps paketiyle yapılır.

### Ne değişiyor?

**Kaldırılanlar:** crDroid tarayıcısı (Jelly), MatLog, AxSandbox, Updater, Etar, Gallery2, Glimpse, Twelve, stok klavye, AudioFX, crDroid Telefon/Mesajlar/Kişiler/Saat/Hesap Makinesi ve 85 kilit ekranı saat yazı tipi (5 tanesi kalır).

**Eklenenler (Google):** Telefon, Mesajlar, Kişiler, Saat, Hesap Makinesi, Takvim, Fotoğraflar, Gboard, Carrier Services, Android Auto, Google Arama (Velvet), Google Metin Okuma, Play Store ve Play Hizmetleri.

**Varsayılan ayarlar (ilk açılışta bir kez uygulanır, sonra değiştirilebilir):**
- Hareketle gezinme
- Hızlı ayarlarda ses kaydırıcısı kapalı
- Dinamik çubuk açık
- Pil yüzdesi simgenin içinde
- Üç parmak kaydırma = ekran görüntüsü

**Ekstra tuş:** Cihazın sağ tarafındaki Mi AI tuşu, **Ayarlar > Sistem > Ekstra tuş** altından seçilen görevi yapar. Tek basış, çift basış ve uzun basış için ayrı görev atanabilir (asistan, kamera, fener, ekran görüntüsü, son uygulamalar, bildirimler, hızlı ayarlar, ekranı kilitle, istediğiniz uygulama). Ekran kapalıyken de çalışır. Kaynak kodu `extrakey/` içinde.

### Gereksinimler
- Bootloader'ı **açık** bir Mi 9.
- Bilgisayarda USB kablosu ile bağlı **tek** Android cihaz.
- Telefonda USB hata ayıklama açık (ya da telefon fastboot modunda) ve en az %50 şarj.
- macOS/Linux için `bash`, `curl`, `unzip`, `zip`, `awk`. Windows için Windows 10 (17063+) ve PowerShell.
- Yaklaşık 6 GB boş disk alanı.

### Kurulum
> **Uyarı:** Kurulum telefondaki **tüm veriyi siler**. Garanti ve sorumluluk yoktur, kendi riskinizle kullanın.

macOS / Linux:
```bash
git clone git@github.com:meteciftci/xiaomi-mi9-android-16.git
cd xiaomi-mi9-android-16
./install.sh
```
Windows: depoyu indirin ve `install.bat` dosyasına çift tıklayın. **Windows betikleri henüz test edilmedi.**

Betik sırayla: platform-tools indirir, dosyaları doğrular (MD5), resmi NikGapps zip'inden özel GApps paketini bu bilgisayarda üretir, veriyi formatlar, recovery'yi yükler, firmware, ROM ve GApps'i kurar ve telefonu başlatır. İlk açılış 5-10 dakika sürebilir.

### Dosyalar ve telif
- **ROM:** resmi crDroid'den indirilir (SourceForge).
- **Google uygulamaları bu depoda yoktur.** Resmi NikGapps zip'i indirilir ve paket sizin bilgisayarınızda üretilir.
- **Firmware ve recovery** (`manifest.conf` içinde `FW_URL`, `REC_URL`) için Release bağlantıları henüz eklenmedi; şimdilik `files/` klasörüne elle konur.

### Sınırlar
- Bootloader açık olduğu için **Play Integrity** büyük olasılıkla geçmez (bankacılık/ödeme uygulamaları çalışmayabilir).
- Bu bir test aşamasıdır; yalnızca `cepheus` ve belirtilen sürümlerle denenmiştir.

---

## English

A slimmed-down setup and one-command installer for the Xiaomi Mi 9 (`cepheus`) on top of the official **crDroid 16 (Android 16)**. The ROM zip is **not modified**; the official crDroid zip is downloaded. All extra work is done by a custom GApps package that is built on your own computer during installation.

### What changes?

**Removed:** crDroid browser (Jelly), MatLog, AxSandbox, Updater, Etar, Gallery2, Glimpse, Twelve, the stock keyboard, AudioFX, crDroid Dialer/Messaging/Contacts/Clock/Calculator, and 85 lock-screen clock fonts (5 stay).

**Added (Google):** Phone, Messages, Contacts, Clock, Calculator, Calendar, Photos, Gboard, Carrier Services, Android Auto, Google Search (Velvet), Google Text-to-speech, Play Store and Play Services.

**Defaults (applied once on first boot, changeable afterwards):**
- Gesture navigation
- Volume slider hidden in Quick Settings
- Dynamic bar on
- Battery percentage inside the icon
- Three-finger swipe = screenshot

**Side button:** The Mi AI button on the right side runs the action chosen in **Settings > System > Extra key**. Single press, double press and long press can each have their own action (assistant, camera, flashlight, screenshot, recents, notifications, quick settings, lock screen, any app). It also works with the screen off. Source code is in `extrakey/`.

### Requirements
- A Mi 9 with an **unlocked bootloader**.
- **Exactly one** Android device connected to the computer by USB.
- USB debugging on (or the phone in fastboot mode) and at least 50% battery.
- macOS/Linux: `bash`, `curl`, `unzip`, `zip`, `awk`. Windows: Windows 10 (17063+) and PowerShell.
- About 6 GB of free disk space.

### Install
> **Warning:** Installation **erases all data** on the phone. No warranty; use at your own risk.

macOS / Linux:
```bash
git clone git@github.com:meteciftci/xiaomi-mi9-android-16.git
cd xiaomi-mi9-android-16
./install.sh
```
Windows: download the repository and double-click `install.bat`. **The Windows scripts have not been tested yet.**

The script downloads platform-tools, verifies the files (MD5), builds the custom GApps package from the official NikGapps zip on your computer, formats data, installs the recovery, flashes firmware, ROM and GApps, then boots the phone. First boot can take 5-10 minutes.

### Files and licensing
- **ROM:** downloaded from official crDroid (SourceForge).
- **No Google apps are in this repository.** The official NikGapps zip is downloaded and the package is built on your machine.
- **Firmware and recovery** (`FW_URL`, `REC_URL` in `manifest.conf`): release links are not set yet; for now put the files into `files/` manually.

### Limitations
- With an unlocked bootloader, **Play Integrity** will most likely fail (banking/payment apps may not work).
- This is a test-stage project; only tried on `cepheus` with the versions pinned in `manifest.conf`.

---

## Credits / Teşekkür
- [crDroid](https://crdroid.net) and the cepheus maintainer (raystef66)
- [NikGapps](https://nikgapps.com) (Nikhil Menghani)
- OrangeFox Recovery and the unofficial cepheus build
- Google platform-tools

## License / Lisans
Scripts and the Extra key app: MIT (see `LICENSE`). crDroid, NikGapps, OrangeFox and Google software keep their own licenses.
