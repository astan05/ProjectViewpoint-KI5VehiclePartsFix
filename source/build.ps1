param(
    [Parameter(Mandatory=$true)][string]$GameDir,
    [Parameter(Mandatory=$true)][string]$ViewpointJar,
    [switch]$Test
)
$ErrorActionPreference='Stop'
$modRoot=Join-Path (Split-Path $PSScriptRoot -Parent) 'build'
$buildRoot=Join-Path ([IO.Path]::GetTempPath()) ('vpvparts-build-'+[guid]::NewGuid().ToString('N'))
$classDir=Join-Path $buildRoot 'classes'
$testDir=Join-Path $buildRoot 'tests'
New-Item -ItemType Directory -Force $classDir,$testDir | Out-Null
$zbJar=Join-Path $GameDir 'ZombieBuddy.jar'
$pzJar=Join-Path $GameDir 'projectzomboid.jar'
New-Item -ItemType Directory -Force $modRoot | Out-Null
$outJar=Join-Path $modRoot 'ViewpointVehiclePartsGuard.jar'
$sourceFiles=Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'vpvparts') -Filter '*.java' | ForEach-Object FullName
& javac -encoding UTF-8 -cp $zbJar -d $classDir $sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
& jar --create --file $outJar -C $classDir vpvparts
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }
if ($Test) {
    $compileClasspath=$pzJar+';'+$ViewpointJar+';'+$zbJar
    & javac -encoding UTF-8 -cp $compileClasspath -d $testDir $sourceFiles (Join-Path $PSScriptRoot 'GuardHarness.java') (Join-Path $PSScriptRoot 'AdviceHarness.java')
    if ($LASTEXITCODE -ne 0) { throw 'test compilation failed' }
    & java --sun-misc-unsafe-memory-access=allow -cp ($testDir+';'+$compileClasspath) GuardHarness
    if ($LASTEXITCODE -ne 0) { throw 'guard tests failed' }
    & java -cp ($testDir+';'+$compileClasspath) AdviceHarness
    if ($LASTEXITCODE -ne 0) { throw 'advice binding tests failed' }
}
Write-Output ('Built '+$outJar)
Write-Output ('Temporary build files: '+$buildRoot)
