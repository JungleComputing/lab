package ibis.dachsatin;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
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

	public void done() { 
		
		if (out != null) {
			try { 
				out.flush();
				out.close();
			} catch (Exception e) {
				// ignore ?
			}
		}
	}
}
