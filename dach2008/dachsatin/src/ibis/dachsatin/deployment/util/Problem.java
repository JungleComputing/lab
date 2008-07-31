package ibis.dachsatin.deployment.util;

import java.io.File;

public class Problem {

	public final String ID;
	public final String directory;	
	public final File outputFile;
	
	public Problem(String ID, String name, File file) { 
		this.ID = ID;
		this.directory = name;		
		this.outputFile = file;
	}
	
	public boolean outputExists() { 
		
		if (outputFile == null) {
			return false;
		}
		
		return outputFile.exists();
	}
}

