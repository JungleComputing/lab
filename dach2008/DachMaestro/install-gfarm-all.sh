#!/bin/sh
#
# Usage: install-gfarm-all
#
MYNAME=`hostname -f`
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS=`cat deployment-tables/remoteheadnodes.list`
rm -rf gfarmlog
mkdir gfarmlog
for host in $HOSTS; do
    scp install-gfarm-site.sh $host
    ssh $host sh -x ./install-gfarm-site.sh deployment-tables/$host-all.list '>' $host-instal-gfarm.out '2>' $host-istall-gfarm.err
    scp -r '*-install-gfarm.*' $host-gfarm-output `hostname -f`:gfarmlog
done
