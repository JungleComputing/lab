package dach;

import ibis.dfs.DFSClient;
import ibis.dfs.FileInfo;
import ibis.masterworker.Job;
import ibis.masterworker.JobProcessor;
import ibis.masterworker.Result;
import ibis.masterworker.Worker;
import ibis.util.RunProcess;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Logger;

public class DACHWorker implements JobProcessor {
	
	private static final Logger logger = Logger.getLogger("dach.worker");
	
	// These must be set by command line parameters
	private static String serverAddress;
	private static String pool;
	private static String location;
	private static String node;
	private static String masterID;
	
	private static String dataDir;
	private static String tmpDir;
	private static String exec;
	
	public static boolean preferSmall = false;	
	public static boolean dfs = false;
	public static int concurrentWorkers = 1;
	
	public static boolean startDFSServer = false;
//	public static String dataDir = null;
		
	// These may be set by command line parameters
	private static String hubAddresses;

	// Time that we are willing to wait for a Job reply (-1 means use default) 
	private static long timeout = -1;
	
	// Do not actually execute the job 
	private static boolean dryRun = false;
	
	// Local copy operation executable
	public static String cpExec = "/bin/cp";

	// Local copy operation executable
	public static String scpExec = "/usr/bin/scp";
	
	private Worker worker;
	
	private final long startTime;
	
	private DACHJob currentJob = null;
	
	private LoadMonitor load; 
	
	class LoadMonitor extends Thread { 
		
		private final long sleep;
		
		public LoadMonitor(long sleep) {
			this.setDaemon(true);
			this.sleep = sleep;
		}
		
		public void run() { 
			
			while (true) { 
				try { 
					sleep(sleep);
				} catch (Exception e) {
					// ignore
				}
	
				StringBuilder out = new StringBuilder();
				StringBuilder err = new StringBuilder();
				
				MiscUtils.getMachineLoad(out, err);
		
				if (out.length() > 0) { 
					// Get the first 20 lines 
					int index = out.indexOf("\n");
					
					int count = 1;
					
					while (index > 0 && count < 20) { 
						index = out.indexOf("\n", index+1);
						count++;
					}
					
					if (index > 0) {
						logger.info("LoadMonitor (20):\n" + out.substring(0, index+1));
					} else { 
						logger.info("LoadMonitor (all):\n" + out);	
					}
				}
			}	
		}
	}
	
	public DACHWorker() { 
		
		startTime = System.currentTimeMillis();
		
		try {
			worker = new Worker(this, serverAddress, hubAddresses, pool,
					node, location, masterID, preferSmall, timeout, 
					concurrentWorkers, startDFSServer, dataDir);
		} catch (Exception e) {
			logger.warn("Failed to start worker!", e);
			System.exit(0);
		}
	
		load = new LoadMonitor(30000);
		load.start();
	}
	
	private long time() { 
		return System.currentTimeMillis() - startTime;
	}
	
	private void info(DACHResult r, String message) {
		r.info(message, time());
		logger.info(message);
	}

	private void fatal(DACHResult r, String message) {
		r.fatal(message, time());
		logger.warn(message);
	}

	private void error(DACHResult r, String message) {
		r.error(message, time());
		logger.warn(message);
	}
	
	/*
	private boolean remoteCopy(DACHJob job, DACHResult r, FileInfo info) {

		boolean done = false;
		boolean copied = false;
		
		long start = System.currentTimeMillis();
		
		String file = job.getProblemDir(dataDir) + File.separator + info.name;
		
		Set<String> usedReplicas = new HashSet<String>(); 
		Set<String> usedSites = new HashSet<String>(); 
		
		while (!done) { 
			
			String host = info.selectReplica(usedReplicas, usedSites);
		
			logger.info("Attempting to copy file " + info.name + " from replica " + host);
			
			if (host == null) { 
				done = true;
			} else { 
				StringBuilder out = new StringBuilder();
				StringBuilder err = new StringBuilder();

				int exit = FileUtils.run(new String [] { scpExec, "-oStrictHostKeyChecking=no", "-oConnectTimeout=10", host + ":" + file, tmpDir }, out, err); 

				if (exit != 0) {
					fatal(r, "Failed to remotely copy file " + file + " (stdout: " + out + ") (stderr: " + err + ")\n");
				} else { 
					done = true;
					copied = true;
				}
			} 		
		}
		
		if (!copied) { 
			fatal(r, "Failed to find working replica for " + file);
			return false;
		}
				
		long end = System.currentTimeMillis();
		
		info(r, "Remote copying " + file + " took " + (end-start) + " ms.");
		return true;		
	}
	*/
		
