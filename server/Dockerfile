FROM arm64v8/openjdk:11-jre-slim-buster
RUN curl -sSL https://pi4j.com/install | sudo bash
COPY build/libs/fermbot-*-all.jar fermbot.jar
COPY python python
EXPOSE 8080

#The following -Dmicronaut-environments sets this to run on a Rasbperry Pi (Will attempt to load raspberry-pi specific
#bluetooth modules and other hardware, etc). If you are going to run this Dockerfile on Linux as a simulation, remove 
#the -D argument or it will not work.
CMD java -Dcom.sun.management.jmxremote -Dmicronaut-environments=Raspberry-Pi -noverify ${JAVA_OPTS} -jar fermbot.jar
