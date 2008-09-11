package ibis.masterworker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class JobQueueHashMaps implements JobQueue {
	
	private static final Logger logger = Logger.getLogger("masterworker.queue");
	
	private final ArrayList<HashMap<Set<String>, LinkedList<Job>>> matched = 
		new ArrayList<HashMap<Set<String>,LinkedList<Job>>>();
		
	private final LinkedList<Job> unmatched = new LinkedList<Job>();
	
	private final Set<String> locations;
	
	private int totalJobs = 0;
	
	public JobQueueHashMaps(List<String> locations)  { 
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
		
		for (HashMap<Set<String>, LinkedList<Job>> entry : matched) { 
			if (entry != null) {
				for (Entry<Set<String>, LinkedList<Job>> e : entry.entrySet()) { 
					if (e.getValue().size() > 0) {
						if (abortJob(e.getValue(), jobID)) { 
							return true;
						}
					}
				}
			}
		}
		
		return false;
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
		
		if (totalJobs == 0) { 
			logger.info("No jobs left!");
			return null;
		}
	
		logger.info("Selecting a job for any location");
		
		// First try to return a job on a cluster that no-one has claimed
		if (unmatched.size() > 0) { 
			Job tmp = unmatched.removeFirst();
			logger.info("Job found in 'unmatched' queue: " + tmp);
			totalJobs--;
			return tmp;
		}

		// This is so expensive it makes me weep!
		LinkedList<LinkedList<Job>> nonempty = new LinkedList<LinkedList<Job>>();
 		
		for (HashMap<Set<String>, LinkedList<Job>> entry : matched) { 
			if (entry != null) {
				for (Entry<Set<String>, LinkedList<Job>> e : entry.entrySet()) { 
					if (e.getValue().size() > 0) {
						nonempty.add(e.getValue());
					}
				}
			}
		}
		
		if (nonempty.size() > 0) { 
			Collections.shuffle(nonempty);
			LinkedList<Job> tmp = nonempty.removeFirst();
			
			if (tmp.size() > 0) { 
				totalJobs--;
				return tmp.removeFirst();
			}
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
		
		for (HashMap<Set<String>, LinkedList<Job>> entry : matched) { 
			if (entry != null) {
				for (Set<String> key : entry.keySet()) { 
					if (key.contains(location)) { 
						LinkedList<Job> tmp = entry.get(key);
						
						if (tmp != null && tmp.size() > 0) { 
							logger.info("Select job with prefered sites: " + key);
							totalJobs--;
							return tmp.removeFirst();
						} else { 
							logger.info("Slot empty: " + key);
						}
					}
				}
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
	
		int index = locations.size()-1;
	
		matched.ensureCapacity(index+1);
		
		for (int i=matched.size();i<=index;i++) { 
			matched.add(i, new HashMap<Set<String>, LinkedList<Job>>());
		}
		
		HashMap<Set<String>, LinkedList<Job>> tmp = matched.get(index);
		
		if (tmp == null) { 
			// Should not happen!
			tmp = new HashMap<Set<String>, LinkedList<Job>>();
			matched.add(index, tmp);
		}
		
		LinkedList<Job> queue = tmp.get(locations);
		
		if (queue == null) { 
			queue = new LinkedList<Job>();
			tmp.put(locations, queue);
		}
	
		sortedAdd(queue, job);
		
		totalJobs++;
	}
	
	private void sortedAdd(LinkedList<Job> list, Job job) { 
		
		if (list.size() == 0) { 
			list.add(job);
			return;
		}
		
		ListIterator<Job> itt = list.listIterator();
		
		final long size = job.getJobSize();
		
		while (itt.hasNext()) { 
			
			Job current = itt.next();
			
			final long currentSize = current.getJobSize();
			
			if (size > currentSize) {
				
				if (itt.hasPrevious()) { 
					itt.previous();
				}
				
				itt.add(job);
				return;
			}
		}
		
		list.addLast(job);
	}
	
	public synchronized void addJobs(List<? extends Job> jobs) {
		for (Job j : jobs) { 
			addJob(j);
		}
	}
	
	public synchronized int getLength() {
		return totalJobs;
	}
}
