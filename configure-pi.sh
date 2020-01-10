#!/bin/bash
apt-get update
echo "Installing Open JDK"
apt-get install -y openjdk-8-jdk
echo "Installing PI4J"
curl -sSL https://pi4j.com/install | sudo bash
echo "Installing WiringPi"
apt-get install -y wiringpi
echo "Necessary Fermbot dependencies successfully installed"

#enable ds18b20
