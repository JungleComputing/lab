#!/bin/sh
#
# Usage: install-gfarm-all
#
MYNAME=`hostname -f`
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS=`cat deployment-tables/remoteheadnodes.list'
rm -rf gfarmlog
mkdir gfarmlog
gxpc use ssh '' ''
gxpc explore $HOSTS
gxpc e -H $MYNAME scp $MYNAME:.gfarm_shared_key .
gxpc e sh -x install-gfarm-site.sh deployment-tables/'`hostname`'-all.list '>' '`hostname`'-install-gfarm.out '2>' '`hostname`'-install-gfarm.err
gxpc e scp -r '*-install-gfarm.*' '`hostname`'-gfarm-output `hostname -f`:gfarmlog
gxpc quit
