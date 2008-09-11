package ibis.masterworker;

import ibis.dfs.DFSClient;
import ibis.dfs.DFSServer;
import ibis.ipl.IbisIdentifier;
import ibis.simpleComm.SimpleCommunication;
import ibis.simpleComm.Upcall;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class Worker implements Upcall {
	
	private static final int DEFAULT_TIMEOUT = 60000;
	private static final int DEFAULT_END_TIMEOUT = 10000;

	private static final int MAX_JOB_WAIT = 30000;
	private static final int INITIAL_JOB_WAIT = 1000;
	
	private static final Logger logger = Logger.getLogger("masterworker.worker");
	
	private final String node; 
	private final String location; 
	
	private final boolean preferSmall;
	
	private final long initialTimeout;
	private long timeout;

	private final JobProcessor processor;
	
	private final SimpleCommunication comm;
	
	private final IbisIdentifier master;
	
	private final LinkedList<Job> jobs = new LinkedList<Job>();
	
	private final DFSClient dfsClient;
	
	private boolean done = false;
	
	private class WorkerThread extends Thread { 
		
		private final int workerNo;
		
		public WorkerThread(final int workerNo) {
			this.workerNo = workerNo;
		}
		
		public void run() { 
			
			while (!getDone()) { 
				
				Job job = getJob();
					
				while (job != null) { 
							
					logger.info("WorkerThread" + workerNo + ": Processing job");

					activeWorkers.incrementAndGet();
					
					Result r = processor.process(job);
					
					activeWorkers.decrementAndGet();
					
					if (!comm.send(master, r)) { 
						logger.warn("Could not return result to master!");
					}
						
					job = getJob();
				}
			}
		}	
	}
	
	private final WorkerThread [] workers;
	
	private AtomicInteger requiredJobs = new AtomicInteger(0);
	
	private AtomicInteger activeWorkers = new AtomicInteger(0);
	
	private final int totalWorkers;
	
	private long stealID = 0;
	
	private ReplyHandler replies = new ReplyHandler();
	
	private DFSServer dfsServer;
	
	public Worker(JobProcessor processor, String serverAddress, String hubAddresses, 
			String pool, String node, String location, String masterID, boolean preferSmall, 
			long timeout, int concurrentJobs, boolean startDFSServer, String dataDir) throws Exception { 
		
		logger.info("Starting worker on " + node + " at location " + location);
		
		this.node = node;
		this.location = location;
		this.processor = processor;
		this.preferSmall = preferSmall;
		
		this.totalWorkers = concurrentJobs;
		
		if (timeout > INITIAL_JOB_WAIT) { 
			this.initialTimeout = Math.min(timeout, MAX_JOB_WAIT); 
		} else { 
			this.initialTimeout = INITIAL_JOB_WAIT;
		}
		
		this.timeout = this.initialTimeout + (int) (Math.random() * INITIAL_JOB_WAIT);

		try {
			comm = SimpleCommunication.create("DACH", this, serverAddress, hubAddresses, pool);
		} catch (Exception e) {
			logger.warn("Failed to initialize communication layer!", e);
			throw new Exception("Failed to initialize communication layer!", e);
		}
		
		master = comm.getElectionResult(masterID, DEFAULT_TIMEOUT);
		
		if (master == null) { 
			throw new Exception("Failed to retrieve master ID (" + masterID + ")!");
		}
		
		
		logger.warn("Starting " + concurrentJobs + " workers!");
		
		workers = new WorkerThread[concurrentJobs];
		
		for (int i=0;i<concurrentJobs;i++) { 
			workers[i] = new WorkerThread(i);
			workers[i].start();
		}
		
		if (startDFSServer) { 
			logger.warn("Starting DFSServer on worker " + node);
			dfsServer = new DFSServer(dataDir, node);
			new Thread(dfsServer, "DFSServer").start();
		}
		
		dfsClient = new DFSClient(node, location); 		
	}
	
	public DFSClient getDFSClient() { 
		return dfsClient;
	}
	
	/*
	private synchronized void handleStealReply(IbisIdentifier src, StealReply r) { 
		
		if (r.job != null) { 
			jobs.addLast(r.job);
			timeout = initialTimeout;
			
			logger.info("Got steal reply (work, " + jobs.size() + "): " + r.job);
		} else if (r.done) {
			done = true;
			timeout = initialTimeout;
		
			logger.info("Got steal reply (done, " + jobs.size() + ")");	
		} else { 
			// When we get an 'no jobs' reply, we increment the 
			// steal timeout and queue a new request.
		//	requiredJobs.incrementAndGet();
			increaseTimeout();
			
			logger.info("Got steal reply (no work, " + jobs.size() + ")");
		}

		gotReply = true;
		notifyAll();
	}*/
	
	
	private void handleAbort(IbisIdentifier src, Abort a) { 
		
		logger.info("Got abort request: " + a.JobID);
		
		// First try to abort the job in the local queue
		synchronized (this) {
		
			if (a.JobID == -1) { 
				jobs.clear();
			} else { 
				
				ListIterator<Job> itt = jobs.listIterator();
				
				while (itt.hasNext()) { 
					Job j = itt.next();
				
					if (j.ID == a.JobID) { 
						itt.remove();
						return;
					}
				}
			}
		
			// If the jobs hasn't been found, we pass the abort on to the processor
			processor.abort(a.JobID);
		}
	}
	
	public boolean upcall(IbisIdentifier src, Object o) {
		
		if (o instanceof StealReply) { 
			StealReply reply = (StealReply) o;
			replies.storeReply(reply.stealID, reply);
			return true;
		} else if (o instanceof Abort) { 
			handleAbort(src, (Abort) o);
			return true;
		} 
		
		return false;
	}
	
	public void died(IbisIdentifier src) { 

		// We only care about the master 
		if (src.equals(master)) { 
			logger.warn("WARNING: Master died! -- Exiting");
			done();
		} else if (src.equals(comm.getIdentifier())) { 
			logger.warn("WARNING: We have been declared dead -- Exiting");
			done();
		}
	}
	
	public void end(long timeout) { 
		comm.end(timeout);
	}
	
	public void end() { 
		end(DEFAULT_END_TIMEOUT);
	}
	
	private synchronized Job getJob() { 
		
		requiredJobs.incrementAndGet();
		notifyAll();
		
		while (jobs.size() == 0 && !getDone()) {
			try { 
				wait();
			} catch (Exception e) {
				// ignore
			}			
		}
		
		requiredJobs.decrementAndGet();
		
		if (getDone()) { 
			return null;
		}
		
		if (jobs.size() > 0) { 
			return jobs.removeFirst();
		}
		
		return null;
	}	
	
	private synchronized void done() {
		done = true;
		replies.done();
	}
	
	private synchronized boolean getDone() {
		return done;
	}
	
	private void sleepForTimeout() {
		
		logger.info("Sleeping for " + timeout + " ms.");
		
		try { 
			Thread.sleep(timeout);
		} catch (Exception e) {
			// ignored
		}
	}
	
	private synchronized void increaseTimeout() { 
		timeout *= 2 + (int)(Math.random() * INITIAL_JOB_WAIT);
		
		if (timeout > MAX_JOB_WAIT + INITIAL_JOB_WAIT) { 
			timeout = MAX_JOB_WAIT + (int)(Math.random() * INITIAL_JOB_WAIT);
		}
		
		logger.info("Job timeout is " + timeout);
	}
	
	/*
	private void doRequest() { 
		
		StealRequest s = new StealRequest(getStealID(), location, node, preferSmall);
		
		int tmp = requiredJobs.get();
		
		while (tmp > 0 && !getDone()) { 
	
			sleepForTimeout();

			logger.info("Sending steal request");

			clearReply();
			
			if (!comm.send(master, s)) { 
				logger.warn("Failed to send to master!");
				increaseTimeout();				
			} else { 
				waitForReply();		
				tmp = requiredJobs.decrementAndGet();			
			}
		}
	}
	*/
	
	private synchronized int queuedJobs() {
		return jobs.size();
	}
	
	private synchronized long getStealID() { 
		return stealID++;
	}
	
	private synchronized void handleStealReply(long stealID) { 
		StealReply r = replies.waitForReply(stealID);

		if (r == null) {
			// we are done ?
			
			logger.info("Got empty reply! " + stealID);
			
			notifyAll();
			return;
		}

		if (r.job != null) { 
			logger.info("Got steal reply " + stealID + " (work, " + jobs.size() + "): " + r.job);
			jobs.addLast(r.job);
			timeout = initialTimeout;
		} else if (r.done) {
			logger.info("Got steal reply " + stealID + " (done, " + jobs.size() + ")");	
			done = true;
			timeout = initialTimeout;
		} else { 
			// When we get an 'no jobs' reply, we increment the 
			// steal timeout and queue a new request.
			logger.info("Got steal reply " + stealID + " (no work, " + jobs.size() + ")");
			increaseTimeout();
		}
		
		notifyAll();
	}
	
	public void start() { 
	
		logger.info("Worker register at master...");
		
		Register reg = new Register(node, 0);
		
		while (!comm.send(master, reg)) {

			logger.info("Worker failed to send register message!");

			try { 
				Thread.sleep(2000);
			} catch (Exception e) {
				// ignore
			}
		}
		
		logger.info("Worker registered at master...");
		
		long nextStatusMessage = System.currentTimeMillis() + 30000;
		long stealID = 0;
		
		while (!getDone()) { 
			
			sleepForTimeout();

			if (System.currentTimeMillis() > nextStatusMessage) { 
				
				Status s = new Status(location, node, activeWorkers.get(), totalWorkers);
				
				logger.info("Sending status: " + activeWorkers + " / " + totalWorkers);
				
				if (!comm.send(master, s)) { 
					logger.warn("Failed to send status message to master!");
				}
				
				nextStatusMessage = System.currentTimeMillis() + 30000;		
			}
			
			if (!getDone() && queuedJobs() == 0 && requiredJobs.get() > 0) { 		
			
				stealID = getStealID();
				
				StealRequest s = new StealRequest(stealID, location, node, preferSmall);
			
				replies.registerSingleRequest(stealID);
				
				logger.info("Sending steal request (" + requiredJobs.get() + " / " + jobs.size() + ")");

				if (!comm.send(master, s)) { 
					logger.warn("Failed to send steal request to master!");
					replies.clearRequest(stealID);
					increaseTimeout();				
				} else { 
					handleStealReply(stealID);
				}
			} else {
				logger.info("NOT sending steal request (" + getDone() + " / " 
						+ requiredJobs.get() + " / " + jobs.size() + ")");
			}
		}
	}
}
