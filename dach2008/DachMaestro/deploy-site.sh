#!/bin/sh
LOGDIR=`hostname`-output
rm -rf $LOGDIR
mkdir $LOGDIR
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
gxpc use ssh '' ''
gxpc explore `cat $1`
gxpc e $2 '>' $LOGDIR/'`hostname`'.out '2>' $LOGDIR/'`hostname`'.err
gxpc quit
