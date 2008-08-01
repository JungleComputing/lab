#!/bin/sh
GXP_SESSION=`gxpc --create_daemon 1`
export GXP_SESSION
trap 'gxpc quit; exit 1' 2
MYNAME=`hostname -f`
SRCJARFILE=$HOME/dachmaestro-0.2.zip
INSTALLJARFILE=$HOME/tmp-dachmaestro-0.2.zip
JAR=/usr/local/jdk/bin/jar
GFARMDIR=/tmp/dach001
gxpc quit
gxpc use ssh '' ''
gxpc explore -t $HOME/deployment-tables/headnodes.list
gxpc e scp $MYNAME:.gfarm_shared_key .
gxpc e scp $MYNAME:$SRCJARFILE $INSTALLJARFILE
gxpc e $JAR xf $INSTALLJARFILE
gxpc e rm -f $INSTALLJARFILE
gxpc e chmod +x *.sh
gxpc e cp -f .bashrc .profile
gxpc e /data/local/gfarm_v2/bin/fusermount -u $GFARMDIR
gxpc e /data/local/gfarm_v2/bin/gfarm2fs $GFARMDIR
gxpc quit
