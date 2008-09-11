#!/bin/sh
java -cp ./lib/*:./lib/deploy/*:./lib/ibis/*:. -Dsmartsockets.servicelink.force=true -Dsshtrilead.connect.timeout=10000 -Dsshtrilead.kex.timeout=10000 -Dgat.adaptor.path=./lib/deploy/adaptors $@
