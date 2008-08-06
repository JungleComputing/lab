#!/bin/sh
#
# Usage: deploy-all <site-set> <command> <output-directory>
#
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS="nimbus.titech.hpcc.jp suzuk000.intrigger.titech.ac.jp chiba000.intrigger.nii.ac.jp hongo000.logos.ic.i.u-tokyo.ac.jp imade000.kuis.kyoto-u.ac.jp kyoto000.para.media.kyoto-u.ac.jp okubo000.yama.info.waseda.ac.jp mirai000.intrigger.jp kobe000.intrigger.scitec.kobe-u.ac.jp kyushu000.bioinfo.kyushu-u.ac.jp hiro000.net.hiroshima-u.ac.jp keio000.sslab.ics.keio.ac.jp"
rm -rf $3
mkdir $3
gxpc use ssh '' ''
gxpc explore $HOSTS
gxpc e sh deploy-site.sh deployment-tables/'`hostname`'-$1.list "$2" '>' '`hostname`'-deployer.out '2>' '`hostname`'-deployer.err
gxpc e scp -r '*-deployer.*' '`hostname`'-output `hostname -f`:$3
gxpc quit
