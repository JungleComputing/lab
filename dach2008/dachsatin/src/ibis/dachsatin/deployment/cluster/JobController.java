package ibis.dachsatin.deployment.cluster;

import java.util.HashMap;
import java.util.LinkedList;

public class JobController {

	private static final int TIMEOUT = 1000;
	
	private LinkedList<JobHandler> toSubmit = new LinkedList<JobHandler>();
	
	private HashMap<String, JobHandler> active = new HashMap<String, JobHandler>();
	
	private LinkedList<JobHandler> stopped = new LinkedList<JobHandler>();
	
	private boolean done = false;
	
	private LinkedList<Submitter> submitters = new LinkedList<Submitter>();
	
	public JobController(int threads) { 
		
		for (int i=0;i<threads;i++) { 
			Submitter s = new Submitter(this);
			submitters.add(s);
			s.start();
		}
	}
	
	public synchronized void addJobToSubmit(JobHandler h) { 
		toSubmit.addLast(h);
		notifyAll();
	}
	
	protected synchronized JobHandler getJobToSubmit() { 
		while (toSubmit.size() == 0 && !done) { 
			try { 
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
		}
			
		if (done) { 
			return null;
		}
		
		JobHandler h = toSubmit.removeFirst();
		active.put(h.ID, h);
		return h;
	}
	
	public synchronized void stopped(String ID) { 
		
		JobHandler tmp = active.remove(ID);
		
		if (tmp == null) { 
			System.err.println("EEP: stopped got non-existent ID!");
			return;
		}
		
		stopped.add(tmp);
		notifyAll();
	}

	public synchronized LinkedList<JobHandler> getStoppedJobs() {

		if (stopped.size() == 0) { 
			try { 
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
		} 
		
		if (stopped.size() == 0) { 
			return null;
		}
		
		LinkedList<JobHandler> tmp = stopped;
		stopped = new LinkedList<JobHandler>();
		return tmp;
	}
	
	public synchronized boolean hasJobs() { 
		return toSubmit.size() > 0 || active.size() > 0 || stopped.size() > 0;
	}
	
	
}
