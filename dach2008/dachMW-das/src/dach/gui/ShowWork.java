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
		
		OutputParser p = new OutputParser(args[0], false);
		
		long startTime = p.getTime(args[0]); 
		long endTime = p.getTime(args[1]); 
		
		File dir = new File(args[2]);
		String prefix = args[3];
		String postfix = args[4];
		
		String [] clusters = new String[args.length-5];
		System.arraycopy(args, 5, clusters, 0, args.length-5);				
		
		try {
			LinkedList<ClusterStatistics> s = p.parseAll(dir, startTime, prefix, clusters, postfix);

			System.out.println("Got " + s.size() + " clusters");
		
			long nodeEndTime = 0;
			long jobEndTime = 0;
			
			for (ClusterStatistics c : s) { 
				System.out.println("  " + c.name + " " + c.nodes.size() + " nodes " + c.getJobs() + " jobs");
			
				jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
				nodeEndTime = Math.max(nodeEndTime, c.getLatestEndTime());
			}
		
			System.out.println("total time = " + nodeEndTime);
			System.out.println("total job time = " + jobEndTime);
			
			System.out.println("total work time = " + p.totalWorkTime);
			System.out.println("total compute time = " + p.totalComputeTime);
			System.out.println("total transfer time = " + (p.totalWorkTime - p.totalComputeTime));

			new WorkFrame(s, endTime-startTime);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
