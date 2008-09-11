package dach.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

import javax.swing.JFrame;

public class WorkFrame extends JFrame {
    
    public WorkFrame(LinkedList<ClusterStatistics> stats) { 
        
        super("Worker activity");
        
        final WorkPanel gp = new WorkPanel(this, stats);
   
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                remove(gp);
                dispose();
            }
        });
       
        add("Center", gp);
        
        pack();
        setVisible(true);
    }  
}