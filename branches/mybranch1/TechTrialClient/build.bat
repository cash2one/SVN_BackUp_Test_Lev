REM store current working directory in order to get back after build
set CUR_DIR=%CD%

REM set msBuildDir=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319
set msBuildDir=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319

REM check if any msconfig exists at all
REM dir /s %WINDIR%\Microsoft.NET\msbuild.exe

REM CD %CUR_DIR%\project\

REM call msbuild on the solution
REM call %msBuildDir%\msbuild.exe  PassValueToForm.sln /p:Configuration=Debug /l:FileLogger,Microsoft.Build.Engine;logfile=Manual_MSBuild_ReleaseVersion_LOG.log


REM to check without any build option
call %msBuildDir%\msbuild.exe  %CUR_DIR%\TechTrialClient.csproj /property:Configuration=Debug

REM change back to orig directory
REM CD %CUR_DIR%

REM call %msBuildDir%\msbuild.exe .\project\PassValueToForm\PassValueToForm.csproj /p:Config=Debug

set msBuildDir=

REM copy the installer to output\installers
REM mkdir .\output\installers\PassValueToForm\
REM xcopy /s .\project\PassValueToForm\bin\Debug\PassValueToForm.exe .\output\installers\PassValueToForm\
REM xcopy /s .\build\install.bat .\output\installers\


exit /b 0;