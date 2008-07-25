set term postscript eps enhanced
set output "copy.eps"
set font "Helvetica"
set key left top
set xlabel "Copy"
set ylabel "Time in ms." 

plot "times3-copying.sorted" title "Copy time", 2000 with lines title "LOCAL" 
