#!/bin/bash

SITE=$1
shift

./bin/site-cmd.sh $SITE "mkdir -p /tmp/dach004/dfs && fusermount -u /tmp/dach004/dfs && /data/local/gfarm_v2/bin/gfarm2fs /tmp/dach004/dfs"
