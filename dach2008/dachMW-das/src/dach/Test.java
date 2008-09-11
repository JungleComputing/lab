package dach;

import ibis.dfs.FileInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Test {

	private static String dataDir;
	private static String tmpDir;
	
	private static List<Problem> problems = new LinkedList<Problem>();
	private static String scpExec = "/usr/bin/scp";
	
	/*
	private static boolean remoteCopy(DACHJob job, DACHResult r, FileInfo info) {

		boolean done = false;
		boolean copied = false;
		
		long start = System.currentTimeMillis();
		
		String file = job.getProblemDir(dataDir) + File.separator + info.name;
		
		Set<String> usedReplicas = new HashSet<String>(); 
		Set<String> usedSites = new HashSet<String>(); 
		
		while (!done) { 
			
			String host = info.selectReplica(usedReplicas, usedSites);
		
			if (host == null) { 
				done = true;
			} else { 
				StringBuilder out = new StringBuilder();
				StringBuilder err = new StringBuilder();

				int exit = FileUtils.run(new String [] { scpExec, "-oStrictHostKeyChecking=no", host + ":" + file, tmpDir }, out, err); 

				if (exit != 0) {
					System.err.println("Failed to remotely copy file " + file + " (stdout: " + out + ") (stderr: " + err + ")\n");
				} else { 
					done = true;
					copied = true;
				}
			} 		
		}
		
		if (!copied) { 
			System.err.println("Failed to find working replica for " + file);
			return false;
		}
				
		long end = System.currentTimeMillis();
		
		System.out.println("Remote copying " + file + " took " + (end-start) + " ms.");
		
		return true;		
	}
	*/
	
	public static void main(String [] args) { 
		
		String server = args[0];
		String pool = args[1];
		
		String datasites = args[2];
		
		String ID = args[3];
		String problem = args[4];
		
		problems.add(new Problem(ID, problem, System.getProperty("user.dir")));
		
		List<Set<String>> sites = null;
		
		try {
			sites = MiscUtils.readSites(datasites);
		} catch (Exception e) {
			System.err.println("Failed to read data sites file " + datasites);
			e.printStackTrace(System.err);
			System.exit(1);			
		}
		
		JobProducer producer = null;

		try {
	//		producer = new DistributedJobs(server, null, pool, dataDir, sites, problems, true);
		} catch (Exception e) {
			System.err.println("Failed to create job producer");
			e.printStackTrace(System.err);
			System.exit(1);			
		}
		
		System.out.println("Producing jobs!");
		
		List<DACHJob> jobs = null;

		try {
			jobs = producer.produceJobs(false, 1);

			System.out.println("Produced " + jobs.size() + " jobs");
			
			for (DACHJob j : jobs) { 
				System.out.println("    " + j.toString());					
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed!");
			e.printStackTrace();
		}
		
		/*
		System.out.println("Looking for file " + file);
		
		for (DACHJob d : jobs) { 
			if (d.beforeInfo.name.equals(file)) { 
				
				System.out.println("Copying file: " + file);
				remoteCopy(d, new DACHResult(d.ID, "aap", "noot"), d.beforeInfo);
				
			} else if (d.afterInfo.name.equals(file)) { 
				
				System.out.println("Copying file: " + file);
				remoteCopy(d, new DACHResult(d.ID, "aap", "noot"), d.afterInfo);				
			}
		}
		*/
		
	}
	
	
}