	/*
	private boolean remoteCopy(DACHJob job, DACHResult r, FileInfo info, String localFile) {
		
		DFSClient c = worker.getDFSClient();
		
		long start = System.currentTimeMillis();
		
		String path = job.getProblemDir(dataDir) + File.separator + info.name;
		
		logger.warn("Copying remote file: " + path + " to local file " + localFile);
		
		Set<String> replicas = info.replicaHosts;
		
		boolean success = false;
		
		for (String replica : replicas) { 
			try { 
				c.get(replica, path, false, localFile);
				success = true;
				break;
			} catch (Exception e) {
				logger.warn("Failed to copy file " + path + " from replica " + replica, e);
			}
		}
		
		long end = System.currentTimeMillis();
		
		if (!success) { 
			logger.warn("Failed to remotely copy file " + path + " in " + (end-start) + "ms. from any of its replicas: " + replicas);
			fatal(r, "Failed to remotely copy file " + path + " in " + (end-start) + "ms. from any of its replicas: " + replicas + "\n");
			return false;
		}		
		
		File f = new File(localFile);
		
		if (!f.exists() || !f.canRead() || (f.length() != info.size)) { 
			logger.warn("Remotely copy file " + path + " was corrupted!" + (end-start));
			fatal(r, "Remotely copy file " + path + " was corrupted!" + (end-start));
			return false;
		}	
		
		info(r, "Remote copying " + path + " took " + (end-start) + " ms.");
		return true;		
	}
	
	private boolean localCopy(DACHJob job, DACHResult r, FileInfo info, String localFile) { 
	
		long start = System.currentTimeMillis();
		
		String file = job.getProblemDir(dataDir) + File.separator + info.name;
		
		logger.warn("Copying local file: " + file + " to local file " + localFile);
		
		if (!MiscUtils.fileExists(file)) { 
			logger.warn("Input file " + file + " not found\n");
			return false;
		}
		
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();

		int exit = MiscUtils.run(new String [] { cpExec, file, localFile }, out, err); 

		if (exit != 0) {
			fatal(r, "Failed to locally copy file " + file + " (stdout: " + out + ") (stderr: " + err + ")\n");
			return false;
		}
		
		long end = System.currentTimeMillis();
		
		info(r, "Local copying " + file + " took " + (end-start) + " ms.");
		return true;		
	}		
	*/
	
	private boolean prepareInputFile(DACHJob job, DACHResult r, FileInfo info, String localFile) { 
	
		/*
		if (dfs) {			
			return localCopy(job, r, info, localFile);
		} else {
			
			if (info.onSiteOfHost(node)) {
				// This is a local file!
				return localCopy(job, r, info, localFile);
			} else {
				return remoteCopy(job, r, info, localFile);
			}
		}*/
		
		String path = job.getProblemDir(dataDir) + File.separator + info.name;
		StringBuilder out = new StringBuilder();
		
		DFSClient c = worker.getDFSClient();
		
		boolean local = job.isPreferredLocation(location);
		
		if (!c.copy(path, info, localFile, out, local)) { 
			r.fatal(out.toString(), time());
			return false;	
		}
		
		r.info(out.toString(), time());
		return true;
	}
	
	private void compare(DACHJob pair, DACHResult result) {

		long start = System.currentTimeMillis();
		
		info(result, "Comparing Job " + pair.ID + " " + start + " " + pair.beforeInfo.name 
				+ " " + pair.afterInfo.name + "\n");
    	
    	String problem = pair.getProblemDir(dataDir);
    	String before = pair.getBeforePath(dataDir);
    	String after = pair.getAfterPath(dataDir);
    	
    	if (!MiscUtils.directoryExists(problem)) { 
			fatal(result, "Problem directory " + pair.getProblemDir(dataDir) + " not found!\n");
			return;		
		}
    	
    	before = tmpDir + File.separator + pair.beforeInfo.name;
    	after = tmpDir + File.separator + pair.afterInfo.name;
    	
    	if (!prepareInputFile(pair, result, pair.beforeInfo, before)) { 
    		return;
    	}
    	
    	long t1 = System.currentTimeMillis();
    	
    	if (!prepareInputFile(pair, result, pair.afterInfo, after)) { 
    		return;
    	}
    			
    	if (dryRun) { 
    		result.setResult("DryRun -- No processing performed".getBytes(), time());
		} else { 
			
			long startCompute = System.currentTimeMillis();
			
			RunProcess p = new RunProcess(new String [] { exec, "-w", tmpDir, before, after });
			p.run();

			byte [] out = p.getStdout();
			String err = new String(p.getStderr());
				
			int exit = p.getExitStatus();		
			
			if (exit != 0) {
				fatal(result, "Failed to run comparison: (stderr: " + err + ")\n");
				return;
			}

			long end = System.currentTimeMillis();
			
			error(result, err.toString());		
		
			result.setResult(out, time());
			
			String message = "Completed Job " + pair.ID + " "
				+ before + " " + after + " " + end + " " + 
				+ (end-start) + " Transfer " + 
				+ (startCompute-start) + " " + (t1-start) + " " + (startCompute-t1) + " Compute "
				+ (end-startCompute) + " [ " + start + " " + t1 + " " + startCompute + " " + end + " ]";
			
			info(result, message);
		}
    	
    	File tmp = new File(before);
    	
    	try { 
    		tmp.delete();
    	} catch (Exception e) {
    		logger.warn("Failed to delete file: " + before);
    	}
	
    	tmp = new File(after);
    	
    	try { 
    		tmp.delete();
    	} catch (Exception e) {
    		logger.warn("Failed to delete file: " + after);
    	}
    }
	
