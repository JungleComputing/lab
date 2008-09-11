package dach.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class WorkPanel extends JPanel implements ActionListener {

	private static final Logger logger = Logger.getLogger(WorkPanel.class);
	
    // Generated
    private static final long serialVersionUID = 1L;

    private static final String ADD    = "Add";
    private static final String REMOVE = "Remove";
    
    private JFrame frame;
    
    private WorkMap map;
    
    public WorkPanel(JFrame frame, LinkedList<ClusterStatistics> stats) {
        
        this.frame = frame;        
        
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        map = new WorkMap(stats);
         
        add(map);       
    }

    public void actionPerformed(ActionEvent e) {
        logger.debug("Got event: " + e);        
    }
}
