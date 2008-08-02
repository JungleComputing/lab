#!/bin/sh
GXP_SESSION=`gxpc --create_daemon 1`
export GXP_SESSION
trap 'gxpc quit; exit 1' 2
MYNAME=`hostname -f`
SRCJARFILE=$HOME/dachmaestro-0.2.zip
INSTALLJARFILE=$HOME/tmp-dachmaestro-0.2.zip
JAR=/usr/local/jdk/bin/jar
GFARMDIR=/tmp/dach001
rm -rf dachmaestro-0.2
$JAR xf $SRCJARFILE
chmod +x *.sh
gxpc use ssh '' ''
gxpc explore --timeout 60 -t $HOME/deployment-tables/remoteheadnodes.list
gxpc e scp $MYNAME:$SRCJARFILE $INSTALLJARFILE
gxpc e rm -rf '*.sh' deployment-tables dachmaestro-0.2
gxpc e $JAR xf $INSTALLJARFILE
gxpc e rm -f $INSTALLJARFILE
gxpc e chmod +x '*.sh'
gxpc e cp -f .bashrc .profile
gxpc quit
