/**
 * 
 */
package ibis.dachsatin;

import ibis.satin.SatinObject;
import ibis.util.Pair;
import ibis.util.RunProcess;

import java.util.Arrays;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Comparator extends SatinObject implements ComparatorSatinInterface {
   
	/** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;

    // Timing hack
    private static long start = -1;
    
    private String compare(Pair pair, String exec) {

		long time = System.currentTimeMillis() - start;
		
		System.out.println(time + ": Comparing '" + pair.before + "' and '" + pair.after + "'");
		
		if (exec == null) { 		
			exec = System.getenv("DACHCOMPARATOR");
			
			if (exec == null ) {
				return "No command found!";
			}
		}
		
		String command [] = {
				exec,
				pair.before.getAbsolutePath(),
				pair.after.getAbsolutePath()
		};

		RunProcess p = new RunProcess(command);
		p.run();

		int exit = p.getExitStatus();

		if (exit != 0) {
			String cmd = "";
			
			for (String c: command) {
				if (!cmd.isEmpty()) {
					cmd += ' ';
				}
				cmd += c;
			}

			long time2 = System.currentTimeMillis() - start;
			
			System.out.println(time2 + ": Failed '" + pair.before + "' and '" + pair.after + "' in " + (time2-time) + " ms. " +
					"stdout: " + new String(p.getStdout()) + " stderr: " + new String(p.getStderr()));
			
			return "";
		}

		long time2 = System.currentTimeMillis() - start;
		
		System.out.println(time2 + ": Completed '" + pair.before + "' and '" + pair.after + "' in " + (time2-time) + " ms.");
		
		return new String(p.getStdout());
	}
    
    /**
     *
     * @param pairs The list of pairs to compare.
     * @return The list of comparison results.
     */
    public Result compareAllPairs(Pair [] pairs, String command) {
        
    	if (start == -1) { 
    		start = System.currentTimeMillis();
    	}
    	
    	if (pairs.length == 1) {    		
    		long start = System.currentTimeMillis();    		
            String output = compare(pairs[0], command);
            long end = System.currentTimeMillis();            
            return new Result(pairs, output, end-start);
        }
        
    	int mid = pairs.length / 2;
    	   	
    	Result resa = compareAllPairs(Arrays.copyOfRange(pairs, 0, mid), command);
        Result resb = compareAllPairs(Arrays.copyOfRange(pairs, mid, pairs.length), command);
        
        sync();
        
        return new Result(resa, resb);        
    }
    
    
    public Result start(Pair [] pairs, String command) {
        Result r = compareAllPairs(pairs, command);
        sync();
        return r;    	
    }
        

}
