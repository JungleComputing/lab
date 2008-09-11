#!/bin/sh

exec java -Xmx256M \
    -classpath ./lib/*:./lib/ibis/* \
    -Dlog4j.configuration=file:./log4j.properties \
    ibis.server.Server "$@"

