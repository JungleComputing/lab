#!/bin/sh
#
# Usage: clean-all
#
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS=`cat deployment-tables/remoteheadnodes.list`
rm -rf cleanlog
mkdir cleanlog
gxpc use ssh '' ''
gxpc explore --timeout 60 -t deployment-tables/headnodes.list
gxpc e sh -x clean-site.sh deployment-tables/'`hostname`'-all.list '>' '`hostname`'-cleaner.out '2>' '`hostname`'-cleaner.err
gxpc e scp -r '*-cleaner.*' '`hostname`'-cleaner-output `hostname -f`:cleanlog
gxpc quit
