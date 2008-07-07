/**
 * 
 */
package ibis.dachsatin;

import java.util.Arrays;

import ibis.satin.SatinObject;
import ibis.util.Pair;
import ibis.util.RunProcess;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk
 *
 */
public class Comparator extends SatinObject implements ComparatorSatinInterface {
   
	/** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;

    private String compare(Pair pair) {

		String res = null;

		String comparatorExecutable = System.getenv("DACHCOMPARATOR");

		if( comparatorExecutable == null ) {
			comparatorExecutable = "/usr/bin/diff";
		}

		System.out.println("Comparing '" + pair.before + "' and '" + pair.after + "'");

		String command[] = {
				comparatorExecutable,
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
			
			System.err.println("Comparison command '" + cmd + "' failed:");
			System.err.println("stdout: " + new String(p.getStdout()));
			System.err.println("stderr: " + new String(p.getStderr()));
			
			return null;
		}
		
		res = new String(p.getStdout());
		
		return res;
	}
    
    /**
     *
     * @param pairs The list of pairs to compare.
     * @return The list of comparison results.
     */
    public Result compareAllPairs(Pair [] pairs) {
        
    	if (pairs.length == 1) {    		
    		long start = System.currentTimeMillis();    		
            String output = compare(pairs[0]);
            long end = System.currentTimeMillis();            
            return new Result(output, end-start);
        }
        
    	int mid = pairs.length / 2;
    	   	
    	Result resa = compareAllPairs(Arrays.copyOfRange(pairs, 0, mid));
        Result resb = compareAllPairs(Arrays.copyOfRange(pairs, mid, pairs.length));
        
        sync();
        
        return new Result(resa, resb);        
    }
    
    
    public Result start(Pair [] pairs) {
        Result r = compareAllPairs(pairs);
        sync();
        return r;    	
    }
        

}
