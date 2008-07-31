package ibis.dachsatin.worker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Problem {

	public final String ID;
	public final String directory;
	
	private BufferedWriter out;
	
	public Problem(String ID, String name) { 
		this.ID = ID;
		this.directory = name;
	}
	
	public void writeResult(String result) throws IOException { 
		
		if (out == null) { 
			out = new BufferedWriter(new FileWriter(ID + ".txt"));
		}
		
		out.write(result);
		out.flush();
	}

	public void done() throws IOException { 
		
		if (out != null) {
			try { 
				out.flush();
				out.close();
			} catch (Exception e) {
				// ignore ?
			}
		}
		
		new File(ID + ".done").createNewFile();
	}
}
