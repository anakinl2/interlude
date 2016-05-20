@echo off
:start
TITLE L2Dream Auth Server
echo %DATE% %TIME% Auth server is running !!! > login_is_running.tmp
echo Starting L2Dream Auth Server.
echo.
java -Xms32m -Xmx32m -cp lib/*; l2d.auth.L2LoginServer
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormaly
echo.
:end
echo.
echo server terminated
echo.
del login_is_running.tmp
pause