package ibis.dachsatin.deployment.util;

import java.io.File;

public class Problem {

	public final String ID;
	public final String directory;	
	
	public final String outputFile;	
	public final String doneFile;	
	
	public Problem(String ID, String name, String homeDir) { 
		this.ID = ID;
		this.directory = name;		
		
		outputFile = homeDir + File.separator + ID + ".txt";
		doneFile = homeDir + File.separator + ID + ".done";
	}
}

