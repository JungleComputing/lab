#!/bin/sh
LOGDIR=`hostname`-output
rm -rf $LOGDIR
mkdir $LOGDIR
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
gxpc use ssh '' ''
gxpc explore --timeout 30 `cat $1`
shift
gxpc e $* '>' $LOGDIR/'`hostname`'.out '2>' $LOGDIR/'`hostname`'.err
gxpc quit
