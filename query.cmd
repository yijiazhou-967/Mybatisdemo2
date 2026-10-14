@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
call mvn -B -q -gs docs\query-maven-settings.xml -s docs\query-maven-settings.xml "-Dmaven.repo.local=%USERPROFILE%\.m2\repository" -DskipTests compile org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath "-Dmdep.includeScope=runtime" "-Dmdep.outputFile=target/query-classpath.txt" <nul
if errorlevel 1 exit /b 1
for /f "usebackq delims=" %%C in ("target\query-classpath.txt") do set "QUERY_CLASSPATH=%%C"
java -Dfile.encoding=UTF-8 -cp "target\classes;%QUERY_CLASSPATH%" com.console.RelationshipConsole %*
exit /b %errorlevel%
