#!/bin/sh
java -cp ./lib/*:./lib/deploy/*:./lib/ibis/*:. -Dsshtrilead.connect.timeout=10000 -Dsshtrilead.kex.timeout=10000 -Duser.name=dach004 -Dgat.adaptor.path=./lib/deploy/adaptors ibis.dachsatin.deployment.master.Main $@
