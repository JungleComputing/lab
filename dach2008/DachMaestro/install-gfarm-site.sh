#!/bin/sh
LOGDIR=`hostname`-gfarm-output
GFARMDIR=/tmp/dach001
MYNAME=`hostname -f`
rm -rf $LOGDIR
mkdir $LOGDIR
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
./install-gfarm.sh
gxpc use ssh '' ''
## We're very persistent since the nodes can be very busy.
## The point is to change that...
gxpc explore --timeout 60 `cat $1`
gxpc e -H `hostname` ./install-gfarm.sh
gxpc quit
