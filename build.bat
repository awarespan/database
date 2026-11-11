@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set GRADLE_USER_HOME=C:\Users\bchbe\AppData\Local\Temp\gradle-home
cd /d C:\Shared\Projects\SIP-Android\backend
call gradlew.bat compileJava --no-daemon
