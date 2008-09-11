package dach;

import java.io.File;

public class Problem {

	public final String ID;
	public final String directory;	
	
	public final String outputFile;	
	
	public Problem(String ID, String name, String homeDir) { 
		this.ID = ID;
		this.directory = name;		
		
		outputFile = homeDir + File.separator + ID + ".txt";
	}
}

