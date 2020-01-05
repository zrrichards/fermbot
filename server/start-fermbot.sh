#!/bin/bash
sudo java -classpath .:/opt/pi4j/lib/'*' -Dmicronaut.environments=raspberrypi -jar fermbot-0.1-all.jar
