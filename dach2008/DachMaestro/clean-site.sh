#!/bin/sh
LOGDIR=`hostname`-output
rm -rf $LOGDIR
mkdir $LOGDIR
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
gxpc use ssh '' ''
## We're very persistent since the nodes can be very busy.
## The point is to change that...
gxpc explore --timeout 120 `cat $1`
gxpc e /usr/bin/killall java '>' $LOGDIR/'`hostname`'.out '2>' $LOGDIR/'`hostname`'.err
gxpc e /usr/bin/killall imsub3vp3 '>' $LOGDIR/'`hostname`'.out '2>' $LOGDIR/'`hostname`'.err
gxpc quit
