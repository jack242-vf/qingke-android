$ErrorActionPreference='Stop'
$items=@(
 @{File='aapt2.jar';Url='https://dl.google.com/android/maven2/com/android/tools/build/aapt2/8.9.2-12782657/aapt2-8.9.2-12782657-windows.jar';Hash='A1C812CC3100EF5AE90D46C12B2B8288133691AB63C5EC7B0F9A5DA23B26549E'},
 @{File='r8.jar';Url='https://dl.google.com/android/maven2/com/android/tools/r8/8.8.34/r8-8.8.34.jar';Hash='84F28F3934F983AF27939460E6B7B5E617530BCF8D61D9F72BB035B6819EC900'},
 @{File='apksig.jar';Url='https://dl.google.com/android/maven2/com/android/tools/build/apksig/8.9.2/apksig-8.9.2.jar';Hash='C070ED1394629D74641AA0906F60B2FFA1EE77E6366A1F93437F59717B1AEB89'}
)
foreach($item in $items){
 $destination=Join-Path $PSScriptRoot $item.File
 if(!(Test-Path -LiteralPath $destination)){Invoke-WebRequest -Uri $item.Url -OutFile $destination}
 if((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $item.Hash){throw "Checksum mismatch: $($item.File)"}
 Write-Output "$($item.File): SHA-256 verified"
}
