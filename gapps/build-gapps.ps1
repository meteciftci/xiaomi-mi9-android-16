# Ozel GApps zip'i uretir (Windows): tam NikGapps paketinden sadece packages.txt'teki ogeleri tutar.
# build-gapps.sh'in PowerShell karsiligidir. Windows 10 (build 17063+) ile gelen tar.exe gerekir.
param(
  [Parameter(Mandatory = $true)][string]$Source,
  [Parameter(Mandatory = $true)][string]$Output
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding $false

$W = Join-Path ([IO.Path]::GetTempPath()) ('mi9gapps-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $W | Out-Null
try {
  Write-Host "==> Aciliyor: $(Split-Path $Source -Leaf)"
  & tar -xf $Source -C $W
  if ($LASTEXITCODE -ne 0) { throw 'tar ile acilamadi (Windows 10 build 17063 ve uzeri gerekir).' }

  # packages.txt: [config] ve [files] bolumleri
  $sec = $null; $enable = @{}; $keep = @{}
  foreach ($l in Get-Content (Join-Path $here 'packages.txt')) {
    $t = $l.Trim()
    if ($t -eq '' -or $t.StartsWith('#')) { continue }
    if ($t -match '^\[(.+)\]$') { $sec = $Matches[1]; continue }
    if ($sec -eq 'config') { $enable[$t] = $true } elseif ($sec -eq 'files') { $keep[$t] = $true }
  }

  Write-Host '==> nikgapps.config guncelleniyor'
  $settings = @('WipeDalvikCache', 'WipeRuntimePermissions', 'ExecuteBackupRestore', 'OverwriteWithZipConfig', 'GmsOptimization', 'GenerateLogs')
  $cfgPath = Join-Path $W 'afzc/nikgapps.config'
  $lines = foreach ($l in Get-Content $cfgPath) {
    if ($l -match '^((>>)?[A-Za-z0-9]+)=([01])\s*$') {
      $key = $Matches[1]; $plain = $key -replace '^>>', ''
      if ($key -eq 'UseZipConfig') { 'UseZipConfig=1' }
      elseif ($settings -contains $plain) { $l }
      elseif ($enable.ContainsKey($key)) { "$key=1" }
      else { "$key=0" }
    } else { $l }
  }
  # Recovery'deki sh icin LF satir sonu sart
  [IO.File]::WriteAllText($cfgPath, (($lines -join "`n") + "`n"), $utf8)
  if (-not (Select-String -Path $cfgPath -Pattern '^UseZipConfig=1' -Quiet)) { [IO.File]::AppendAllText($cfgPath, "UseZipConfig=1`n", $utf8) }

  Write-Host '==> debloater.config ekleniyor'
  $deb = (Get-Content (Join-Path $here 'debloater.config') -Raw) -replace "`r`n", "`n"
  [IO.File]::WriteAllText((Join-Path $W 'afzc/debloater.config'), $deb, $utf8)

  Write-Host '==> Kullanilmayan paketler siliniyor'
  $appSet = Join-Path $W 'AppSet'
  $removed = 0
  Get-ChildItem $appSet -Recurse -Filter '*.tar.xz' | ForEach-Object {
    $rel = 'AppSet/' + ($_.FullName.Substring($appSet.Length + 1) -replace '\\', '/')
    if (-not $keep.ContainsKey($rel)) { Remove-Item $_.FullName; $removed++ }
  }
  Get-ChildItem $appSet -Directory | Where-Object { -not (Get-ChildItem $_.FullName) } | Remove-Item
  Write-Host "    silinen paket: $removed"
  foreach ($rel in $keep.Keys) { if (-not (Test-Path (Join-Path $W $rel))) { throw "zip'te yok: $rel" } }

  Copy-Item (Join-Path $here 'gpio-keys.kl') (Join-Path $W 'gpio-keys.kl')
  Copy-Item (Join-Path $here '..\extrakey\ExtraKey.apk') (Join-Path $W 'ExtraKey.apk')
  Copy-Item (Join-Path $here 'privapp-permissions-extrakey.xml') (Join-Path $W 'privapp-permissions-extrakey.xml')
  Copy-Item (Join-Path $here 'sysconfig-extrakey.xml') (Join-Path $W 'sysconfig-extrakey.xml')
  Copy-Item (Join-Path $here 'overlay-config.xml') (Join-Path $W 'overlay-config.xml')
  $cs = Join-Path $W 'customize.sh'
  $src = (Get-Content $cs -Raw) -replace "`r`n", "`n"
  $anchor = '    . "$COMMONDIR/install.sh"'
  if (-not $src.Contains($anchor)) { throw 'customize.sh icinde kanca noktasi bulunamadi' }
  $hook = @'
    unpack "overlay-config.xml" "$TMPDIR/overlay-config.xml"
    oc_dir="${product:-/product}/overlay/config"
    mkdir -p "$oc_dir" && cp -f "$TMPDIR/overlay-config.xml" "$oc_dir/config.xml" && chmod 644 "$oc_dir/config.xml" && ui_print "- Varsayilan hareketle gezinme ayarlandi"
    unpack "gpio-keys.kl" "$TMPDIR/gpio-keys.kl"
    kl_dir="${product:-/product}/usr/keylayout"
    mkdir -p "$kl_dir" && cp -f "$TMPDIR/gpio-keys.kl" "$kl_dir/gpio-keys.kl" && chmod 644 "$kl_dir/gpio-keys.kl" && ui_print "- Ekstra tus eslemesi kuruldu"
    unpack "ExtraKey.apk" "$TMPDIR/ExtraKey.apk"
    unpack "privapp-permissions-extrakey.xml" "$TMPDIR/privapp-permissions-extrakey.xml"
    unpack "sysconfig-extrakey.xml" "$TMPDIR/sysconfig-extrakey.xml"
    ek_dir="${system_ext:-/system_ext}"
    mkdir -p "$ek_dir/etc/sysconfig" && cp -f "$TMPDIR/sysconfig-extrakey.xml" "$ek_dir/etc/sysconfig/sysconfig-extrakey.xml" && chmod 644 "$ek_dir/etc/sysconfig/sysconfig-extrakey.xml"
    mkdir -p "$ek_dir/priv-app/ExtraKey" "$ek_dir/etc/permissions" && cp -f "$TMPDIR/ExtraKey.apk" "$ek_dir/priv-app/ExtraKey/ExtraKey.apk" && cp -f "$TMPDIR/privapp-permissions-extrakey.xml" "$ek_dir/etc/permissions/privapp-permissions-extrakey.xml" && chmod 755 "$ek_dir/priv-app/ExtraKey" && chmod 644 "$ek_dir/priv-app/ExtraKey/ExtraKey.apk" "$ek_dir/etc/permissions/privapp-permissions-extrakey.xml" && ui_print "- Ekstra tus uygulamasi kuruldu"
'@
  $src = $src.Replace($anchor + "`n", ($hook -replace "`r`n", "`n") + "`n" + $anchor + "`n")
  [IO.File]::WriteAllText($cs, $src, $utf8)

  Write-Host "==> Paketleniyor: $Output"
  New-Item -ItemType Directory -Force -Path (Split-Path $Output) | Out-Null
  if (Test-Path $Output) { Remove-Item $Output }
  # './' on eki olmasin diye ust duzey ogeleri tek tek veriyoruz
  $names = Get-ChildItem $W -Force | ForEach-Object { $_.Name }
  & tar -a -c -f $Output -C $W @names
  if ($LASTEXITCODE -ne 0) { throw 'zip olusturulamadi.' }
  '    boyut: {0:N0} MB' -f ((Get-Item $Output).Length / 1MB)
}
finally {
  Remove-Item $W -Recurse -Force -ErrorAction SilentlyContinue
}
