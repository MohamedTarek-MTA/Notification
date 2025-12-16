@echo off
title Kafka & Zookeeper Starter

REM Change this to your Kafka installation path
set KAFKA_HOME=C:\kafka

cd /d %KAFKA_HOME%

echo ===============================
echo Starting Zookeeper...
echo ===============================

start "Zookeeper" cmd /k "%KAFKA_HOME%\bin\windows\zookeeper-server-start.bat %KAFKA_HOME%\config\zookeeper.properties"

echo Waiting 20 seconds for Zookeeper to fully start...
timeout /t 20 >nul

echo ===============================
echo Starting Kafka Broker...
echo ===============================

start "Kafka Broker" cmd /k "%KAFKA_HOME%\bin\windows\kafka-server-start.bat %KAFKA_HOME%\config\server.properties"

echo Waiting 10 seconds for Kafka Broker to fully start...
timeout /t 10 >nul

echo ===============================
echo Listing Kafka Topics...
echo ===============================

start "Kafka Topics" cmd /k "%KAFKA_HOME%\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --list"

echo ===============================
echo Kafka and Zookeeper are now running.
echo ===============================

pause