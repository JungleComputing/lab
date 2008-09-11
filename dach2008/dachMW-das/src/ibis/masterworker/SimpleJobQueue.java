package ibis.masterworker;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

public class SimpleJobQueue implements JobQueue {
	
	private static final Logger logger = Logger.getLogger("masterworker.queue");
	
	private final LinkedList<Job> jobs = new LinkedList<Job>();	
	
	public SimpleJobQueue()  { 
	}
	
	public synchronized boolean abortJob(long jobID) {
		
		// not implemented yet
		return false;
	}
	
	public Job getJob() {
		return getJob(null, true); 
	} 
		
	public synchronized Job getJob(String location, boolean allowRemote) {
		
		if (jobs.size() == 0) { 
			return null;
		}
		
		Job job = jobs.removeFirst();
		
		logger.info("Returning job from queue: " + job);
		
		return job;
	}
	
	public synchronized void addJob(Job job) {
		sortedAdd(job);
		logger.info("Adding job to queue: " + job);
	}
	
	private void sortedAdd(Job job) { 
		
		if (jobs.size() == 0) { 
			jobs.add(job);
			return;
		}
		
		ListIterator<Job> itt = jobs.listIterator();
		
		while (itt.hasNext()) { 
			
			Job current = itt.next();
			
			if (current.getJobSize() <= job.getJobSize()) { 
				if (itt.hasPrevious()) { 
					itt.previous();
				}
				itt.add(job);
				return;
			}
		}
		
		jobs.addLast(job);	
	}
	
	public synchronized void addJobs(List<? extends Job> jobs) {
		for (Job j : jobs) { 
			addJob(j);
		}
	}
	
	public synchronized int getLength() {
		return jobs.size();
	}
}
