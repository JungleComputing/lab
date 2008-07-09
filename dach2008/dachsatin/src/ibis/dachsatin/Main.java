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
    	String command = null;
    	
    	for (int i=0;i<args.length;i++) { 
    		
    		if (args[i].equals("-v") || args[i].equals("--verbose")) { 
    			verbose = true;
    		} else if (args[i].equals("-c") || args[i].equals("--command")) {
    			if (command == null) { 
    				command = args[++i];
    			} else { 
    				System.err.println("Command already specified!");
    				System.exit(1);
    			}
    		} else if (args[i].equals("-d") || args[i].equals("--directory")) { 
    			if (dir == null) {  
    				dir = new File(args[++i]);
    			} else { 
    				System.err.println("Directory already specified!");
    				System.exit(1);
    			}    			
    		} else { 
    			usage();
    		}
    	}
    	
    	if (dir == null) { 
    		System.err.println("No directory specified!");
			System.exit(1);	
    	}
    	
    	if (command == null) { 
    		System.err.println("No command specified!");
			System.exit(1);	
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
    	Result r = c.start(pairs, command);
        
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
