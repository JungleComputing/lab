package ibis.dachsatin.deployment.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class JobController extends Thread {

	private static final int TIMEOUT = 1000;
	
	private LinkedList<JobHandler> undelayed = new LinkedList<JobHandler>();

	private LinkedList<JobHandler> delayed = new LinkedList<JobHandler>();
	
	private HashMap<String, JobHandler> active = new HashMap<String, JobHandler>();
	
	private LinkedList<JobHandler> stopped = new LinkedList<JobHandler>();
	
	private boolean done = false;
	
	private LinkedList<Submitter> submitters = new LinkedList<Submitter>();
	
	public JobController(int threads) { 
		
		System.out.println("JobController starting " + threads + " SubmitThreads!");
		
		for (int i=0;i<threads;i++) { 
			Submitter s = new Submitter(this, i);
			submitters.add(s);
			s.start();
		}

		// Start ourselves too!
		start();
	}
	
	public synchronized void addJobToSubmit(JobHandler h) {

		long time = System.currentTimeMillis();
		
		if (h.nextSubmissionTime() <= time) {
			System.out.println("Adding undelayed job: " + h.ID);
			undelayed.addLast(h);
		} else {
			System.out.println("Adding delayed job: " + h.ID);
			
			ListIterator<JobHandler> itt = delayed.listIterator();
			
			while (itt.hasNext()) { 
				JobHandler tmp = itt.next();
				
				if (tmp.nextSubmissionTime() > h.nextSubmissionTime()) { 
					itt.add(h);
					break;
				}
			}
		}
		
		notifyAll();
	}
	
	protected synchronized JobHandler getJobToSubmit() { 
		
		while (undelayed.size() == 0 && !done) { 
			try { 
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
		}
			
		if (done) { 
			return null;
		}
		
		JobHandler h = undelayed.removeFirst();
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
		return delayed.size() > 0 || undelayed.size() > 0 
			|| active.size() > 0 || stopped.size() > 0;
	}
	
	public synchronized boolean isDone() { 
		return done;
	}
	
	public synchronized void done() { 
		done = true;
		notifyAll();
	}
	
	public void run() { 
	
		long sleep = TIMEOUT;
		
		do { 
		
			synchronized (this) {
				if (delayed.size() > 0) { 
				
					long now = System.currentTimeMillis();
					long waitUntil = delayed.getFirst().nextSubmissionTime();
			
					// We have a minimum granularity of 50 ms. here
					if ((now+50) >= waitUntil) { 
						undelayed.addLast(delayed.removeFirst());
						notifyAll();
					} else { 
						sleep = waitUntil - now;
					}
				} else { 
					sleep = TIMEOUT;
				}
				
				// This shouldn't be necessary ?
				if (sleep <= 0) { 
					sleep = TIMEOUT;
				}
				
				try { 
					wait(sleep);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			
		} while (!isDone());
	}
	
	
}
