#!/bin/sh
#
# File: run-test.sh
#
# Created: Tuesday, March 27 2018
#

java -Xms1g -Xmx1g \
     -Djava.net.preferIPv4Stack=true \
     $* \
     -cp conf:`echo lib/*.jar | sed 's/ /:/g'` \
     io.gatling.app.Gatling \
     -s com.datastax.alexott.bootcamp.GatlingLoadSim \
     -rf results \
     -on bootcamp-poc-gatling
