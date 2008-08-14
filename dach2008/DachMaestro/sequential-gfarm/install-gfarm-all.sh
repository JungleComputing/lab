#!/bin/sh
#
# Usage: install-gfarm-all
#
HOSTS=`cat deployment-tables/remoteheadnodes.list`
SITESCRIPT=install-gfarm-site.sh
CLUSTERSCRIPT=install-gfarm-cluster.sh
rm -rf gfarmlog
mkdir gfarmlog
for host in $HOSTS; do
    scp $SITESCRIPT $CLUSTERSCRIPT $host:
    ssh $host chmod +x $SITESCRIPT $CLUSTERSCRIPT
    ssh $host ./$CLUSTERSCRIPT `cat deployment-tables/$host-all.list`
done
