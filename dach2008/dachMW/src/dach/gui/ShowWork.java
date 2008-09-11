package dach.gui;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class ShowWork {

	public static void main(String [] args) { 
		
		if (args.length < 5) { 
			System.err.println("Usage: OutputParser.main time dir prefix postfix cluster+");
			System.exit(1);
		}
		
		OutputParser p = new OutputParser();
		
		long startTime = p.getTime(args[0]); 
		File dir = new File(args[1]);
		String prefix = args[2];
		String postfix = args[3];
		
		String [] clusters = new String[args.length-4];
		System.arraycopy(args, 4, clusters, 0, args.length-4);
		
		
		
		try {
			LinkedList<ClusterStatistics> s = p.parseAll(dir, startTime, prefix, clusters, postfix);

			System.out.println("Got " + s.size() + " clusters");
		
			long endTime = 0;
			long jobEndTime = 0;
			
			for (ClusterStatistics c : s) { 
				System.out.println("  " + c.name + " " + c.nodes.size() + " nodes " + c.getJobs() + " jobs");
			
				jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
				endTime = Math.max(endTime, c.getLatestEndTime());
			}
		
			System.out.println("total time = " + endTime);
			System.out.println("total job time = " + jobEndTime);
			
			new WorkFrame(s);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
