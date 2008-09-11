package ibis.masterworker;

import ibis.ipl.IbisIdentifier;
import ibis.masterworker.deployment.Cluster;
import ibis.masterworker.deployment.Deployment;
import ibis.simpleComm.SimpleCommunication;
import ibis.simpleComm.SimpleServer;
import ibis.simpleComm.Upcall;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

public class MasterWrapper implements Upcall {

	private static final int DEFAULT_TIMEOUT = 60000;
	private static final int DEFAULT_END_TIMEOUT = 10000;

	private static final Logger logger = Logger.getLogger("masterworker.masterwrapper");

	private final String node; 
	private final String location; 

	private final boolean preferSmall;

	private final SimpleCommunication comm;
	// private final Master master; 

	private final String myMaster;
	private IbisIdentifier masterID;

	private IbisIdentifier myID;
	
	//private final StealRequest steal;
	
	private SimpleServer hub;
	
	private long stealID = 0;
	
	private WorkerInfo workerInfo = new WorkerInfo();
	private ReplyHandler replies = new ReplyHandler(); 
	
	private boolean done = false;
	
	private final String myName;
	private final String outputDir;
	
	@SuppressWarnings("unused")
	private List<Deployment> deployers = new LinkedList<Deployment>();
		
	public MasterWrapper(String serverAddress, String hubAddresses, String pool, 
			List<Cluster> workerClusters, List<String> workerOptions, String myMaster, 
			String node, String location, String workerDir, 
			boolean preferSmall, boolean useConcurrent) throws Exception { 
		
		this.node = node;
		this.location = location;
		this.preferSmall = preferSmall;
		this.myMaster = myMaster;

        // This one is a bit iffy ...
		this.outputDir = workerDir + File.separator + "output";
		
		LinkedList<String> tmp = new LinkedList<String>();
		
		for (Cluster c : workerClusters) { 
			tmp.add(c.name);
		}
		
		try { 
			hub = new SimpleServer(hubAddresses, true);
		} catch (Exception e) {
			logger.warn("Failed to start hub!", e);
			System.exit(1);
		}
	
		hubAddresses = hub.getHubAddresses();
		
		try {
			comm = SimpleCommunication.create("DACH", this, serverAddress, hubAddresses, pool);
		} catch (Exception e) {
			logger.warn("Failed to initialize communication layer!", e);
			throw new Exception("Failed to initialize communication layer!", e);
		}
		
		if (useConcurrent) { 
			if (workerClusters.size() == 1) { 
				workerOptions.add("-concurrent");
				workerOptions.add(Integer.toString(workerClusters.get(0).cores));
			} else { 
				workerOptions.add("-concurrent");
			}
		}
		
		//master = new Master(this, -1, serverAddress, hubAddresses, pool, location, 
		//		workerClusters, workerOptions, workerDir, null, null, false, false);
	
		myID = comm.getIdentifier();
			
		masterID = comm.getElectionResult(myMaster, DEFAULT_TIMEOUT);

		if (masterID == null) { 
			throw new Exception("Failed to retrieve master " + myMaster);
		}
		
		myName = generateUniqueMasterName(location);
		
		IbisIdentifier m = comm.elect(myName, DEFAULT_TIMEOUT);
		
		if (m == null || !m.equals(comm.getIdentifier())) { 
			logger.warn("Failed to elect myself as local master!");
			System.exit(1);
		}
		
		if (workerClusters.size() > 0) { 
			startWorkers(workerClusters, workerOptions, workerDir);
		}
		
		//steal = new StealRequest(location, node, preferSmall);
	}
	
	private String generateUniqueMasterName(String location) { 
		
		String tmp = "master" + (location != null ? ("-" + location) : (""));
		
		try { 
			File tmp1 = File.createTempFile(tmp, "", null);
			String uniqueID = tmp1.getName();
			tmp1.delete();
			return uniqueID;
		} catch (Exception e) {
			logger.warn("Failed to create unique master name!");
			return tmp;
		}
	}
	
