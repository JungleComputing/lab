#!/bin/bash

SITE=$1
shift

./bin/site-cmd.sh $SITE /bin/killall -u dach004
