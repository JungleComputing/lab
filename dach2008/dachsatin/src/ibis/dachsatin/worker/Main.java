package ibis.dachsatin.worker;



import ibis.dachsatin.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Main {

	/**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        HashMap<String, Problem> problems = new HashMap<String, Problem>();
        
        boolean verbose = false;
        boolean robust = false;

        for (int i=0;i<args.length;i++) { 

            if (args[i].equals("-v") || args[i].equals("--verbose")) { 
                verbose = true;
            } else if (args[i].equals("-p") || args[i].equals("--problem")) {
            	
            	String ID = args[++i];
            	String dir = args[++i];
            	
            	problems.put(ID, new Problem(ID, dir));
            } else { 
            	System.err.println("FATAL: Unknown option: " + args[i]);
            	System.exit(1);
            }
        }

        if (problems.size() == 0) { 
        	System.err.println("FATAL: No problems specified!");
        	System.exit(1);
        }
        
		File dir = new File(Util.dataDir);
        
		FindPairs finder = new FindPairs(dir, problems.values(), verbose);
    	
    	ArrayList<Pair> pairs = null;
    	
    	try { 
    		pairs = finder.getPairs(robust);
    	} catch (IOException e) {
    		System.err.println("FATAL: Failed to load all pairs from directory " + dir);
    		e.printStackTrace(System.err);
    		System.exit(1);
    	}
    	
    	if (pairs.size() == 0) { 
    		System.err.println("FATAL: No pairs found in directory " + dir);
    		System.exit(1);
    	}
    	
    	if (verbose) { 
    		System.out.println("Main starting comparison of " + pairs.size() + " pairs.");        	        	        	
    	}
    	
    	Comparator c = new Comparator();
    	ArrayList<Result> results = c.start(pairs);
        
        long end = System.currentTimeMillis();

        int jobs = results.size();
        long total = Result.totalTime(results);
        long app = (end-start);

        double avg = ((double) total) / jobs;
        double speedup = ((double) total) / app;

        System.out.printf("DONE: Application took       : %d ms.\n", app);        	        	
        System.out.printf("DONE: Processed jobs         : %d\n", jobs);        	
        System.out.printf("DONE: Accum. processing time : %d ms.\n", total);
        System.out.printf("DONE: Avg time/job           : %.2f ms.\n", avg);
        System.out.printf("DONE: Speedup                : %.2f\n", speedup);        	
        
        for (Result r : results) { 
        	
        	System.out.println("STATS: Pair  : " + r.input.before + " - " + r.input.after);        
        	System.out.println("STATS: Time  : " + r.time);        
        	System.out.println("STATS: Error : " + r.stderr);        
        	System.out.println("STATS: Output: " + r.stdout);
        	System.out.println("STATS: Result: " + r.result.length() + " characters.");
        	System.out.println();        
        
        	Problem p = problems.get(r.input.ID);
        	
        	if (p == null) { 
        		System.err.println("ERROR: Failed to find problem " + r.input.ID);
        	} else { 
        
        		try { 
        			p.writeResult(r.result);
        		} catch (Exception e) {
        			System.err.println("ERROR: Failed to write result " + r.input.before 
        					+ " - " + r.input.after + " of problem " + p.directory 
        					+ " to output " + r.input.ID);
        		}       		
        	}
        }
        
        for (Problem p : problems.values()) { 
        	p.done();
        }
    }

}