	private void startWorkers(List<Cluster> cluster, List<String> workerOptions, String homeDir) { 

		String workerMain = "dach.DACHWorker";
		
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("log4j.configuration", "file:" + homeDir	+ File.separator + "log4j.properties");
		
		String classpath = homeDir + "/lib/*:" + homeDir + "/lib/ibis/*";

		LinkedList<String> args = new LinkedList<String>();
		args.add("-pool"); 
		args.add(comm.getPool());
		args.add("-server"); 
		args.add(comm.getServerAddress());
		
		String hubs = comm.getHubAddresses();
		
		if (hubs != null && hubs.length() > 0) {
			args.add("-hubs"); 
			args.add(hubs);
		}
		
		args.add("-master"); 
		args.add(myName);
		
		// Not sure this is entirely right...
		args.addAll(workerOptions);
		
		// Start the deployment of the workers
		try { 
			Deployment tmp = new Deployment(cluster, homeDir, outputDir,
					workerMain, classpath, properties, args, false, "worker"); 
			tmp.deploy();
			
			deployers.add(tmp);
		} catch (Exception e) {
			logger.warn("Failed to initialize deployment layer!", e);
			System.exit(1);
		}
	}
	
	
	private synchronized void done() {
		done = true;
		replies.done();
	}
	
	private synchronized boolean getDone() {
		return done;
	}

	
	private void handleStealReply(IbisIdentifier src, StealReply r) { 
		replies.storeReply(r.stealID, r);
	}

	private void handleAbort(IbisIdentifier src, Abort a) { 

		logger.warn("Discarding abort request: " + a.JobID);

		// Pass on the abort to my workers
		// master.abortJob(a.JobID);

		// FIXME!!! NOT IMPLEMETED
	}
	
	private void handleRegister(IbisIdentifier src, Register r) { 
		
		String node = r.node;
		
		WorkerStatistics tmp = workerInfo.getStats(node);
		
		if (tmp != null && !tmp.checkIdentity(src)) { 
			
			IbisIdentifier died = tmp.getIdentity();
			
			logger.warn("Got RE-registration for node " + node 
					+ " changing from ibis " + died + " --> " + src);
		
			Long [] pending = tmp.getPendingJobIDs();
			
			for (Long id : pending) { 
				tmp.errorJob(id, 0);
			}
			
			FailedJobs failed = new FailedJobs(node, pending);
			
			if (!comm.send(masterID, failed)) { 
				logger.error("Failed to send failed jobs list to " + myMaster);
			}
			
			tmp.addIdentity(src);
			comm.maybeDead(died);
		} else { 
			
			logger.warn("Got NEW registration for node " + node 
					+ " from ibis " + src);
			
			if (!workerInfo.addStats(node, src)) { 
				logger.warn("Failed to add worker statistics for worker: " + node + " on " + src);
			} 
		}
		
		
		logger.info("Registration done...");
	}
	
	private void handleStatus(IbisIdentifier src, Status r) { 
	
		logger.info("Master got status message from " + src);
	
		if (!workerInfo.updateStatus(src, r.node, r.activeWorkers, r.totalWorkers)) { 
			logger.warn("Got status update from from unregistered node " + r.node + " on " + src);
		} 
	}
	
	private void handleStealRequest(IbisIdentifier src, StealRequest r) { 
		
		logger.info("Got steal request " + r.stealID + " from " + src);
		
		long ID = getStealID();
		
		StealRequest steal = new StealRequest(ID, location, node, preferSmall);
		
		replies.registerSingleRequest(ID);
		
		if (!comm.send(masterID, steal)) { 
			logger.error("Failed to forward steal request to " + myMaster);
			replies.clearRequest(ID);
			return;
		}

		StealReply masterReply = replies.waitForReply(ID);
		StealReply myReply = null;
		
		if (masterReply == null) { 
			// we are done ?
			myReply = new StealReply(r.stealID, null, true);
		} else { 
			
			if (masterReply.done) { 
				done();
			}
			
			myReply = new StealReply(r.stealID, masterReply.job, masterReply.done);
		}
		
		if (!comm.send(src, myReply)) { 
			logger.error("ERROR: Failed to forward steal reply to " + src);
			return;
		}
	}
	
	
	
	
	public boolean upcall(IbisIdentifier src, Object o) {

		// These are send by my master
		if (o instanceof StealReply) { 
			handleStealReply(src, (StealReply) o);
			return true;
		} else if (o instanceof Abort) { 
			handleAbort(src, (Abort) o);
			return true;
		} 
		
		// These are send by the workers
		if (o instanceof Status) { 
			handleStatus(src, (Status) o);		
			return true;
		} else if (o instanceof StealRequest) {
			handleStealRequest(src, (StealRequest) o);
			return true;
		} else if (o instanceof Result) { 
			handleResult(src, (Result) o);
			return true;
		} else if (o instanceof Register) { 
			handleRegister(src, (Register) o);
			return true;
		}
	
		return false;
	}

	public void died(IbisIdentifier src) { 
		// We only care about ourselved and the master 
		if (src.equals(masterID)) { 
			logger.warn("WARNING: Master died! -- Exiting");
			done();
		}
		
		if (src.equals(myID)) { 
			logger.warn("WARNING: I seem to have died! -- Exiting");
			done();
		}
	}

