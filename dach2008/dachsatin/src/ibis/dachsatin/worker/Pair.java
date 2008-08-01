package ibis.dachsatin.worker;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A pair of images that should be compared.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
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
	public final String before;

	/** The local filename of the 'after' file. */
	public final String after;

	/** The size of the 'before' file. */
	public final long beforeSize;

	/** The size of the 'after' file. */
	public final long afterSize;
	
	public Set<String> replicaSites = null;
	
	public Set<String> blacklist = null;

	public int executionAttempts = 0;
	
	Pair(final String ID, final String problem, final String before, final long beforeSize, 
			final String after, final long afterSize) {
		this.ID = ID;
		this.problem = problem;
		this.before = before;
		this.after = after;
		this.beforeSize = beforeSize;
		this.afterSize = afterSize;
	}
	
	public String getProblemDir(String dataDir) {
		return dataDir + File.separator + problem; 
	}
	
	public String getBeforePath(String dataDir) {		
		return dataDir + File.separator + problem + File.separator + before;
	}
		
	public String getAfterPath(String dataDir) {
		return dataDir + File.separator + problem + File.separator + after;
	} 
	
	public void incrementAttempts() { 
		executionAttempts++;
	}
	
	public int getAttempts() { 
		return executionAttempts;
	}
	
	public void addToBlacklist(String machineID) { 
		
		if (blacklist == null) { 
			blacklist = new HashSet<String>();
		}
		
		blacklist.add(machineID);
	}

	public boolean onBlacklist(String machineID) { 
		
		if (blacklist == null) { 
			return false;
		}
		
		return blacklist.contains(machineID);		
	}

	public void addReplicaSite(String domain) { 
		
		if (replicaSites == null) { 
			replicaSites = new HashSet<String>();
		}
		
		replicaSites.add(domain);
	}

	public void addReplicaSites(Set<String> sites) { 
		
		if (replicaSites == null) { 
			replicaSites = new HashSet<String>();
		}
		
		replicaSites.addAll(sites);
	}
	
	public boolean inReplicaSite(String domain) { 
		
		if (replicaSites == null) { 
			return false;
		}
		
		return blacklist.contains(replicaSites);		
	}

}
