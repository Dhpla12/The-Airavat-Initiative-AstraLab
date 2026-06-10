$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path "$PSScriptRoot\..\.."
$AppName = "AstraLab"
$AppVersion = "1.0.0"
$MainJar = "astralab-$AppVersion.jar"
$MainClass = "com.airavat.astralab.ui.AstraLabLauncher"
$ArvtAssoc = "scripts\packaging\file-associations\arvt.properties"
$AprojAssoc = "scripts\packaging\file-associations\aproj.properties"
$UpgradeUuid = "2F48B3EF-73B0-463A-9F8D-B2D06E7EEB13"

Set-Location $RootDir
mvn -DskipTests package

Remove-Item -Recurse -Force "target\jpackage-input" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "target\installer" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "target\jpackage-input\lib" | Out-Null
New-Item -ItemType Directory -Force "target\installer" | Out-Null
Copy-Item "target\$MainJar" "target\jpackage-input\"
Copy-Item "target\lib\*.jar" "target\jpackage-input\lib\"

jpackage `
  --type msi `
  --name $AppName `
  --app-version $AppVersion `
  --vendor "Airavat Aerospace" `
  --input "target\jpackage-input" `
  --main-jar $MainJar `
  --main-class $MainClass `
  --dest "target\installer" `
  --file-associations $ArvtAssoc `
  --file-associations $AprojAssoc `
  --win-menu `
  --win-menu-group $AppName `
  --win-shortcut `
  --win-upgrade-uuid $UpgradeUuid `
  --java-options "-Xmx1g"

Write-Host "Windows MSI written to target\installer"
