#!/bin/sh
GFARMDIR=/tmp/dach001
killall -q -9 gfarm2fs
sleep 1
/usr/bin/fusermount -u $GFARMDIR
#rm -rf $GFARMDIR
mkdir -p $GFARMDIR
/data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
