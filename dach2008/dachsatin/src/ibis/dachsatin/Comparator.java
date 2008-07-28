/**
 * 
 */
package ibis.dachsatin;

import ibis.satin.SatinObject;
import ibis.util.RunProcess;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Comparator extends SatinObject implements ComparatorSatinInterface {
   
	/** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;

    private static final int MAX_ATTEMPTS = 10;
    
    // Timing hack
    private static long start = -1;

    // Local directory hack
	private static String dataDir = null;

	// Local temp dir hack
	private static String tmpDir = null;

	// Local copy operation hack
	private static String cpExec = null;
	
    // Machine identification hack
	private static String machineID = null;
	
	// Local executable hack
	private static String exec = null;
	
	// Static block to initialize local static configuration.
	static { 
    
    	start = System.currentTimeMillis();
    	
    	dataDir = System.getenv("DACH_DATA_DIR");

    	if (dataDir == null) { 
    		System.err.println("DACH_DATA_DIR not set!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}

    	if (!directoryExists(dataDir)) {
    		System.err.println("DACH_DATA_DIR (" + dataDir + ") not found!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}
    	
    	machineID = System.getenv("DACH_MACHINE_ID");

    	if (machineID == null) { 
    		System.err.println("DACH_MACHINE_ID not set!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}
    
    	exec = System.getenv("DACH_EXECUTABLE");

    	if (exec == null ) {
    		System.err.println("DACH_EXECUTABLE not set!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}

    	if (!fileExists(exec)) {
    		System.err.println("DACH_EXECUTABLE (" + exec + ") not found!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}

    	cpExec = System.getenv("DACH_COPY");

    	if (cpExec == null ) {
    		System.err.println("DACH_COPY not set!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}

    	if (!fileExists(cpExec)) {
    		System.err.println("DACH_COPY (" + cpExec + ") not found!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}
    	
    	tmpDir = System.getenv("DACH_TMP_DIR");
		
    	if (tmpDir == null) {  
    		System.err.println("DACH_TMP_DIR not set!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}

    	if (!directoryExists(tmpDir)) {
    		System.err.println("DACH_TMP_DIR (" + tmpDir + ")not found!");
    		// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
    		System.exit(1);
    	}
    }
    
	private static boolean fileExists(File f) { 
    	return f.exists() && f.canRead() && !f.isDirectory();
    }

    private static boolean fileExists(String f) { 
    	return fileExists(new File(f));
    }
    
    private static boolean directoryExists(File dir) { 
    	return dir.exists() && dir.canRead() && dir.isDirectory();
    }
    
    private static boolean directoryExists(String dir) {
    	return directoryExists(new File(dir));
    }
    
    private static long time() { 
    	return System.currentTimeMillis() - start;
    }
    
    private boolean prepareInputFile(Result r, String file) { 
    	
    	long start = System.currentTimeMillis();
    	
    	if (!fileExists(file)) { 
    		r.fatal("Input file " + file + " not found\n", time());
    		return false;
    	}

    	String [] cp = new String [] { cpExec, file, tmpDir };
		RunProcess p = new RunProcess(cp);
		p.run();

		int exit = p.getExitStatus();
		
		if (exit != 0) {
			r.fatal("Failed to copy file " + file + " (stdout: " + new String(p.getStdout()) 
				+ ") (stderr: " + new String(p.getStderr()) + ")\n", time());
			return false;
		}

		long end = System.currentTimeMillis();
		
		r.info("Copying " + file + " took " + (end-start) + " ms.", time());
		r.addTransferTime(end-start);
		
		return true;
    }
    
    private Result compare(Pair pair) {

    	Result r = new Result(pair, machineID);

    	r.info("Comparing '" + pair.before + "' and '" + pair.after + "'\n", time());
    	
    	String problem = pair.getProblemDir(dataDir);
    	String before = pair.getBeforePath(dataDir);
    	String after = pair.getAfterPath(dataDir);
    	
    	if (!directoryExists(problem)) { 
			r.fatal("Problem directory " + pair.getProblemDir(dataDir) + " not found!\n", time());
			return r;		
		}
    	
    	if (!prepareInputFile(r, before)) { 
    		return r;
    	}
    	
    	if (!prepareInputFile(r, after)) { 
    		return r;
    	}
    			
    	before = tmpDir + File.separator + pair.before;
    	after = tmpDir + File.separator + pair.after;
    	
		String [] command = new String [] { exec, "-w", tmpDir, before, after };
		
		RunProcess p = new RunProcess(command);
		p.run();

		int exit = p.getExitStatus();

		
		if (exit != 0) {
			r.fatal("Failed to run comparison: (stdout: " + new String(p.getStdout()) 
				+ ") (stderr: " + new String(p.getStderr()) + ")\n", time());
			
			return r;
		}

		r.error(new String(p.getStderr()), time());		
		r.setResult(new String(p.getStdout()), time());
		r.info("Completed '" + before + "' and '" + after + "' in " + r.time + " ms.", time());
		
		return r;
    }
    
    /**
     *
     * @param pairs The list of pairs to compare.
     * @return The list of comparison results.
     */
    public ArrayList<Result> compareAllPairs(ArrayList<Pair> pairs) {
        	
    	if (pairs.size() == 1) {
    		
    		Pair p = pairs.get(0);
    		p.incrementAttempts();    		
    		Result r = compare(p);
    		
    		ArrayList<Result> result = new ArrayList<Result>();
    		result.add(r);    		
    		return result;    		
        }
        
    	int mid = pairs.size() / 2;
    	
    	
    	// NOTE: Need this because Satin is way too strict about passing non-serializable 
    	// interfaces! As a result, we cannot pass a 'List', and the type returned by subList 
    	// is NOT an arrayList!
    	ArrayList<Pair> suba = new ArrayList<Pair>(pairs.subList(0, mid));
    	ArrayList<Pair> subb = new ArrayList<Pair>(pairs.subList(mid, pairs.size()));
    	
    	ArrayList<Result> resa = compareAllPairs(suba);
        ArrayList<Result> resb = compareAllPairs(subb);
        
        sync();

        // Combine the results.
        ArrayList<Result> success = new ArrayList<Result>();
        ArrayList<Pair> failed = new ArrayList<Pair>();
        
        for (Result r : resa) { 
        	if (r.failed && r.input.getAttempts() < MAX_ATTEMPTS) { 
        		failed.add(r.input);
        	} else { 
        		success.add(r);
        	}
        }
        
        for (Result r : resb) { 
        	if (r.failed && r.input.getAttempts() < MAX_ATTEMPTS) { 
        		failed.add(r.input);
        	} else { 
        		success.add(r);
        	}
        }

        while (failed.size() > 0) { 
        	
        	// TODO: how do I force these jobs to run on a different machine ????
        	
        	System.out.println("INFO(" + time() + "): Respawning " + failed.size() + " failed jobs!");
        	
        	List<Result> res = compareAllPairs(failed);            
        	sync();
        	
        	failed.clear();
        	
        	for (Result r : res) { 
            	if (r.failed && r.input.getAttempts() < MAX_ATTEMPTS) { 
            		failed.add(r.input);
            	} else { 
            		success.add(r);
            	}
            }	
        }
        
        return success;        
    }
    
    
    public ArrayList<Result> start(ArrayList<Pair> pairs) {

    	ArrayList<Result> success = new ArrayList<Result>();
        
    	while (pairs.size() > 0) { 
    		ArrayList<Result> results = compareAllPairs(pairs);
    		sync();
 
    		pairs.clear();
        
    		for (Result r : results) { 
    			if (r.failed && r.input.getAttempts() < MAX_ATTEMPTS) { 
    				pairs.add(r.input);
    			} else { 
    				success.add(r);
    			}
    		}
    		
    		if (pairs.size() > 0) { 
    			System.out.println("INFO(" + time() + "): Respawning " + pairs.size() + " failed jobs!");
		    }	
    	}
    	
        return success;    	
    }
        

}
