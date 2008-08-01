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

    	r.info("Comparing '" + pair.beforeInfo.name + "' and '" + pair.afterInfo.name + "'\n", Util.time());
    	
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
    			
    	before = Util.tmpDir + File.separator + pair.beforeInfo.name;
    	after = Util.tmpDir + File.separator + pair.afterInfo.name;
    	
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
   
    private Pair selectJob(ArrayList<Pair> pairs) { 
    
    	// When selecting a job, we prefer then is this order:
    	//
    	//   - two files on this node
    	//   - one file on this node, and one in this site
    	//   - one file on this node, one in a remote site
    	//   - two files on this site
    	//   - one file in this site, one in a remote site
    	//   - two files in a remote site
    	//
    	// For the first four we prefer the largest file size, for the 
    	// last two we prefer the smallest file size.
      	
    	System.out.println("Selecting job for host \"" + Util.host + "\"");
    	
    	if (pairs.size() == 1) { 
    		// No choice
    		return pairs.remove(0);
    	}
    
    	Pair p = pairs.get(0);
    	int index = 0;
    	int score = p.scoreLocation(Util.host);
    	
    	for (int i=1;i<pairs.size();i++) { 
    		
    		Pair tmp = pairs.get(i);
			int tmpScore = tmp.scoreLocation(Util.host);
    	
			if (tmpScore > score) { 
				p = tmp;
				index = i;
				score = tmpScore;
			} else if (tmpScore == score) {
				
				if (tmpScore >= 4) { 
					// Prefer the biggest files
					if (tmp.beforeInfo.size > p.beforeInfo.size) {
						p = tmp;
						index = i;
						score = tmpScore;
					} 
				} else { 
					// Prefer the smallest files
					if (tmp.beforeInfo.size < p.beforeInfo.size) {
						p = tmp;
						index = i;
						score = tmpScore;
					}
				}
			}
    	}
		
    	System.out.println("Selected job with score " + score + " : " 
    			+ p.beforeInfo.name + " - " + p.afterInfo.name + " sizes: " 
    			+ p.beforeInfo.size + " " + p.afterInfo.size);
    	
    	pairs.remove(index);
    	return p;
    }
    
    private void sortResults(Result local, ArrayList<Result> resa, ArrayList<Result> resb, 
    		ArrayList<Result> success, ArrayList<Pair> failed) { 
    	
    	// Add the local result to the right list
        if (local.failed && local.input.getAttempts() < Util.MAX_ATTEMPTS) { 
			failed.add(local.input);
		} else { 
			success.add(local);
		}
        
        // Add the results from the first spawned job (if necessary)
        if (resa != null) { 	
        	for (Result r : resa) { 
        		if (r.failed && r.input.getAttempts() < Util.MAX_ATTEMPTS) { 
        			failed.add(r.input);
        		} else { 
        			success.add(r);
        		}
        	}
        }
        
        // Add the results from the first spawned job (if necessary)
        if (resb != null) {
        	for (Result r : resb) { 
        		if (r.failed && r.input.getAttempts() < Util.MAX_ATTEMPTS) { 
        			failed.add(r.input);
        		} else { 
        			success.add(r);
        		}
        	}
        }
    }
    
    /**
     *
     * @param pairs The list of pairs to compare.
     * @return The list of comparison results.
     */
    public ArrayList<Result> compareAllPairs(ArrayList<Pair> pairs) {
        	
    	// We pick the job that suits us best, typically the largest job we can 
    	// find that is replicated locally. We then spawn the rest, compute our job,
    	// and sync for the results. This way, we actually have some influence on what 
    	// job is assigned to us.
    		
    	Pair local = selectJob(pairs);
    	
    	ArrayList<Result> resa = null;
    	ArrayList<Result> resb = null;
    	
    	int size = pairs.size();
    	
    	if (size == 1) { 
    		// Will be spawned
    		resa = compareAllPairs(pairs);
    	} else if (size >= 2) { 
    		// There is something left to spawn!
    		int mid = pairs.size() / 2;
        	
        	// NOTE: Need this because Satin is way too strict about passing non-serializable 
        	// interfaces! As a result, we cannot pass a 'List', and the type returned by subList 
        	// is NOT an arrayList!
        	ArrayList<Pair> suba = new ArrayList<Pair>(pairs.subList(0, mid));
        	ArrayList<Pair> subb = new ArrayList<Pair>(pairs.subList(mid, pairs.size()));
        	
        	// Both will be spawned
        	resa = compareAllPairs(suba);
            resb = compareAllPairs(subb);
    	}
    	
    	// Perform local computation here!
    	local.incrementAttempts();    		
    	Result localResult = compare(local);
    		
    	// Wait for the results to come in from the spawned jobs
    	sync();

        // Combine the results.
        ArrayList<Result> success = new ArrayList<Result>();
        ArrayList<Pair> failed = new ArrayList<Pair>();

        sortResults(localResult, resa, resb, success, failed);
        
        // Respawn any failed jobs ?
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
