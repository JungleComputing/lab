package ibis.dachsatin;


import ibis.util.FindPairs;
import ibis.util.Pair;

import java.io.File;
import java.util.Arrays;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Main {
   
    private static void usage() { 
		System.err.println("Usage: Main <directory>");
		System.exit(1);
	}
    
    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main(String[] args) {
    
    	long start = System.currentTimeMillis();
    	
    	if (args.length == 0) { 
    		usage();
    	}

    	File dir = null;
    	boolean verbose = false;
    	
    	for (String a : args) { 
    		
    		if (a.equals("-v") || a.equals("--verbose")) { 
    			verbose = true;
    		} else if (dir == null) {  
    			dir = new File(a);    	    			
    		} else { 
    			usage();
    		}
    	}
    	
    	if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) { 
    		System.err.println("Directory " + dir + " cannot be accessed!");
    		System.exit(1);
    	}

    	FindPairs finder = new FindPairs(dir);
    	
    	Pair [] pairs = finder.getPairs();
    	
    	if (pairs.length == 0) { 
    		System.err.println("No pairs found in directory " + args[0]);
    		System.exit(1);
    	}
    	
    	Comparator c = new Comparator();
    	Result r = c.start(pairs);
        
        long end = System.currentTimeMillis();
        
        System.out.println(r.mergeOutput());        
        
        if (verbose) { 
        	
        	int jobs = r.time.length;
        	long total = r.totalTime();
        	long app = (end-start);
        	
        	double avg = ((double) total) / jobs;
        	double speedup = ((double) total) / app;
        	
        	System.out.printf("Application took       : %d ms.\n", app);        	        	
        	System.out.printf("Processed jobs         : %d\n", jobs);        	
        	System.out.printf("Accum. processing time : %d ms.\n", total);
        	System.out.printf("Avg time/job           : %.2f ms.\n", avg);
        	System.out.printf("Speedup                : %.2f\n", speedup);        	
        	System.out.printf("Job Times              : " + Arrays.toString(r.time) + "\n");
        	
        }        
    }

}
