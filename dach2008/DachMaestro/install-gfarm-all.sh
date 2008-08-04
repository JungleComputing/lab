#!/bin/sh
#
# Usage: install-gfarm-all
#
MYNAME=`hostname -f`
export GXP_SESSION=`gxpc --create_daemon 1`
trap 'gxpc quit; exit 1' 2
HOSTS="suzuk000.intrigger.titech.ac.jp chiba000.intrigger.nii.ac.jp hongo000.logos.ic.i.u-tokyo.ac.jp imade000.kuis.kyoto-u.ac.jp kyoto000.para.media.kyoto-u.ac.jp okubo000.yama.info.waseda.ac.jp mirai000.intrigger.jp kobe000.intrigger.scitec.kobe-u.ac.jp kyushu000.bioinfo.kyushu-u.ac.jp hiro000.net.hiroshima-u.ac.jp keio000.sslab.ics.keio.ac.jp"
rm -rf gfarmlog
mkdir gfarmlog
gxpc use ssh '' ''
gxpc explore $HOSTS
gxpc e -H $MYNAME scp $MYNAME:.gfarm_shared_key .
gxpc e sh -x install-gfarm-site.sh deployment-tables/'`hostname`'-all.list '>' '`hostname`'-install-gfarm.out '2>' '`hostname`'-install-gfarm.err
gxpc e scp -r '*-install-gfarm.*' '`hostname`'-gfarm-output `hostname -f`:gfarmlog
gxpc quit
