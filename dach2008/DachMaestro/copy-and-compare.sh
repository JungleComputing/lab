#!/bin/sh
#
# -e: stop on non-zero exit code
# -u: complain about non-existent variables
set -e -u
DIR=/tmp/dach001-scratch-$$
COMPARATOR=/home/dach/finder/dach.sh
mkdir $DIR
cp $1 $DIR/file1.fits
cp $2 $DIR/file2.fits
$COMPARATOR -w $DIR $DIR/file1.fits $DIR/file2.fits
rm -rf $DIR
