set term postscript eps enhanced
set output "times.eps"
set font "Helvetica"
set key left top
set xlabel "Jobs"
set ylabel "Time in ms." 

plot [0:125][0:1200000] \
     "times.sorted" title "Naive" w l , \
     "times2.sorted" title "Local TMP" w l , \
     "times3.sorted" title "Local TMP/DATA" w l  
