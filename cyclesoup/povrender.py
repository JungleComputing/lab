#!/usr/bin/env python
import sys
import os
import string

scp = 'scp -B -q '

host = 'lily'
components_dir = host + ":kunst/gliders/"

def execute( cmd ):
    print 'execute [' + cmd + ']'
    os.system( cmd )

jobs = sys.argv[1:]

jobset = '{' + string.join( jobs, ',' ) + '}'

def mapfmt( fmt, l ):
    res = None
    for e in l:
        s = fmt % e
        if res == None:
            res = s
        else:
            res += ' ' + s
    return res

execute( scp + components_dir + "'*.inc' " + components_dir + "'frames/" + jobset  + ".pov' . " )
for job in jobs:
    cmd="povray +W720 +H540 +FN16 +Q9 -V -GW -GS -GR -D +A0.1 +R2 +AM1 +J0 +I%s.pov +O%s.png > %s-out.txt 2> %s-err.txt" % (job, job, job, job)
    execute( cmd )
execute( scp + "*.png " + components_dir + "cells/" )
execute( scp + "*-out.txt *-err.txt " + components_dir + "log/" )

sys.exit( 0 )
