#!/bin/sh
LOGDIR=`hostname`-gfarm-output
GFARMDIR=/tmp/dach001
MYNAME=`hostname -f`
rm -rf $LOGDIR
mkdir $LOGDIR
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
/usr/bin/fusermount -u $GFARMDIR
rm -rf $GFARMDIR
mkdir $GFARMDIR
/data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
gxpc use ssh '' ''
## We're very persistent since the nodes can be very busy.
## The point is to change that...
gxpc explore --timeout 60 `cat $1`
gxpc e -H `hostname` /usr/bin/fusermount -u $GFARMDIR
gxpc e -H `hostname` rm -rf $GFARMDIR
gxpc e -H `hostname` mkdir $GFARMDIR
gxpc e -H `hostname` /data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
gxpc quit
