#!/bin/sh
export GXP_SESSION `gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
MYNAME=`hostname -f`
SRCJARFILE=$HOME/dachmaestro-0.2.zip
INSTALLJARFILE=$HOME/tmp-dachmaestro-0.2.zip
JAR=/usr/local/jdk/bin/jar
gxpc quit
gxpc use ssh '' ''
gxpc explore -t $HOME/deployment-tables/headnodes.list
gxpc e scp $MYNAME:$SRCJARFILE $INSTALLJARFILE
gxpc e $JAR xf $INSTALLJARFILE
gxpc e rm -f $INSTALLJARFILE
gxpc quit
