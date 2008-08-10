#!/bin/sh
LOGDIR=`hostname`-gfarm-output
GFARMDIR=/tmp/dach001
MYNAME=`hostname -f`
rm -rf $LOGDIR
mkdir $LOGDIR
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
/usr/bin/fusermount -u $GFARMDIR
gxpc use ssh '' ''
## We're very persistent since the nodes can be very busy.
gxpc explore --timeout 120 `cat $1`
gxpc e -H `hostname` /usr/bin/fusermount -u $GFARMDIR
gxpc quit
