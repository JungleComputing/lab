#!/bin/bash

SITE=$1
shift

./bin/site-cmd.sh $SITE /bin/ps -u dach004
