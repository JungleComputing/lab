package dach.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class JobInfo {

	final int number;
	
	final String file1;
	final String file2;
	
	final long size1;
	final long size2;
	
	public JobInfo(final int number, final String file1, final String file2, final long size1, final long size2) {
		super();
		this.number = number;
		this.file1 = file1;
		this.file2 = file2;
		this.size1 = size1;
		this.size2 = size2;
	}	
	
	private static JobInfo parseLine(String line) { 
		
		StringTokenizer tok = new StringTokenizer(line);
		
		if (tok.countTokens() != 5) { 
			System.err.println("Failed to parse line: " + line);
			return null;
		}
		
		int number = Integer.parseInt(tok.nextToken());
		String file1 = tok.nextToken();
		long size1 = Long.parseLong(tok.nextToken());
		String file2 = tok.nextToken();
		long size2 = Long.parseLong(tok.nextToken());
		
		return new JobInfo(number, file1, file2, size1, size2);
	}
	
	public static JobInfo [] parseJobInfo(File f) throws IOException { 
		
		LinkedList<JobInfo> tmp = new LinkedList<JobInfo>();
		
		BufferedReader r = new BufferedReader(new FileReader(f));
		
		String line = r.readLine();
		
		while (line != null) { 
		
			JobInfo j = parseLine(line);
			
			if (j == null) {
				System.out.println("Failed to parse: " + line);
			} else { 
				tmp.add(j);
			}
			
			line = r.readLine();
		}

		return tmp.toArray(new JobInfo[tmp.size()]);
	}
	
}
