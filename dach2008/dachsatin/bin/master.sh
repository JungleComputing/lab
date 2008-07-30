#!/bin/sh
java -cp ./lib/*:./lib/deploy/*:./lib/ibis/*:. -Duser.name=dach004 -Dgat.adaptor.path=./lib/deploy/adaptors ibis.dachsatin.deployment.master.Main $@
