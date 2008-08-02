#!/bin/sh
GXP_SESSION=`gxpc --create_daemon 1`
export GXP_SESSION
trap 'gxpc quit; exit 1' 2
MYNAME=`hostname -f`
GFARMDIR=/tmp/dach001
/usr/bin/fusermount -u $GFARMDIR
rm -rf $GFARMDIR
mkdir $GFARMDIR
/data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
gxpc use ssh '' ''
gxpc explore --timeout 60 -t $HOME/deployment-tables/remoteheadnodes.list
gxpc e scp $MYNAME:.gfarm_shared_key .
gxpc e /usr/bin/fusermount -u $GFARMDIR
gxpc e rm -rf $GFARMDIR
gxpc e mkdir $GFARMDIR
gxpc e /data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
gxpc quit
