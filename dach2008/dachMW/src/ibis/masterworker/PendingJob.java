package ibis.masterworker;

import ibis.ipl.IbisIdentifier;

import java.util.HashSet;

public class PendingJob {

	public final HashSet<IbisIdentifier> workers;
	public final Job job;

	public PendingJob(final IbisIdentifier worker, final Job job) {
		super();
		this.workers = new HashSet<IbisIdentifier>();
		this.workers.add(worker);
		this.job = job;
	}
	
	public synchronized void addWorker(final IbisIdentifier worker) { 
		workers.add(worker);
	}

	public synchronized int countWorkers() { 
		return workers.size();
	}
	
	public synchronized boolean hasWorker(final IbisIdentifier worker) { 
		return workers.contains(worker);
	}
	
	public synchronized int removeWorker(final IbisIdentifier worker) { 
		workers.remove(worker);
		return workers.size();
	}
}
