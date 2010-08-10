@if "%DEBUG%" == "" @echo off

@rem 
@rem $Revision: 19761 $ $Date: 2010-04-05 15:29:23 +0300 (Mon, 05 Apr 2010) $
@rem

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

:begin
@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

set CLASSNAME=groovy.ui.InteractiveShell
if "%OLDSHELL%" == "" set CLASSNAME=org.codehaus.groovy.tools.shell.Main

"%DIRNAME%\startGroovy.bat" "%DIRNAME%" %CLASSNAME% %*

@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
%COMSPEC% /C exit /B %ERRORLEVEL%