	public void end(long timeout) { 
		comm.end(timeout);
	}

	public void end() { 
		end(DEFAULT_END_TIMEOUT);
	}

	public void handleResult(IbisIdentifier src, Result result) {
		
		logger.info("Forwarding result " + result.ID + " to " + myMaster);
		
		// Claim the result
		result.setNode(node);
		
		if (!comm.send(masterID, result)) { 
			logger.error("Failed to forward result " + result.ID + " to " + myMaster);
		}
	}
	
	private synchronized long getStealID() { 
		return stealID++;
	}
	
	public boolean needMoreJobs() { 
		
		StealRequest steal = new StealRequest(getStealID(), location, node, preferSmall);
		
		if (!comm.send(masterID, steal)) { 
			logger.error("Failed to forward steal request to " + myMaster);
		}

		return true;
	}
	
	public void start() { 
		
		Register r = new Register(node, 0);
		
		while (!comm.send(masterID, r)) {

			logger.error("MasterWrapper failed to send register message!");

			try { 
				Thread.sleep(2000);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		long nextStatusMessage = System.currentTimeMillis() + 30000;
		
		while (!getDone()) { 

			logger.info("MasterWrapper running!");

			try { 
				Thread.sleep(5000);
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			if (System.currentTimeMillis() > nextStatusMessage) { 
				
				Status s = new Status(location, node, 
						workerInfo.getActiveWorkers(), workerInfo.getTotalWorkers());
				
				if (!comm.send(masterID, s)) { 
					logger.error("Failed to send status message to master!");
				}
				
				nextStatusMessage = System.currentTimeMillis() + 30000;		
			}
		}

		workerInfo.printStatistics(logger);
		
		end();
	}
	
	private static boolean isSet(String value) { 
		return value != null && value.trim().length() > 0;
	}
	
	public static void main(String [] args) { 
		
		boolean preferSmall = false;
		boolean useConcurrent = false;
		
		String serverAddress = null;
		String hubAddresses = null;
		String pool = null;
		
		String node = null;
		String location = null;
		
		String homeDir = null;
		String myMaster = null;
		
		LinkedList<String> other = new LinkedList<String>();
		LinkedList<Cluster> workerClusters = new LinkedList<Cluster>();
		
		logger.warn("MasterWrapper parsing command line");
		
		for (int i=0;i<args.length;i++) { 
			if (args[i].equals("-concurrent")) { 
    			useConcurrent = true;        		
    		} else if (args[i].equals("-small")) { 
    			preferSmall = true;        		
    		} else if (args[i].equals("-pool") && i != args.length-1) { 
    			pool = args[++i];
    		} else if (args[i].equals("-homeDir") && i != args.length-1) { 
    			homeDir = args[++i];
    		} else if (args[i].equals("-master") && i != args.length-1) { 
    			myMaster = args[++i];
    		} else if (args[i].equals("-node") && i != args.length-1) { 
    			node = args[++i];
    		} else if (args[i].equals("-location") && i != args.length-1) { 
    			location = args[++i];
    		} else if (args[i].equals("-server") && i != args.length-1) { 
    			serverAddress = args[++i];
    		} else if (args[i].equals("-hubs") && i != args.length-1) { 
    			hubAddresses = args[++i];
    		} else if (args[i].equals("-cluster") && i != args.length - 1) {
    			try {
					workerClusters.add(Cluster.read(args[++i]));
				} catch (Exception e) {
					System.err.println("Failed to read cluster file " + args[i]);
					e.printStackTrace(System.err);
					System.exit(1);
				}
    		} else { 
    			logger.warn("Unrecognised option: " + args[i] + " (assuming it's a worker option)");
    			other.add(args[i]);
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
			
		if (!isSet(node)) { 
			logger.fatal("node not set");
			System.exit(0);
		}
	
		if (!isSet(homeDir)) { 
			logger.fatal("homeDir not set");
			System.exit(0);
		}
	
		if (!isSet(myMaster)) { 
			logger.fatal("homeDir not set");
			System.exit(0);
		}
	
		File output = new File(homeDir + File.separator + "output");
		output.mkdir();
		
		logger.warn("MasterWrapper starting!");
		
		try { 
			new MasterWrapper(serverAddress, hubAddresses, pool, workerClusters, other, myMaster, 
					node, location, homeDir, preferSmall, useConcurrent).start();

			logger.warn("MasterWrapper exit!");
		} catch (Exception e) { 
			logger.warn("Failed to run MasterWrapper", e);
		}
	}
	
}


