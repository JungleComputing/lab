#!/bin/bash

SITE=$1
shift

./bin/site-cmd.sh $SITE /bin/ls $@
