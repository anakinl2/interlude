@echo off
:start
echo.
TITLE L2Dream Game Server
echo %DATE% %TIME% Game server is running !!! > gameserver_is_running.tmp
echo Starting L2Dream Game Server.
echo.
rem ======== Optimize memory settings =======
rem Minimal size with geodata is 1.5G, w/o geo 1G
rem Make sure -Xmn value is always 1/4 the size of -Xms and -Xmx.
rem -Xms and -Xmx should always be equal.
rem ==========================================
java  -Dfile.encoding=UTF-8 -Xmn128m -Xms512m -Xmx1024m -cp lib/*; l2d.game.GameServer
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
del gameserver_is_running.tmp
pause
