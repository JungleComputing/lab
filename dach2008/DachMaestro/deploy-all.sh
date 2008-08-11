#!/bin/sh
#
# Usage: deploy-all <site-set> <command> <output-directory>
#
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS=`cat deployment-tables/headnodes.list`
rm -rf $3
mkdir $3
gxpc use ssh '' ''
gxpc explore --timeout 60 -t deployment-tables/headnodes.list
gxpc e sh deploy-site.sh deployment-tables/'`hostname`'-$1.list "$2" '>' '`hostname`'-deployer.out '2>' '`hostname`'-deployer.err
gxpc e scp -r '*-deployer.*' '`hostname`'-output `hostname -f`:$3
gxpc quit
