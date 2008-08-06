#!/bin/sh
#
# Usage: clean-all
#
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS="nimbus.titech.hpcc.jp suzuk000.intrigger.titech.ac.jp chiba000.intrigger.nii.ac.jp hongo000.logos.ic.i.u-tokyo.ac.jp imade000.kuis.kyoto-u.ac.jp kyoto000.para.media.kyoto-u.ac.jp okubo000.yama.info.waseda.ac.jp mirai000.intrigger.jp kobe000.intrigger.scitec.kobe-u.ac.jp kyushu000.bioinfo.kyushu-u.ac.jp hiro000.net.hiroshima-u.ac.jp keio000.sslab.ics.keio.ac.jp"
rm -rf cleanlog
mkdir cleanlog
gxpc use ssh '' ''
gxpc explore $HOSTS
gxpc e sh -x clean-site.sh deployment-tables/'`hostname`'-all.list '>' '`hostname`'-cleaner.out '2>' '`hostname`'-cleaner.err
gxpc e scp -r '*-cleaner.*' '`hostname`'-cleaner-output `hostname -f`:cleanlog
gxpc quit
