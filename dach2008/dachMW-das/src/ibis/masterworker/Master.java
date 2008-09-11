package ibis.masterworker;

import ibis.dfs.DFSLocationService;
import ibis.ipl.IbisIdentifier;

import ibis.masterworker.deployment.Cluster;
import ibis.masterworker.deployment.Deployment;
import ibis.simpleComm.SimpleCommunication;
import ibis.simpleComm.SimpleServer;
import ibis.simpleComm.Upcall;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

public class Master implements Upcall {
	
	private static final int DEFAULT_TIMEOUT = 60000;
	private static final Logger logger = Logger.getLogger("masterworker.master");

	private static final int MAX_REMOTE_JOB_DUPLICATION = 2;
	private static final int MAX_LOCAL_JOB_DUPLICATION = 10;
	
	@SuppressWarnings("unused")
	private final String homeDir;
	
	private final String outputDir;
	
	private SimpleServer server;
	private SimpleCommunication comm;
	
	@SuppressWarnings("unused")
	private DFSLocationService locationService;
	
	@SuppressWarnings("unused")
	private List<Deployment> deployers = new LinkedList<Deployment>();
		
	private final JobQueue jobs;
	
	// This map keeps track of which jobs are running where. Its main purpose is to enable us to 
	// restart pending jobs if necessary, and to ensure the (succesfull) job results are only 
	// returned to the client application once.
	private final HashMap<Long, PendingJob> pendingJobs = new HashMap<Long, PendingJob>();
	
	// This map keeps track of which workers are active, how much work they have done, etc.
	//private final HashMap<String, WorkerStatistics> workers = new HashMap<String, WorkerStatistics>();
	//private final ReadWriteLock workersLock = new ReentrantReadWriteLock();
	//private final AtomicInteger activeWorkers = new AtomicInteger(0);
	//private final AtomicInteger totalWorkers = new AtomicInteger(0);
	
	private final WorkerInfo workerInfo = new WorkerInfo();
	
	private boolean done = false;
	
	private final ResultProcessor processor;

	//private final int maxPending;
	
	private final long startTime;
	
	private final String myName;
	
	private final boolean allowRemoteSteals;
	
	
	public Master(ResultProcessor processor, 
			List<Cluster> workerClusters, List<String> workerOptions, String workerDir,
			List<Cluster> dataClusters, String dataDir, boolean allowRemoteSteals, boolean advancedQueue, 
			boolean workersAreDFSServer) { 
		this(processor, -1, null, null, null, null,
				workerClusters, workerOptions, workerDir, dataClusters, dataDir, 
				allowRemoteSteals, advancedQueue, workersAreDFSServer);
	} 
		
