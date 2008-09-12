package dach.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class FilterJobsDone {

	private static long startTime = -1;
	
	private static void parseLine(String line) { 
		
		StringTokenizer tok = new StringTokenizer(line);
		
		long time = getTime(tok.nextToken());
		
		if (startTime == -1) { 
			startTime = time;
			time = 0;
		} else { 
			time = time-startTime;
		}
	
		skipTokens(tok, 7);
		
		String queued = tok.nextToken();
		
		skipTokens(tok, 1);
		
		String pending = tok.nextToken();
		
		skipTokens(tok, 1);
			
		String done = tok.nextToken();
		
		skipTokens(tok, 6);
		
		String workers = tok.nextToken();
			
		System.out.println(time + " " + done + " " + pending + " " + queued + " " + workers);
	}
	
	private static void skipTokens(StringTokenizer tok, int num) { 
		for (int i=0;i<num;i++) { 
			tok.nextToken();
		}
	}
	
	private static long getTime(String time) { 

		StringTokenizer tok = new StringTokenizer(time, ":");
		
		if (tok.countTokens() != 3) { 
			System.out.println("Cannot parse time! " + time);
			return 0;
		}
		
		long hour = Integer.parseInt(tok.nextToken());
		long minute = Integer.parseInt(tok.nextToken());
		long second = Integer.parseInt(tok.nextToken());
		
		return second + 60*minute + 60*60*hour; 
	}
	
	public static void main(String [] args) { 
		
		// Parse output file with lines looking like:
		//
		// 22:24:30 INFO  [main] dach.master - Current status (Q/P/D/E/T): 1701 / 510 / 1629 / 0 / 3840 Active workers: 510 / 512
		// 
		// Convert the time to seconds from start and put each of the number in a seperate column
		
		try {
			BufferedReader b = new BufferedReader(new FileReader(args[0]));
	
			String line = b.readLine();
			
			while (line != null) { 
	
				parseLine(line);
				
				line = b.readLine();	
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
