#!/bin/bash

SITE=$1
shift

./bin/site-cmd.sh $SITE "fusermount -u /tmp/dach004/dfs ; rm -r /tmp/dach004"
