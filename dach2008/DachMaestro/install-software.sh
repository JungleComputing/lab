#!/bin/sh
GXP_SESSION=`gxpc --create_daemon 1`
export GXP_SESSION
trap 'gxpc quit; exit 1' 2
MYNAME=`hostname -f`
SRCJARFILE=$HOME/dachmaestro-0.2.zip
INSTALLJARFILE=$HOME/tmp-dachmaestro-0.2.zip
JAR=/usr/local/jdk/bin/jar
rm -rf dachmaestro-0.2
$JAR xf $SRCJARFILE
chmod +x *.sh
gxpc use ssh '' ''
gxpc explore --timeout 120 -t $HOME/deployment-tables/remoteheadnodes.list
gxpc e -H `hostname` scp $MYNAME:$SRCJARFILE $INSTALLJARFILE
gxpc e -H `hostname` rm -rf '*.sh' deployment-tables dachmaestro-0.2
gxpc e -H `hostname` $JAR xf $INSTALLJARFILE
gxpc e -H `hostname` rm -f $INSTALLJARFILE
gxpc e -H `hostname` chmod +x '*.sh'
gxpc e -H `hostname` cp -f .bashrc .profile
gxpc quit
