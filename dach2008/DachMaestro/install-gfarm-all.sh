#!/bin/sh
#
# Usage: install-gfarm-all
#
MYNAME=`hostname -f`
HOSTS=`cat deployment-tables/remoteheadnodes.list`
rm -rf gfarmlog
mkdir gfarmlog
for host in $HOSTS; do
    scp instal-q l-gfarm-site.sh $host:
    ssh $host sh -x ./install-gfarm-site.sh deployment-tables/$host-all.list 
done
