param(
 [string]$Jdk = $env:JAVA_HOME,
 [string]$AndroidJar = ''
)
$ErrorActionPreference='Stop'
$project=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $project
if(!$Jdk){throw 'Set JAVA_HOME or pass -Jdk with a Java 17+ JDK directory.'}
if(!$AndroidJar){
 $sdkRoot=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{$env:ANDROID_SDK_ROOT}
 if(!$sdkRoot){throw 'Set ANDROID_HOME or pass -AndroidJar with the Android 35 android.jar path.'}
 $AndroidJar=Join-Path $sdkRoot 'platforms/android-35/android.jar'
}
$java=Join-Path $Jdk 'bin/java.exe'
$javac=Join-Path $Jdk 'bin/javac.exe'
$keytool=Join-Path $Jdk 'bin/keytool.exe'
foreach($item in @($java,$javac,$keytool,$AndroidJar)){if(!(Test-Path -LiteralPath $item)){throw "Missing: $item"}}
function CheckExit { if($LASTEXITCODE -ne 0){throw "Build command exited with $LASTEXITCODE"} }
New-Item -ItemType Directory -Path 'build/classes','build/gen','build/dex','build/signing','output','tools/aapt2' -Force | Out-Null
if(Test-Path -LiteralPath 'build/classes/SignApk.class'){Remove-Item -LiteralPath 'build/classes/SignApk.class'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$gradleSource=Get-Content -LiteralPath 'app/build.gradle' -Raw
function AndroidSetting([string]$name,[string]$pattern){$match=[regex]::Match($gradleSource,$pattern);if(!$match.Success){throw "Missing Android setting: $name"};return $match.Groups[1].Value}
$appId=AndroidSetting 'applicationId' "applicationId\s+'([^']+)'"
$versionName=AndroidSetting 'versionName' "versionName\s+'([^']+)'"
$versionCode=AndroidSetting 'versionCode' 'versionCode\s+(\d+)'
$minSdk=AndroidSetting 'minSdk' 'minSdk\s+(\d+)'
$targetSdk=AndroidSetting 'targetSdk' 'targetSdk\s+(\d+)'
$androidNs='http://schemas.android.com/apk/res/android'
[xml]$manifest=Get-Content -LiteralPath 'app/src/main/AndroidManifest.xml' -Raw
$manifest.DocumentElement.SetAttribute('package',$appId)
function AndroidAttribute($element,[string]$name,[string]$value){$attribute=$manifest.CreateAttribute('android',$name,$androidNs);$attribute.Value=$value;$element.Attributes.Append($attribute) | Out-Null}
AndroidAttribute $manifest.DocumentElement 'versionCode' $versionCode
AndroidAttribute $manifest.DocumentElement 'versionName' $versionName
$sdk=$manifest.CreateElement('uses-sdk')
AndroidAttribute $sdk 'minSdkVersion' $minSdk
AndroidAttribute $sdk 'targetSdkVersion' $targetSdk
$manifest.DocumentElement.PrependChild($sdk) | Out-Null
$manifest.Save((Join-Path $project 'build/AndroidManifest.xml'))
if(!(Test-Path -LiteralPath 'tools/aapt2/aapt2.exe')){[System.IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $project 'tools/aapt2.jar'),(Join-Path $project 'tools/aapt2'),$true)}
& 'tools/aapt2/aapt2.exe' compile --dir 'app/src/main/res' -o 'build/resources.zip'; CheckExit
& 'tools/aapt2/aapt2.exe' link -o 'build/unsigned.apk' --manifest 'build/AndroidManifest.xml' -I $AndroidJar --java 'build/gen' -A 'app/src/main/assets' 'build/resources.zip'; CheckExit
$sources=@(Get-ChildItem 'app/src/main/java','build/gen' -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
& $javac -encoding UTF-8 -source 8 -target 8 -classpath $AndroidJar -d 'build/classes' @sources; CheckExit
if(Test-Path -LiteralPath 'build/classes.jar'){Remove-Item -LiteralPath 'build/classes.jar'}
[System.IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $project 'build/classes'),(Join-Path $project 'build/classes.jar'))
& $java -cp 'tools/r8.jar' com.android.tools.r8.D8 --release --min-api 26 --lib $AndroidJar --output 'build/dex' 'build/classes.jar'; CheckExit
$apk=[System.IO.Compression.ZipFile]::Open((Join-Path $project 'build/unsigned.apk'),[System.IO.Compression.ZipArchiveMode]::Update)
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apk,(Join-Path $project 'build/dex/classes.dex'),'classes.dex') | Out-Null
# Windows AAPT2 can emit backslashes in nested asset names. Android AssetManager
# resolves forward slashes, so normalize these entries before signing.
$windowsEntries=@($apk.Entries | Where-Object {$_.FullName.Contains('\')})
foreach($entry in $windowsEntries){
 $targetName=$entry.FullName.Replace('\','/')
 $inputStream=$entry.Open(); $buffer=[System.IO.MemoryStream]::new();$inputStream.CopyTo($buffer);$inputStream.Dispose()
 $newEntry=$apk.CreateEntry($targetName);$outputStream=$newEntry.Open();$buffer.Position=0;$buffer.CopyTo($outputStream);$outputStream.Dispose();$buffer.Dispose();$entry.Delete()
}
$apk.Dispose()
if(!(Test-Path -LiteralPath 'tools/qingke-debug.p12')){
 & $keytool -genkeypair -keystore 'tools/qingke-debug.p12' -storetype PKCS12 -storepass android -keypass android -alias qingke-debug -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Qingke Local Development'; CheckExit
}
& $javac -cp 'tools/apksig.jar' -d 'build/signing' 'tools/SignApk.java'; CheckExit
$apkOutput="output/qingke-$versionName-debug.apk"
& $java -cp 'tools/apksig.jar;build/signing' SignApk 'tools/qingke-debug.p12' 'build/unsigned.apk' $apkOutput; CheckExit
Get-FileHash -LiteralPath $apkOutput -Algorithm SHA256
