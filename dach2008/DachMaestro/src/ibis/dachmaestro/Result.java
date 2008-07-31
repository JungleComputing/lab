package ibis.dachmaestro;

import java.io.Serializable;

/**
 * The result of a comparison.
 * 
 * @author Kees van Reeuwijk
 *
 */
class Result implements Serializable
{
    /** The output of the comparison process. It may be null if there was an error. */
    final String result;
    
    /** The time in ns it took to do the comparison. */
    final long computeTime;

    /** If not null, the error that caused this result to be invalid. */
    final String error;

    /**
     * @param result The result of the comparison.
     * @param computeTime The time in ns it took to do the comparison.
     * @param error <code>null</code> if all went well, else an error message.
     */
    Result(String result, long computeTime, String error) {
	this.result = result;
	this.computeTime = computeTime;
	this.error = error;
    }
}
