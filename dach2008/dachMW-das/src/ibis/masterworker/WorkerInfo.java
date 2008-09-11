package ibis.masterworker;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import ibis.ipl.IbisIdentifier;

public class WorkerInfo {

	// This map keeps track of which workers are active, how much work they have done, etc.
	private final HashMap<String, WorkerStatistics> workers = new HashMap<String, WorkerStatistics>();
	// private final ReadWriteLock workersLock = new ReentrantReadWriteLock();

	private final long startTime;
	
	private int activeWorkers = 0;
	private int totalWorkers = 0;
	
	public WorkerInfo() { 
		startTime = System.currentTimeMillis();
	}
	
	public synchronized WorkerStatistics getStats(String node) { 
		return workers.get(node);
	}

	public synchronized boolean addStats(String node, IbisIdentifier src) { 

		WorkerStatistics tmp = workers.get(node);

		if (tmp != null) {
			return false;
		}

		tmp = new WorkerStatistics(node, src, System.currentTimeMillis()-startTime);
		workers.put(node, tmp);
		return true;
	}

	public synchronized WorkerStatistics searchStats(IbisIdentifier src) { 

		for (WorkerStatistics tmp : workers.values()) { 
			if (tmp.checkIdentity(src)) { 
				return tmp;
			}
		}

		return null;
	}
	
	public boolean updateStatus(IbisIdentifier src, String node, int active, int total) { 
		
		WorkerStatistics stats = getStats(node);
		
		if (stats == null || !stats.checkIdentity(src)) { 
			return false;
		} 
			
		int prevActive = stats.updateActiveWorkers(active);	
		int prevTotal = stats.updateTotalWorkers(total);	
		
		synchronized (this) {
			if (prevActive != active) { 
				activeWorkers += active - prevActive;
			}
		
			if (prevTotal != total) { 
				totalWorkers += total - prevTotal;
			}		
		}
		
		return true;
	}
	
	public synchronized int getTotalWorkers() { 
		return totalWorkers;
	}

	public synchronized int getActiveWorkers() { 
		return activeWorkers;
	}
	
	public synchronized void printStatistics(Logger logger) {
		
		long time = System.currentTimeMillis();
		
		for (WorkerStatistics s : workers.values()) { 
			s.setEndTime(time);
			logger.info(s.toString());
		}
	}
	
}
