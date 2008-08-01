package ibis.dachsatin.worker;

import java.io.File;
import java.io.Serializable;

/**
 * A pair of images that should be compared.
 * 
 * @author Jason Maassen, Kees van Reeuwijk
 *
 */
public class Pair implements Serializable {

	/** Contractual obligation. */
	private static final long serialVersionUID = -3325234510023855038L;

	/** The ID of the problem set */
	public final String ID;
	
	/** The name (subdirectory) of the problem set */
	public final String problem;
	
	/** The local filename of the 'before' file. */
//	public final String before;

	/** The local filename of the 'after' file. */
//	public final String after;

	/** The size of the 'before' file. */
//	public final long beforeSize;

	/** The size of the 'after' file. */
//	public final long afterSize;
	
	enum When { 
		BEFORE,
		AFTER;
	}
	
	public final FileInfo before;
	public final FileInfo after;
	
	public int executionAttempts = 0;
	
	Pair(final String ID, final String problem, final String before, final long beforeSize, 
			final String after, final long afterSize) {
		this.ID = ID;
		this.problem = problem;
		this.before = new FileInfo(before, beforeSize);
		this.after = new FileInfo(after, afterSize);
	}
	
	public String getProblemDir(String dataDir) {
		return dataDir + File.separator + problem; 
	}

	public String getBefore() {		
		return problem + File.separator + before.name;
	}

	public String getAfter() {		
		return problem + File.separator + after.name;
	}
	
	public String getBeforePath(String dataDir) {		
		return dataDir + File.separator + getBefore();
	}
		
	public String getAfterPath(String dataDir) {
		return dataDir + File.separator + getAfter();
	} 
	
	public void incrementAttempts() { 
		executionAttempts++;
	}
	
	public int getAttempts() { 
		return executionAttempts;
	}	
	
	public int scoreLocation(String host) { 
		return before.score(host) + after.score(host);
	}
	
}
