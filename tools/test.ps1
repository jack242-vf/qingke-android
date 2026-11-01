param([string]$Jdk=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
if(!$Jdk){throw 'Set JAVA_HOME or pass -Jdk with a Java 17+ JDK directory.'}
npm test
if($LASTEXITCODE -ne 0){throw 'JavaScript tests failed'}
$suffix=if($env:OS -eq 'Windows_NT'){'.exe'}else{''}
New-Item -ItemType Directory -Path 'build/native-tests' -Force | Out-Null
& (Join-Path $Jdk "bin/javac$suffix") -encoding UTF-8 -d 'build/native-tests' 'app/src/main/java/cn/qingke/timetable/UrlPolicy.java' 'app/src/main/java/cn/qingke/timetable/ScheduleEngine.java' 'tests/UrlPolicyTest.java' 'tests/ScheduleEngineTest.java'
if($LASTEXITCODE -ne 0){throw 'Java test compilation failed'}
foreach($test in @('UrlPolicyTest','ScheduleEngineTest')){
 & (Join-Path $Jdk "bin/java$suffix") -cp 'build/native-tests' $test
 if($LASTEXITCODE -ne 0){throw "Test failed: $test"}
}
