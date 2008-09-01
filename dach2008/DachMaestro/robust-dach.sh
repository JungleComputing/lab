#!/bin/sh
set -e -u

case `uname -m` in
    "x86_64")
      BINDIR=/home/dach/bin64
      ;;
    "i686")
      BINDIR=/home/dach/bin32
      ;;
    *)
      echo "Unknown environment"
      exit ;;
esac

worktop=`pwd`
pflag="N"
old_pwd=`pwd`
thres=1000
sflag="N"
rflag="N"

while getopts "pw:t:s:r" opt
do
  case $opt in
      p ) pflag="Y" ;;
      w ) worktop=$OPTARG ;;
      t ) thres=$OPTARG ;;
      s ) sflag="Y"
          sth=$OPTARG ;;
      r ) rflag="Y" ;;
  esac
done
shift $((OPTIND - 1))

if test $# -ne 2
then
    echo "Usage : dach.sh [-w <working dir>] [-p] <ref> <img>"
    echo "        -w : working directory (default : pwd)"
    echo "        -p : preserve working directory (default : erase directory)"
    exit
fi

workdir=$worktop/dach_work.`hostname`.$$
mkdir -p $workdir

cdir=`pwd`
ref_f=`basename $1`
ref_b=`echo $1 | cut -c 0-1`
if [ "$ref_b" = "/" ]; then
    ref_src=$1
else
    ref_src=$cdir/$1
fi
ref_tgt=$workdir/$ref_f

img_f=`basename $2`
img_b=`echo $2 | cut -c 0-1`
if [ "$ref_b" = "/" ]; then
    img_src=$2
else
    img_src=$cdir/$2
fi
img_tgt=$workdir/$img_f


ln -s $ref_src $ref_tgt
ln -s $img_src $img_tgt
cd $workdir

sh -e -u $BINDIR/run_shift.sh $ref_f $img_f $thres > dach.log
#$BINDIR/run_imsub4.sh $ref_f $img_f $thres >> dach.log
sh -e -u $BINDIR/run_imsub3vp3.sh $ref_f $img_f $thres >> dach.log

img_base=`basename $img_f .fits`
img_result=${img_base}_sub_msk.fits
sex_result=${img_base}.result
sth_param=

if [ $sflag = "Y" ]; then
    sth_param="-DETECT_THRESH $sth"
fi

$BINDIR/sex $img_result -c /home/dach/finder/sextractor_conf/default.sex -CATALOG_NAME $sex_result $sth_param

awk '!/^#/ { printf("%s %s \n", $7, $8) }' $sex_result

if [ $rflag != "N" ]; then
    awk '!/^#/ { printf("circle point %s %s\n", $5, $6) }' $sex_result > $sex_result.reg
fi

if [ $pflag = "N" ]; then
    cd $old_pwd
#    echo "rm -rf $workdir"
    rm -rf $workdir
fi
