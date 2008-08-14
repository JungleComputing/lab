#!/bin/sh
GFARMDIR=/tmp/dach001
echo "installing gfarm on `hostname`"
/usr/bin/fusermount -u $GFARMDIR
rm -rf $GFARMDIR
mkdir $GFARMDIR
/data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
