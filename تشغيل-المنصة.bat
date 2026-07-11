@echo off
chcp 65001 >nul
title منصة متابعة منظومة اجتماعات نائب الأمين
cd /d "%~dp0"

set "JAVA=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe"
set "MVN=C:\Users\drome\.tools\apache-maven-3.9.9\bin\mvn.cmd"

if not exist "target\meetings-erp.jar" (
  echo لم يتم العثور على الحزمة - جاري البناء لأول مرة، انتظر قليلًا...
  call "%MVN%" -q -DskipTests package
)

echo.
echo ============================================================
echo    منصة متابعة منظومة اجتماعات نائب الأمين
echo    الرابط:   http://localhost:8080
echo    سيفتح المتصفح تلقائيًا خلال ثوانٍ.
echo    للإيقاف: أغلق هذه النافذة.
echo ============================================================
echo.

rem فتح المتصفح تلقائيًا بعد أن يجهز الخادم
start "" cmd /c "timeout /t 18 /nobreak >nul & start http://localhost:8080"

"%JAVA%" -jar "target\meetings-erp.jar" --spring.datasource.url="jdbc:h2:file:./data/meetingsdb"

pause
