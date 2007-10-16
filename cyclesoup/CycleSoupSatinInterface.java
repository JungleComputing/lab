import ibis.satin.Spawnable;

// File: $Id: CycleSoupSatinInterface.java,v 1.4 2005/05/13 20:12:27 reeuwijk Exp $

/**
 * @author Kees van Reeuwijk.
 *
 * Methods in this marker interface are Satin divide-and-conquer jobs.
 */
public interface CycleSoupSatinInterface extends Spawnable {
    /**
     * Applies the given computational script to the given list of jobs.
     * This is done by recursively dividing the list into smaller pieces until
     * a reasonable limit is reached.
     * @param script The script to apply.
     * @param jobs The list of job names to execute.
     * @return The list of jobs that could not be executed.
     */
    public String [] applyScript( byte script[], String jobs[] );
}
