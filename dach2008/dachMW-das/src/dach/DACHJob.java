package dach;

import ibis.dfs.FileInfo;
import ibis.masterworker.Job;

import java.io.File;
import java.util.Set;

public class DACHJob extends Job {

	private static final long serialVersionUID = 309719814063686446L;

	/** The ID of the problem set */
	public final String problemID;
	
	/** The name (subdirectory) of the problem set */
	public final String problemDir;
	
	public final FileInfo beforeInfo;
	public final FileInfo afterInfo;
	
	public int executionAttempts = 0;
	
	protected DACHJob(String problemID, String problemDir, 
			FileInfo beforeInfo, FileInfo afterInfo, Set<String> preferredLocations) {
		
		super(preferredLocations);
		
		this.problemID = problemID;
		this.problemDir = problemDir;
		this.beforeInfo = beforeInfo;
		this.afterInfo = afterInfo;	
	}
	
	public String getProblemDir(String dataDir) {
		return dataDir + File.separator + problemDir; 
	}

	public String getBefore() {		
		return problemDir + File.separator + beforeInfo.name;
	}

	public String getAfter() {		
		return problemDir + File.separator + afterInfo.name;
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
	
	public int score(String host) { 
		return beforeInfo.score(host) + afterInfo.score(host);
	}
	
	public long getJobSize() { 
		return beforeInfo.size + afterInfo.size;
	}
	
	
	public String toString() { 
		return "Job " + ID + " " + problemID + ", " 
			+ beforeInfo.name + " " + beforeInfo.size + " " 
			+ afterInfo.name + " " + afterInfo.size + " "
			+ preferredLocations + " " + beforeInfo.replicaHosts + ")";  
	}

}
