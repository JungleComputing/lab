package ibis.masterworker;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.log4j.Logger;

public class JobQueueList implements JobQueue {
	
	private static final Logger logger = Logger.getLogger("masterworker.queue");
	
	private final LinkedList<Job> matched = new LinkedList<Job>();	
	private final LinkedList<Job> unmatched = new LinkedList<Job>();
	
	private final Set<String> locations;
	
	public JobQueueList(List<String> locations)  { 
		if (locations != null && locations.size() > 0) { 
			this.locations = new HashSet<String>(locations);
		} else { 
			this.locations = null;
		}
	}
	
	private boolean abortJob(LinkedList<Job> q, long jobID) {
		
		ListIterator<Job> itt = q.listIterator();

		while (itt.hasNext()) { 	
			Job tmp = itt.next();

			if (tmp.ID == jobID) { 
				itt.remove();
				return true;
			}
		}

		return false;
	}
	
	public synchronized boolean abortJob(long jobID) {
		
		if (jobID == -1) { 
			matched.clear();
			unmatched.clear();
			return false;
		}
		
		return (abortJob(matched, jobID) || abortJob(unmatched, jobID));
	}
	
	public Job getJob() {
		return getJob(null, true); 
	} 
		
	public Job getJob(String location, boolean allowRemote) {
		if (location == null) { 
			return getAnyJob();
		} else { 
			return selectJob(location, allowRemote);
		}
	}
	
	private synchronized Job getAnyJob() {
		
		logger.info("Selecting a job for any location");
		
		// First try to return a job on a cluster that no-one has claimed
		if (unmatched.size() > 0) { 
			Job tmp = unmatched.removeFirst();
			logger.info("Job found in 'unmatched' queue: " + tmp);
			return tmp;
		}
		
		// If this fails, just get any job we can..
		if (matched.size() > 0) { 
			
			int selection = (int) Math.floor(Math.random() * matched.size());
			
			if (selection >= matched.size()) { 
				logger.warn("Random job selection failed ? " + selection + " " + matched.size());
				selection = matched.size()-1;
			}
			
			ListIterator<Job> itt = matched.listIterator();
			
			int count = 0;
			
			Job job = null;
			
			while (count != selection && itt.hasNext()) { 
				job = itt.next();
			}
		
			if (job != null) { 
				itt.remove();
			}
			
			logger.info("Randomly selected job in the 'matched' queue: " + job);
			return job;
		}
	
		logger.info("No job found in any queue");
		return null;
	}
	
	private synchronized Job selectJob(String location, boolean allowRemote) {
		
		logger.info("Selecting a job for location " + location);
		
		if (!locations.contains(location)) { 
			logger.info("Location " + location + " not known");
			
			if (!allowRemote) { 
				return null;
			}
			
			return getAnyJob();		
		}
		
		ListIterator<Job> itt = matched.listIterator();

		while (itt.hasNext()) { 	
			Job tmp = itt.next();

			if (tmp.isPreferredLocation(location)) { 
				itt.remove();
				logger.info("Found job for location " + location + ": " + tmp);
				return tmp;
			}
		}
		
		logger.info("No job found for location " + location);
		
		if (!allowRemote) { 
			return null;
		}
		
		return getAnyJob();
	}	
	
	public synchronized void addJob(Job job) {
		
		Set<String> locations = job.getPreferredLocations();
		
		if (locations == null || this.locations == null 
				|| Collections.disjoint(this.locations, locations)) { 
			logger.info("Adding job to unmatched queue: " + job);
			sortedAdd(unmatched, job);
			return;
		}
		
		logger.info("Adding job to matched queue: " + job);
		
		sortedAdd(matched, job);
		//printList(matched);
	}
	
	private void sortedAdd(LinkedList<Job> list, Job job) { 
		
		if (list.size() == 0) { 
			list.add(job);
			return;
		}
		
		ListIterator<Job> itt = list.listIterator();
		
		while (itt.hasNext()) { 
			
			Job current = itt.next();
			
			int compare = job.compareTo(current);
			
		//	logger.warn("Compare " + compare + " " + job + " " + current);
			
			if (compare <= 0) {
				
				if (itt.hasPrevious()) { 
					itt.previous();
				}
				
				itt.add(job);
			//	logger.warn("Inserted as middle job");
				return;
			}
		}
		
		//logger.warn("Inserted as last job");
		
		list.addLast(job);
	
	}
	
	public synchronized void addJobs(List<? extends Job> jobs) {
		for (Job j : jobs) { 
			addJob(j);
		}
	}
	
	public synchronized int getLength() {
		return matched.size() + unmatched.size();
	}
}