	public Result process(Job j) {

		DACHJob job = (DACHJob) j;
		DACHResult result = new DACHResult(job.ID, node, location);
		
		result.setStartTime();
		
		//if (dryRun) { 
		//	result.setResult("DryRun -- No processing performed", time());
		//} else { 
			setCurrentJob(job);
			compare(job, result);
			clearCurrentJob();
		//}
		
		return result;
	}

	private synchronized void setCurrentJob(DACHJob job) { 
		this.currentJob = job;
	}

	private synchronized void clearCurrentJob() { 
		this.currentJob = null;
	}
	
	public synchronized void abort(long jobID) { 
		
		if (currentJob == null) { 
			return;
		}
		
		if (currentJob.ID == jobID) { 
			logger.warn("Abort of current job not implemented!");
		}
	}
	
	public void start() { 
		worker.start();
		worker.end();
		
		System.exit(0);
	}
	
	private static boolean isSet(String value) { 
		return value != null && value.trim().length() > 0;
	}
	
	public static void main(String [] args) { 
		
		logger.warn("DACHWorker parsing command line");
		
		for (int i=0;i<args.length;i++) { 
    		if (args[i].equals("-dryRun")) { 
    			dryRun = true;
    		} else if (args[i].equals("-small")) {    			
    			preferSmall = true;        		
    		} else if (args[i].equals("-startDFSServer")) { 
    			startDFSServer = true;        		
    		} else if (args[i].equals("-pool") && i != args.length-1) { 
    			pool = args[++i];
    		} else if (args[i].equals("-node") && i != args.length-1) { 
    			node = args[++i];
    		} else if (args[i].equals("-location") && i != args.length-1) { 
    			location = args[++i];
    		} else if (args[i].equals("-master") && i != args.length-1) { 
    			masterID = args[++i];
    		} else if (args[i].equals("-server") && i != args.length-1) { 
    			serverAddress = args[++i];
    		} else if (args[i].equals("-hubs") && i != args.length-1) { 
    			hubAddresses = args[++i];
    		} else if (args[i].equals("-tmpDir") && i != args.length-1) { 
    			tmpDir = args[++i];    	
    		} else if (args[i].equals("-dataDir") && i != args.length-1) { 
    			dataDir = args[++i];    	
    		} else if (args[i].equals("-copy") && i != args.length-1) { 
    			cpExec = args[++i];
    		} else if (args[i].equals("-remoteCopy") && i != args.length-1) { 
    			scpExec = args[++i];    		
    		} else if (args[i].equals("-concurrent") && i != args.length-1) { 
    			concurrentWorkers = Integer.parseInt(args[++i]);    			
    		} else if (args[i].equals("-exec") && i != args.length-1) { 
    			exec = args[++i];
    		} else if (args[i].equals("-cluster") && i != args.length-1) { 
    			// ignored
    			++i;
    		} else if (args[i].equals("-output") && i != args.length-1) { 
    			// ignored
    			++i;
    		} else if (args[i].equals("-dfs")) { 
    			dfs = true;    		
    		} else { 
    			System.err.println("Unknown or incomplete option: " + args[i]);
    			System.exit(0);
    		}
    	}
		
		if (!isSet(serverAddress)) { 
			logger.fatal("serverAddress not set");
			System.exit(0);
		}
	
		if (!isSet(pool)) { 
			logger.fatal("pool not set");
			System.exit(0);
		}
	
		if (!isSet(location)) { 
			logger.fatal("cluster not set");
			System.exit(0);
		}
	
		if (!isSet(tmpDir)) { 
			logger.fatal("tmpDir not set");
			System.exit(0);
		}
		
		if (!isSet(dataDir)) { 
			logger.fatal("dataDir not set");
			System.exit(0);
		}
	
		if (!isSet(exec)) { 
			logger.fatal("DACH exec not set");
			System.exit(0);
		}
		
		if (!isSet(node)) { 
			logger.fatal("node not set");
			System.exit(0);
		}
		
		if (!isSet(masterID)) { 
			logger.fatal("masterID not set");
			System.exit(0);
		}
		
		logger.warn("DACHWorker starting!");
		
		new DACHWorker().start();
	
		logger.warn("DACHWorker exit!");
		
	}
}