	public Master(ResultProcessor processor, int maxPending,
			String serverAddress, String hubAddresses, String pool, String location, 
			List<Cluster> workerClusters, List<String> workerOptions, String workerDir, 
			List<Cluster> dataClusters, String dataDir, boolean allowRemoteSteals, 
			boolean advancedQueue, boolean workersAreDFSServers) { 
	
		this.startTime = System.currentTimeMillis();
		
		this.processor = processor;
		this.homeDir = workerDir;
		this.allowRemoteSteals = allowRemoteSteals;
		
		/*
		if (workerClusters.size() == 1) { 
			this.maxPending = Math.max(1, maxPending);
		} else { 
			int mul = Math.max(1, maxPending);
			this.maxPending = mul * Cluster.maxSize(workerClusters);
		}*/
		
		// This one is a bit iffy ...
		this.outputDir = workerDir + File.separator + "output";
	
		/*
		LinkedList<String> tmp = new LinkedList<String>();
		
		if (workersAreDFSServers) { 
			for (Cluster c : workerClusters) {
				tmp.addAll(c.nodes);
			}
		} else {
			for (Cluster c : workerClusters) { 
				tmp.add(c.name);
			}
		}
		
		if (advancedQueue) { 
			jobs = new JobQueueHashMaps(tmp);
		} else { 
			jobs = new JobQueueList(tmp);
		}*/
		
		jobs = new SimpleJobQueue();
		
		if (serverAddress == null) { 
			try { 
				pool = SimpleServer.generatePool();
			} catch (Exception e) {
				logger.warn("Failed to generate pool name!", e);
				System.exit(1);
			}
		
			try { 
				server = new SimpleServer(hubAddresses);
			} catch (Exception e) {
				logger.warn("Failed to server!", e);
				System.exit(1);
			}
		
			serverAddress = server.getServerAddress();
			hubAddresses = server.getHubAddresses();
		} 
			
		try {
			// FIXME: this name should not be hardcoded!
			comm = SimpleCommunication.create("DACH", this, serverAddress, 
					hubAddresses, pool);
		} catch (Exception e) {
			logger.warn("Failed to initialize communication layer!", e);
			System.exit(1);
		}

		myName = generateUniqueMasterName(location);
		
		IbisIdentifier m = comm.elect(myName, DEFAULT_TIMEOUT);
		
		if (m == null || !m.equals(comm.getIdentifier())) { 
			logger.warn("Failed to elect myself as master!");
			System.exit(1);
		}
		
		if (workersAreDFSServers || (dataClusters != null && dataClusters.size() > 0)) { 
			logger.info("Starting DFSLocationService");

			try { 
				locationService = new DFSLocationService();
			} catch (Exception e) {
				logger.warn("Failed to initialize location service!", e);
				System.exit(1);
			}
		}
		
		// Several cases here:
		// 
		// Multi-level 
		//
		// a) I am the master of the universe, will (optionally) start the DFS, and 1 submaster on 
		//       each cluster
		// b) I am a intermediate master, and 1 submaster on each cluster
		// c) I am a leaf master, and start a worker on each node.
		//
		// Single level 
		//
		// d) I am the master of the (single worker cluster) universe, will (optionally) start the DFS, 
		//    and start a worker on each node.
		//
		
		if (dataClusters != null && dataClusters.size() > 0) { 
			startDataClusters(dataClusters, dataDir);
		}
		
		if (workerClusters.size() > 0) { 
			startWorkerClusters(workerClusters, workerOptions, workerDir, 
					workersAreDFSServers, dataDir);
		} 
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
	
	public String getName() {
		return myName;
	}
	
	private void startDataClusters(List<Cluster> clusters, String dataDir) { 

		String dfsMain = "ibis.dfs.DFSServer";
		
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("log4j.configuration", "file:" + homeDir	+ File.separator + "log4j.properties");
		
		String classpath = homeDir + "/lib/*:" + homeDir + "/lib/ibis/*";

		LinkedList<String> args = new LinkedList<String>();
		args.add("-pool"); 
		args.add(comm.getPool());
		args.add("-server"); 
		args.add(comm.getServerAddress());
		args.add("-root");
		args.add(dataDir);
		
		String hubs = comm.getHubAddresses();
		
		if (hubs != null && hubs.length() > 0) {
			args.add("-hubs"); 
			args.add(hubs);
		}
		
		// Start the deployment of the DFS servers
		try { 
			Deployment tmp = new Deployment(clusters, homeDir, outputDir,
					dfsMain, classpath, properties, args, true, "data"); 					
			tmp.deploy();
			deployers.add(tmp);
		} catch (Exception e) {
			logger.warn("Failed to initialize deployment layer!", e);
			System.exit(1);
		}
	}
	
	private void startWorkerClusters(List<Cluster> clusters, List<String> workerOptions, 
			String homeDir, boolean workersAreDFSServers, String dataDir) { 

		String wrapperMain = "ibis.masterworker.MasterWrapper";
		
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("log4j.configuration", "file:" + homeDir	+ File.separator + "log4j.properties");
		properties.put("gat.adaptor.path", homeDir + "/lib/deploy/adaptors");

		String classpath = homeDir + "/lib/*:" + homeDir + "/lib/deploy/*:" + homeDir + "/lib/ibis/*";

		LinkedList<String> args = new LinkedList<String>();
		args.add("-pool"); 
		args.add(comm.getPool());
		args.add("-server"); 
		args.add(comm.getServerAddress());
		
		if (workersAreDFSServers) { 
			args.add("-startDFSServer");
//			args.add(dataDir);
		}
		
		String hubs = comm.getHubAddresses();
		
		if (hubs != null && hubs.length() > 0) {
			args.add("-hubs"); 
			args.add(hubs);
		}
		
		args.add("-homeDir"); 
		args.add(homeDir);
		
		args.add("-master"); 
		args.add(myName);
		
		args.add("-output");
		args.add(outputDir);
		
		args.addAll(workerOptions);
		
		// Start the deployment of the workers
		try { 
			Deployment tmp = new Deployment(clusters, homeDir, outputDir, 
					wrapperMain, classpath, properties, args, true, "wrapper"); 
			tmp.deploy();
			
			deployers.add(tmp);
			
		} catch (Exception e) {
			logger.warn("Failed to initialize deployment layer!", e);
			System.exit(1);
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
	
	public synchronized boolean getDone() { 
		return done;
	}
	
	public synchronized void done() { 
		done = true;
	}
	
	private synchronized void addPendingJob(IbisIdentifier worker, Job job) { 
		
		PendingJob p = pendingJobs.get(job.ID);
		
		if (p == null) { 
			logger.info("Adding pending job " + job.ID + " for worker " + worker);
			pendingJobs.put(job.ID, new PendingJob(worker, job));
		} else { 
			logger.info("Job " + job.ID + " is already pending, adding worker " + worker);
			p.addWorker(worker);
		}
	}
	
	private synchronized boolean isPendingJob(long ID) { 
		return pendingJobs.containsKey(ID);
	}
		
	private Job removePendingJob(IbisIdentifier worker, long ID, boolean failed) { 
		
		PendingJob p = null;
		
		if (!failed) { 

			synchronized (this) { 
				p = pendingJobs.remove(ID);
			}

			if (p == null) { 
				logger.error("Job " + ID + " is not pending!");
				return null;
			} 

			if (worker != null && !p.hasWorker(worker)) { 
				logger.warn("Job " + ID + " returned by unexpected worker!");
			} 

			Abort abort = new Abort(ID);

			for (IbisIdentifier id : p.workers) { 
					
				if (worker == null || !id.equals(worker)) {
					logger.warn("Sending abort " + ID + " to worker " + id);
					comm.send(id, abort);
				}
			}
			
			
		
		} else { 
			synchronized (this) { 
				p = pendingJobs.get(ID);
			
				if (p == null) { 
					logger.error("Job " + ID + " is not pending!");
					return null;
				} 
				
				if (!p.hasWorker(worker)) { 
					logger.warn("Job " + ID + " returned by unexpected worker: "+ worker 
							+ " not in " + p.workers);
				} else if (p.removeWorker(worker) == 0) { 
					// No pending jobs left!
					pendingJobs.remove(ID);
				}
			}
		}
		
		return p.job;
	}
	
	private synchronized Job selectPendingJob(IbisIdentifier src, String location) { 
		
		// select the pending job with the least workers
		PendingJob smallest = null;
		PendingJob smallestLocal = null;
		
		for (PendingJob p : pendingJobs.values()) { 
			if (!p.hasWorker(src)) {
				if (p.job.isPreferredLocation(location)) { 
					if (smallestLocal == null) { 
						smallestLocal = p;
					} else if (p.countWorkers() < smallestLocal.countWorkers()) { 
						smallestLocal = p;
					}
				} 
				
				if (smallest == null) { 
					smallest = p;
				} else if (p.countWorkers() < smallest.countWorkers())  {
					smallest = p;
				}
			}
		}	
		
		// We prefer to return the job which is least replicated
		if (allowRemoteSteals && smallest != null && smallest.countWorkers() < MAX_REMOTE_JOB_DUPLICATION) { 
			logger.info("Returning pending REMOTE job " + smallest.job.ID + " to machine " + src + " at " 
					+ location + " (" + smallest.workers + ") local = " + smallest.job.isPreferredLocation(location));
			smallest.addWorker(src);
			return smallest.job;
		}
		
		// But if we can't, any local job will do
		if (smallestLocal != null && smallestLocal.countWorkers() < MAX_LOCAL_JOB_DUPLICATION) { 
			logger.info("Returning pending LOCAL job " + smallestLocal.job.ID + " to machine " 
					+ src + " at " + location + " (" + smallestLocal.workers + ") local = " 
					+ true);
			smallestLocal.addWorker(src);
			return smallestLocal.job;
		}
		
		return null;	
	}
	
	/*
	private WorkerStatistics getStats(String node) { 
		
		workersLock.readLock().lock();
		WorkerStatistics tmp = workers.get(node);
		workersLock.readLock().unlock();
		
		return tmp;
	}

	private boolean addStats(String node, IbisIdentifier src) { 
		
		workersLock.writeLock().lock();
		
		WorkerStatistics tmp = workers.get(node);
		
		if (tmp != null) {
			workersLock.writeLock().unlock();
			return false;
		}
			
		tmp = new WorkerStatistics(node, src, System.currentTimeMillis()-startTime);
		workers.put(node, tmp);
		
		workersLock.writeLock().unlock();
		
		return true;
	}
	
	private WorkerStatistics searchStats(IbisIdentifier src) { 
		
		workersLock.readLock().lock();
		
		for (WorkerStatistics tmp : workers.values()) { 
			if (tmp.checkIdentity(src)) { 
				workersLock.readLock().unlock();
				return tmp;
			}
		}
		
		workersLock.readLock().unlock();
		return null;
	}*/
	
	public void handleStealJob(IbisIdentifier src, StealRequest r) {
		
		logger.info("Master got steal request " + r.stealID + " from " + src);
		
		WorkerStatistics stats = workerInfo.getStats(r.node);
		
		if (stats == null || !stats.checkIdentity(src)) { 
			logger.warn("Got steal request from unregistered node " + r.node + " on " + src);

			// Nodes must register before they get work!
			comm.send(src, new StealReply(r.stealID, null, true));
			stats.stealrequest(false, false);
			return;
		}
		
		
		if (getDone()) { 
			logger.info("Steal reply " + r.stealID + " to " + r.node + " we are done");

			// If we are done, we immediately inform the worker of this!
			comm.send(src, new StealReply(r.stealID, null, true));
			stats.stealrequest(false, false);
			return;
		}

		/*	
		if (stats.pendingJobs() > maxPending) { 
			logger.warn("Refusing to send extra job to " + r.node);
			comm.send(src, new StealReply(null, false));
			stats.stealrequest(false, false);			
			return;
		}
		*/
		
		boolean oldJob = false;
		
		Job job = jobs.getJob(r.location, allowRemoteSteals);

		if (job == null) {
			
			// Ask if more work is on the way (although it may not be immediately available)
			if (processor.needMoreJobs()) { 
				// More jobs may have been produced
				job = jobs.getJob(r.location, allowRemoteSteals);	
			} else { 
				// We are out of jobs, so get a pending one ?
				job = selectPendingJob(src, r.location);
				oldJob = true;	
			}
				
			if (job == null) { 
				logger.info("Steal reply " + r.stealID + " to " + src + ": no work");
				comm.send(src, new StealReply(r.stealID, null, false));
				stats.stealrequest(false, false);			
				return;
			}
		} else { 
			addPendingJob(src, job);
		}
		
		if (!comm.send(src, new StealReply(r.stealID, job, false))) { 
			// send has failed, requeue job
			logger.info("Steal reply " + r.stealID + " to " + src + ": failed");
			removePendingJob(src, job.ID, false);
			
			if (!oldJob) { 
				jobs.addJob(job);			
			}
			
			stats.stealrequest(false, false);
			return;
		}
		
		stats.stealrequest(true, oldJob);
		stats.addPendingJob(job.ID);
		
		logger.info("Steal reply " + r.stealID + " to " + r.node + ": succesfull -- Job " + job.ID);
	}
	
	
	public void handleResult(IbisIdentifier src, Result r) { 
		
		String node = r.getNode();
		
		logger.info("Master got result message from " + node + " (" + r.getFailed() + ")");
		
		WorkerStatistics stats = workerInfo.getStats(node);
		
		if (stats == null || !stats.checkIdentity(src)) { 
			logger.warn("WARNING: Got result " + r.ID + " from unregistered node " 
					+ r.getNode() + " on " + src + " -- DISCARDED!");
			return;		
		}
		
		boolean failed = r.getFailed();
		
		Job job = removePendingJob(src, r.ID, failed);

		if (job == null) { 
			logger.warn("WARNING: Pending job " + r.ID + " could not be found!");
			
			// This was a restarted pending job ?
			stats.wastedJob(r.ID, r.getTime());
			return;	
		}	
		
		if (!failed) { 
			logger.info("Job " + r.ID + " succesfully returned by " + node);
			processor.success(job, r);
			stats.finishedJob(r.ID, r.getTime());
		} else { 
			logger.info("Job " + r.ID + " failed " + node);
		
			if (processor.failed(job, r)) { 
				
				if (!isPendingJob(job.ID)) { 
					logger.info("RESTART: failed job " + r.ID);
					jobs.addJob(job);
				} else { 
					logger.info("NO RESTART: failed job " + r.ID + " (is pending)");
				}
				
			} else { 
				logger.warn("NO RESTART failed job " + r.ID + " (owner refused)");
			}
			stats.errorJob(r.ID, r.getTime());
		}	
	} 
	
	private void recycleJobs(WorkerStatistics stats, Long [] ids, IbisIdentifier died) { 
		
		for (Long id : ids) { 
			stats.errorJob(id, 0);

			Job job = removePendingJob(died, id, true);
				
			if (job != null) { 
				if (!isPendingJob(id)) {
					logger.warn("RESTART: job of died ibis " + died);
					jobs.addJob(job);
				} else { 
					logger.warn("NO RESTART: job of died ibis " + died + " (is pending)");
				}
			} else { 
				logger.warn("NOT FOUND: job of died ibis " + died);	
			}
		}		
	}
	
	public void handleRegister(IbisIdentifier src, Register r) { 
		
		String node = r.node;
		
		WorkerStatistics tmp = workerInfo.getStats(node);
		
		if (tmp != null && !tmp.checkIdentity(src)) { 
			
			IbisIdentifier died = tmp.getIdentity();
			
			logger.warn("Got RE-registration for node " + node 
					+ " changing from ibis " + died + " --> " + src);
		
			Long [] pending = tmp.getPendingJobIDs();
			
			tmp.addIdentity(src);
			comm.maybeDead(died);
			
			recycleJobs(tmp, pending, died);
			
		} else { 
			
			logger.warn("Got NEW registration for node " + node 
					+ " from ibis " + src);
			
			if (!workerInfo.addStats(node, src)) { 
				logger.warn("Failed to add worker statistics for worker: " + node + " on " + src);
			}
		}
	}
	
	public void handleStatus(IbisIdentifier src, Status r) { 
	
		logger.info("Master got status message from " + src);
		
		workerInfo.updateStatus(src, r.node, r.activeWorkers, r.totalWorkers);
		
		/*
		WorkerStatistics stats = workerInfo.getStats(r.node);
		
		if (stats == null || !stats.checkIdentity(src)) { 
			logger.warn("Got status update from from unregistered node " + r.node + " on " + src);
			return;
		} 
			
		int prevActive = stats.getActiveWorkers();
		stats.updateActiveWorkers(r.activeWorkers);	

		int prevTotal = stats.getTotalWorkers();
		stats.updateTotalWorkers(r.totalWorkers);	
		
		if (prevActive != r.activeWorkers) { 
			activeWorkers.addAndGet(r.activeWorkers - prevActive);
		}
		
		if (prevTotal != r.totalWorkers) { 
			totalWorkers.addAndGet(r.totalWorkers - prevTotal);
		}*/	
	}
	
	private void handleFailedJobs(IbisIdentifier src, FailedJobs fail) { 
		
		logger.warn("Master got failed jobs from " + src + " " + Arrays.toString(fail.IDs));
		
		WorkerStatistics stats = workerInfo.searchStats(src);
		 
		if (stats == null) {
			logger.info("No information found on ibis: " + src);
			return;
		}
		
		if (stats.pendingJobs() == 0) { 
			logger.info("Ibis " + src + " has no pending jobs so they cannot fail!");
			return;
		}
		
		recycleJobs(stats, fail.IDs, src);
	}
		
	public boolean upcall(IbisIdentifier src, Object o) {
	
		if (o instanceof Status) { 
			handleStatus(src, (Status) o);		
			return true;
		} else if (o instanceof StealRequest) {
			handleStealJob(src, (StealRequest) o);
			return true;
		} else if (o instanceof Result) { 
			handleResult(src, (Result) o);
			return true;
		} else if (o instanceof Register) { 
			handleRegister(src, (Register) o);
			return true;
		} else if (o instanceof FailedJobs) { 
			handleFailedJobs(src, (FailedJobs) o);
			return true;
		}
		
		return false;	
	}
	
	public void died(IbisIdentifier src) { 
		
		// An Ibis has left the building....
		logger.info("An Ibis has died ... looking for information...");
		
		WorkerStatistics stats = workerInfo.searchStats(src);
		 
		if (stats == null) {
			logger.info("No information found on dead ibis");
			return;
		}
		
		if (stats.pendingJobs() == 0) { 
			logger.info("Dead ibis had no pending jobs");
			return;
		}
		
		Long [] tmp = stats.getPendingJobIDs();
		
		recycleJobs(stats, tmp, src);
	}
	
	public int getQueueLength() { 
		return jobs.getLength();
	}
	
	public synchronized int getPending() { 
		return pendingJobs.size();
	}
	
	public int getActiveWorkers() { 
		return workerInfo.getActiveWorkers();
	}
	
	public int getTotalWorkers() { 
		return workerInfo.getTotalWorkers();
	}
	
	public void abortJob(long jobID) {
		logger.info("Master aborting job " + jobID);
		removePendingJob(null, jobID, false);
	}

	
	public void addJob(Job job) {
		logger.info("Master adding 1 jobs");

		jobs.addJob(job);
	}
	
	public void addJobs(List<? extends Job> jobs) {
		
		logger.info("Master adding " + jobs.size() + " jobs");
		
		this.jobs.addJobs(jobs);
	}

	public void printStatistics() {
		workerInfo.printStatistics(logger);
	}
	
	public void exit(int timeout) {
		
		long end = System.currentTimeMillis() + timeout;
		
		while (comm.participants() > 0) { 
		
			logger.info("Master waiting for all workers to exit (" + (comm.participants()-1) + " left)");
			
			try { 
				Thread.sleep(1000);
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			if (System.currentTimeMillis() > end) { 
				break;
			}
		}
		
		logger.info("Master exiting!");
		
		comm.end(timeout);
	}
}
