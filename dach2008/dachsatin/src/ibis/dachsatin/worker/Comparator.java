/**
 * 
 */
package ibis.dachsatin.worker;

import ibis.dachsatin.util.FileUtils;
import ibis.dachsatin.util.Util;
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
    
    private boolean prepareInputFile(Result r, String file) { 
    	
    	long start = System.currentTimeMillis();
    	
    	if (!FileUtils.fileExists(file)) { 
    		r.fatal("Input file " + file + " not found\n", Util.time());
    		return false;
    	}

    	String [] cp = new String [] { Util.cpExec, file, Util.tmpDir };
		RunProcess p = new RunProcess(cp);
		p.run();

		int exit = p.getExitStatus();
		
		if (exit != 0) {
			r.fatal("Failed to copy file " + file + " (stdout: " + new String(p.getStdout()) 
				+ ") (stderr: " + new String(p.getStderr()) + ")\n", Util.time());
			return false;
		}

		long end = System.currentTimeMillis();
		
		r.info("Copying " + file + " took " + (end-start) + " ms.", Util.time());
		r.addTransferTime(end-start);
		
		return true;
    }
    
    private Result compare(Pair pair) {

    	Result r = new Result(pair, Util.machineID);

    	r.info("Comparing '" + pair.before + "' and '" + pair.after + "'\n", Util.time());
    	
    	String problem = pair.getProblemDir(Util.dataDir);
    	String before = pair.getBeforePath(Util.dataDir);
    	String after = pair.getAfterPath(Util.dataDir);
    	
    	if (!FileUtils.directoryExists(problem)) { 
			r.fatal("Problem directory " + pair.getProblemDir(Util.dataDir) + " not found!\n", Util.time());
			return r;		
		}
    	
    	if (!prepareInputFile(r, before)) { 
    		return r;
    	}
    	
    	if (!prepareInputFile(r, after)) { 
    		return r;
    	}
    			
    	before = Util.tmpDir + File.separator + pair.before;
    	after = Util.tmpDir + File.separator + pair.after;
    	
		String [] command = new String [] { Util.exec, "-w", Util.tmpDir, before, after };
		
		RunProcess p = new RunProcess(command);
		p.run();

		int exit = p.getExitStatus();

		
		if (exit != 0) {
			r.fatal("Failed to run comparison: (stdout: " + new String(p.getStdout()) 
				+ ") (stderr: " + new String(p.getStderr()) + ")\n", Util.time());
			
			return r;
		}

		r.error(new String(p.getStderr()), Util.time());		
		r.setResult(new String(p.getStdout()), Util.time());
		r.info("Completed '" + before + "' and '" + after + "' in " + r.time + " ms.", Util.time());
		
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
        	if (r.failed && r.input.getAttempts() < Util.MAX_ATTEMPTS) { 
        		failed.add(r.input);
        	} else { 
        		success.add(r);
        	}
        }
        
        for (Result r : resb) { 
        	if (r.failed && r.input.getAttempts() < Util.MAX_ATTEMPTS) { 
        		failed.add(r.input);
        	} else { 
        		success.add(r);
        	}
        }

        while (failed.size() > 0) { 
        	
        	// TODO: how do I force these jobs to run on a different machine ????
        	
        	System.out.println("INFO(" + Util.time() + "): Respawning " + failed.size() + " failed jobs!");
        	
        	List<Result> res = compareAllPairs(failed);            
        	sync();
        	
        	failed.clear();
        	
        	for (Result r : res) { 
            	if (r.failed && r.input.getAttempts() < Util.MAX_ATTEMPTS) { 
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
    			if (r.failed && r.input.getAttempts() < Util.MAX_ATTEMPTS) { 
    				pairs.add(r.input);
    			} else { 
    				success.add(r);
    			}
    		}
    		
    		if (pairs.size() > 0) { 
    			System.out.println("INFO(" + Util.time() + "): Respawning " + pairs.size() + " failed jobs!");
		    }	
    	}
    	
        return success;    	
    }
        

}
