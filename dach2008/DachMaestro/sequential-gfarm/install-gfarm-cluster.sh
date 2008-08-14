#!/bin/sh
GFARMDIR=/tmp/dach001
for host in $*; do
    ssh $host ./install-gfarm-site.sh
done
