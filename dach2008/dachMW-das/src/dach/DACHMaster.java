package dach;

import ibis.masterworker.Job;
import ibis.masterworker.Master;
import ibis.masterworker.Result;
import ibis.masterworker.ResultProcessor;
import ibis.masterworker.deployment.Cluster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class DACHMaster implements ResultProcessor {
	
	private static final Logger logger = Logger.getLogger("dach.master");

	private static DachAPI api = new DachAPI();
	
	// These static fields may be set by the command line options.
	private static String homeDir = "/home/dach004";
	private static String dataDir = "/data/local/dach004/dfs";
	private static String tmpDir = "/data/local/dach004";
	
	private static String multiCoreExec = "/home/dach004/bin/dach.sh";
	private static String singleCoreExec = "/home/dach/finder/dach.sh";
	
	private static String cpExec;
	private static String locationExec = "/data/local/gfarm_v2/bin/gfwhere";
	
	private static Problem problem;
	
	private static boolean advancedQueue = false;
	private static boolean concurrent = false;
	private static boolean allowRemote = true;
	
	private static boolean dryRun = false;
	private static boolean dfs = false;
	
	private static boolean workersAreDFSServer = false; 
	
	private static int duplicate = 1;
	
	private int totalJobs = 0;
	private int jobsDone = 0;
	private int jobsFailed = 0;
	
	private HashMap<String, FileOutputStream> writers = new HashMap<String, FileOutputStream>();
	
	private LinkedList<Cluster> dataClusters;
	private LinkedList<Cluster> workerClusters;
	
	private Master master;
	
	public DACHMaster(LinkedList<Cluster> workerClusters, LinkedList<Cluster> dataClusters) throws Exception { 

		this.dataClusters = dataClusters;
		this.workerClusters = workerClusters;
		
		LinkedList<String> workerOptions = new LinkedList<String>();
		
		workerOptions.add("-exec");
		
		if (concurrent) { 
			workerOptions.add(singleCoreExec);	
			workerOptions.add("-concurrent");
		} else { 
			workerOptions.add(multiCoreExec);
		}
		
		workerOptions.add("-tmpDir");
		workerOptions.add(tmpDir);
		
		workerOptions.add("-dataDir");
		workerOptions.add(dataDir);
		
		if (cpExec != null) { 
			workerOptions.add("-copy");
			workerOptions.add(cpExec);
		}

		if (dfs) { 
			workerOptions.add("-dfs");
		} 
		
		if (dryRun) { 
			workerOptions.add("-dryRun");			
		}
		
		master = new Master(this, workerClusters, workerOptions, homeDir, 
				dataClusters, dataDir, allowRemote, advancedQueue, workersAreDFSServer);
	}
	
	public boolean failed(Job job, Result result) {

		DACHJob j = (DACHJob) job;
		DACHResult r = (DACHResult) result;
		
		logger.warn("Job " + j.beforeInfo.name + " ... " + j.afterInfo.name + " failed: " + r.stderr);
		
		synchronized (this) {
			jobsFailed++;
			notifyAll();
		}
		
		return true;
	}

	public void success(Job job, Result result) {
		
		DACHJob j = (DACHJob) job;
		DACHResult r = (DACHResult) result;
		
		appendOutput(j, r);
		
		synchronized (this) {
			jobsDone++;
			notifyAll();
		}
	}
	
	public boolean needMoreJobs() {
		return false;
	}
	
	private synchronized void appendOutput(DACHJob job, DACHResult r) { 
		
		String ID = job.problemID;
		
		FileOutputStream b = writers.get(ID);
		
		logger.info("Writing output for Job " + ID + " (" + r.result.length + ")");
		
		if (b == null) { 
			logger.error("Failed to find writer for ID " + ID);
		} else { 
			try { 
				b.write(r.result);
				b.flush();
			} catch (Exception e) {
				logger.error("Failed to write to file " + ID, e);
			}
		}
		
		// HACK -- save all seperatedly
/*		
		String tmp = null;
		
		try { 
			File f = null;
		
			int count = 0;
			
			do { 
				tmp = homeDir + File.separator + job.problemID + "-JOB" 
					+ r.ID + "-" + r.getNode() + ".out" + count++;
			
				logger.info("Creating dump file: " + tmp);
				
				f = new File(tmp);
			} while (f.exists());
			
			FileOutputStream writer = new FileOutputStream(f);
			writer.write(r.result);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			logger.error("  " + ID, e);
		}
*/
	}
	
	private void createWriter(Problem p) {
		
		String ID = p.ID;
		
		try { 
			FileOutputStream b = new FileOutputStream(p.outputFile);
			writers.put(ID, b);
		} catch (IOException e) {
			logger.fatal("Failed to create output file " + p.outputFile, e);
			System.exit(1);
		}
	}
	
	private void closeWriters() { 
		
		for (FileOutputStream b : writers.values()) { 
			try {
				b.flush();
				b.close();
			} catch (IOException e) {
				logger.error("Failed to close file", e);
			}
		}
	}
	
	private void createWriters()  { 
		
		logger.info("Creating output writers");
		
		//for (Problem p : problems) { 
			createWriter(problem);		
		//}		
	}
	
	private List<DACHJob> getJobs() { 
		
		JobProducer producer = null; 
		
		if (dfs) {
			producer = new DFSJobs(new File(dataDir), problem, locationExec, true);
		} else { 
			if (dataClusters.size() == 0 && !workersAreDFSServer) { 
				logger.fatal("No data clusters provided!");
				System.exit(1);
			}
			
			LinkedList<String> servers = new LinkedList<String>();
			
			if (dataClusters != null && dataClusters.size() > 0) { 
				for (Cluster c : dataClusters) { 
					servers.add(c.master);
				}
			}

			if (workersAreDFSServer && workerClusters != null) { 
				for (Cluster c : workerClusters) {
					servers.addAll(c.nodes);
				}
			}
			
			logger.info("Using DFS servers: " + servers);
			
			try {
				producer = new DistributedJobs(servers, problem, true);
			} catch (Exception e) {
				logger.fatal("Failed to create job producer!", e);
			}			
		}
		
		if (duplicate <= 0) {
			logger.warn("Job duplication has illegal value: " + duplicate + " (reset to 1)");
			duplicate = 1;
		} else { 
			logger.info("Job duplication set to: " + duplicate);
		}
		
    	List<DACHJob> jobs = null;
    	
    	try { 
    		jobs = producer.produceJobs(false, duplicate);
    	} catch (IOException e) {
    		logger.fatal("Failed to load all pairs from directory " + dataDir, e);
    		System.exit(1);
    	}
    	
    	if (jobs.size() == 0) { 
    		logger.fatal("FATAL: No pairs found in directory " + dataDir);
    		System.exit(1);
    	}
		
    	return jobs;
	}
	
	@SuppressWarnings("unchecked")
	public void start() { 
		
		List<DACHJob> jobs = getJobs();

		totalJobs = jobs.size();
		
		logger.info("Created " + totalJobs + " jobs");

		for (DACHJob j : jobs) { 
			logger.info("   " + j.toString());
		}
		
		createWriters();
		
		logger.info("Adding jobs to queue");
		
		master.addJobs(jobs);
		
		logger.info("Waiting for results");
		
		synchronized (this) {
			while (jobsDone < totalJobs) { 
				try { 
					wait(5000);
				} catch (InterruptedException e) {
					// ignore
				}
				
				int queueLen = master.getQueueLength();
				int pending = master.getPending();
				int workers = master.getActiveWorkers();
				int totalWorkers = master.getTotalWorkers();
				
				logger.info("Current status (Q/P/D/E/T): " 
						+ queueLen + " / " 
						+ pending + " / " 
						+ jobsDone + " / " 
						+ jobsFailed + " / " 
						+ totalJobs 
						+ " Active workers: " + workers + " / " + totalWorkers);
			}
		}
	
		closeWriters();
		
		api.returnResults(problem);
		
		master.printStatistics();
		master.done();
		master.exit(60000);	
		
		System.exit(0);
	}
	
	private static boolean isSet(String value) { 
		return value != null && value.trim().length() > 0;
	}
	
	private static LinkedList<Problem> getProblems(List<String> problemNames) { 
		
		try {
			return api.getProblems(problemNames, homeDir);
		} catch (Exception e) {
			logger.error("Failed to retrieve problems!", e);
			System.exit(1);
		}
		
		// stupid compiler ;) 
		return null;
	}
	
	
	private static void parseClusters(LinkedList<Cluster> clusters, String arg, String suffix) { 
	
		StringTokenizer tok = new StringTokenizer(arg, ",");
		
		while (tok.hasMoreElements()) { 
			String name = tok.nextToken();
		
			String file = "./clusters/" + name.trim() + suffix; 
			
			try {
				clusters.add(Cluster.read(file));
			} catch (Exception e) {
				System.err.println("Failed to read cluster file " + file);
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
	}
	
	public static void main(String [] args) { 
		
		List<String> problemNames = new LinkedList<String>();
		
		LinkedList<Cluster> dataClusters = new LinkedList<Cluster>();
		LinkedList<Cluster> workerClusters = new LinkedList<Cluster>();
		
		logger.warn("DACHMaster parsing command line");
		
		for (int i=0;i<args.length;i++) { 
    		if (args[i].equals("-dryRun")) { 
    			dryRun = true;
    		} else if (args[i].equals("-dataDir") && i != args.length-1) { 
    			dataDir = args[++i];
    		} else if (args[i].equals("-duplicate") && i != args.length-1) { 
    			duplicate = Integer.parseInt(args[++i]);    			
    		} else if (args[i].equals("-dach_api") && i != args.length-1) { 
    			String tmp = args[++i];
    			api = new DachAPI(tmp, tmp);    	
    		} else if (args[i].equals("-tmpDir") && i != args.length-1) { 
    			tmpDir = args[++i];    	
    		} else if (args[i].equals("-homeDir") && i != args.length-1) { 
    			homeDir = args[++i];    	
    		} else if (args[i].equals("-copy") && i != args.length-1) { 
    			cpExec = args[++i];
    		} else if (args[i].equals("-singleCoreExec") && i != args.length-1) { 
    			singleCoreExec = args[++i];
    		} else if (args[i].equals("-multiCoreExec") && i != args.length-1) { 
    			multiCoreExec = args[++i];		
    		} else if (args[i].equals("-concurrent")) { 
    			concurrent = true;
    		} else if (args[i].equals("-workerDFS")) { 
    			workersAreDFSServer = true;		
    		} else if (args[i].equals("-onlyLocal")) { 
    			allowRemote = false;
    		} else if (args[i].equals("-advancedQueue")) { 
        		advancedQueue = true;	
    		} else if (args[i].equals("-workers") && i != args.length - 1) {
    			parseClusters(workerClusters, args[++i], ".cluster");
    		} else if (args[i].equals("-datasites") && i != args.length - 1) {
    			parseClusters(dataClusters, args[++i], ".cluster");
    		} else if (args[i].equals("-problem") && i != args.length - 1) {
				problemNames.add(args[++i]);
    		} else { 
    			System.err.println("Unknown or incomplete option: " + args[i]);
    			System.exit(0);
    		}
    	}
		
		if (!isSet(homeDir)) { 
			logger.fatal("homeDir not set");
			System.exit(1);
		}
	
		if (!isSet(tmpDir)) { 
			logger.fatal("tmpDir set");
			System.exit(1);
		}
	
		if (!isSet(singleCoreExec)) { 
			logger.fatal("DACH singleCoreExec not set");
			System.exit(1);
		}

		if (!isSet(multiCoreExec)) { 
			logger.fatal("DACH multiCoreExec not set");
			System.exit(1);
		}
		
		if (problemNames.size() == 0) { 
			logger.fatal("No problems specified");
			System.exit(1);
		}
	
		File output = new File(homeDir + File.separator + "output");
		output.mkdir();
		
		if (!output.exists()) { 
			logger.fatal("Failed to create output directory!");
			System.exit(1);
		}
		
		LinkedList<Problem> tmp = getProblems(problemNames);
		
		if (tmp.size() != 1) { 
			System.err.println("EEP: current implementation can only handle 1 problem at a time!");
			System.exit(1);
		}
		
		problem = getProblems(problemNames).removeFirst();
		
		logger.warn("DACHMaster starting!");
		
		try {
			new DACHMaster(workerClusters, dataClusters).start();
		} catch (Exception e) {
			logger.fatal("Failed to start DACHMaster", e);
			System.exit(1);
		}
	
		logger.warn("DACHMaster exit!");
	}
	
}
