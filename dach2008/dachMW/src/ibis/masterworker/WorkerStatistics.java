package ibis.masterworker;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class WorkerStatistics {

	private static final Logger logger = Logger.getLogger("masterworker.master");
	
	public final String node;
	
	public final LinkedList<IbisIdentifier> identity = new LinkedList<IbisIdentifier>();
	
	public final List<Long> finishedJobs = new ArrayList<Long>();
	public final List<Long> pendingJobs = new LinkedList<Long>();
	public final List<Long> wastedJobs = new ArrayList<Long>();
	public final List<Long> errorJobs = new ArrayList<Long>();
	
	public final long startTime;
	public final long delay;
	public long endTime;
	
	public long time = 0;
	public long wastedTime = 0;
	public long errorTime = 0;
	
	public long steals = 0;
	public long success = 0;
	public long oldjobs = 0;
	
	public int activeWorkers = 0;
	public int totalWorkers = 0;
	
	public WorkerStatistics(final String node, final IbisIdentifier id, final long delay) {
		super();
		this.startTime = System.currentTimeMillis();
		this.node = node;
		this.delay = delay;
		identity.add(id);
	}
	
	public synchronized boolean checkIdentity(IbisIdentifier id) {
		return identity.getLast().equals(id);
	}
	
	public synchronized IbisIdentifier getIdentity() {
		return identity.getLast();
	}
	
	public synchronized boolean addIdentity(IbisIdentifier id) {
		
		IbisIdentifier last = identity.getLast();
		
		if (!last.equals(id)) { 
			identity.addLast(id);
			return true;
		} else { 
			return false;
		}
	}
	
	public synchronized int updateActiveWorkers(int activeWorkers) { 
		int tmp = this.activeWorkers;
		this.activeWorkers = activeWorkers;
		return tmp;
	}

	public synchronized int getActiveWorkers() { 
		return activeWorkers;
	}
	
	public synchronized int updateTotalWorkers(int totalWorkers) { 
		int tmp = this.totalWorkers;
		this.totalWorkers = totalWorkers;
		return tmp;
	}

	public synchronized int getTotalWorkers() { 
		return totalWorkers;
	}
	
	public synchronized void addPendingJob(final Long ID) { 
		pendingJobs.add(ID);
	}
	
	public synchronized int pendingJobs() { 
		return pendingJobs.size();
	}
	
	public synchronized Long [] getPendingJobIDs() {
		return pendingJobs.toArray(new Long[pendingJobs.size()]);
	}
	
	public synchronized void finishedJob(final Long ID, final long time) { 
		
		if (!pendingJobs.remove(ID)) { 
			logger.warn("Failed to find pending job " + ID + " on " + node);			
		}
		
		finishedJobs.add(ID);
		this.time += time;
	}
	
	public synchronized void wastedJob(final Long ID, final long time) { 
		
		if (!pendingJobs.remove(ID)) { 
			logger.warn("Failed to find pending job " + ID + " on " + node);			
		}
		
		wastedJobs.add(ID);
		this.wastedTime += time;
	}
	
	public synchronized void errorJob(final Long ID, final long time) { 
		
		if (!pendingJobs.remove(ID)) { 
			logger.warn("Failed to find pending job " + ID + " on " + node);			
		}
		
		errorJobs.add(ID);
		this.errorTime += time;
	}
	
	public synchronized void setEndTime(long endTime) { 
		this.endTime = endTime;
	}
	
	public synchronized void stealrequest(boolean success, boolean old) { 
		
		steals++;
		
		if (success) { 
			this.success++;
		}
		
		if (old) { 
			this.oldjobs++;
		}
	}
	
	public String toString() { 
	
		long total = endTime - startTime;
		
		long useful = (time * 100) / (total); 
		long wasted = (wastedTime * 100) / (total); 
		long error = (errorTime * 100) / (total); 
		
		return "Worker: " + node + " delay: " + delay + " crashes: " + (identity.size()-1)
		    + " steals (T/S/O): " + steals + " / " +  success + " / " + oldjobs
			+ " time (T/U/W/E): " + total + " / " + time + " (" + useful + " %) / " 
			     + wastedTime + " (" + wasted + " %) / " + errorTime + " (" + error + " %)"
			+ " finished: " + finishedJobs.size() + " " + finishedJobs.toString()
			+ " wasted: " + wastedJobs.size() + " " + wastedJobs.toString()
			+ " pending: " + pendingJobs.size() + " " + pendingJobs.toString()
			+ " error: " + errorJobs.size() + " " + errorJobs.toString();
	}

	
}
