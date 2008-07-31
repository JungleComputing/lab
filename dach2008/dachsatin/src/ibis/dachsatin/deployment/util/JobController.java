package ibis.dachsatin.deployment.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class JobController extends Thread {

	private static final int TIMEOUT = 1000;

	private static final long MAX_SUBMISSION_TIME = 60000;
	
	private LinkedList<JobHandler> undelayed = new LinkedList<JobHandler>();

	private LinkedList<JobHandler> delayed = new LinkedList<JobHandler>();
	
	private HashMap<String, JobHandler> submitted = new HashMap<String, JobHandler>();
	
	private HashMap<String, JobHandler> active = new HashMap<String, JobHandler>();
	
	private LinkedList<JobHandler> stopped = new LinkedList<JobHandler>();
	
	private boolean done = false;
	
	private LinkedList<Submitter> submitters = new LinkedList<Submitter>();
	
	private int addedJobs = 0;
	
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

		addedJobs++;
		
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
		submitted.put(h.ID, h);
		notifyAll();
		return h;
	}
	
	public synchronized void running(String ID) { 
		
		JobHandler tmp = submitted.remove(ID);
		
		if (tmp == null) { 
			System.err.println("EEP: running() got non-existent ID!");
			return;
		}
		
		active.put(ID, tmp);
		notifyAll();
	}
	
	
	public synchronized void stopped(String ID) { 
		
		JobHandler tmp = active.remove(ID);
		
		if (tmp == null) { 
			System.err.println("EEP: stopped() got non-existent ID!");
			return;
		}
		
		stopped.add(tmp);
		notifyAll();
	}

	public synchronized LinkedList<JobHandler> getStoppedJobs() {

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
	
	private synchronized void processedDelayedJobs() { 
		
		long now = System.currentTimeMillis();

		while (delayed.size() > 0) { 

			long waitUntil = delayed.getFirst().nextSubmissionTime();

			if (now >= waitUntil) { 
				undelayed.addLast(delayed.removeFirst());
			}			
		}
		
		notifyAll();
	}
	
	private void processStoppedJobs() { 
		
		LinkedList<JobHandler> stopped = getStoppedJobs();

		if (stopped != null) { 

			if (addedJobs > 0 && active.size() == 0 && undelayed.size() == 0) { 
				System.out.println("Terminating -- there are no active/undelayed jobs left!");
				done();
			}
			
			for (JobHandler h : stopped) {

				if (h.submissionError()) { 

					long time = h.getSubmissionTime();
					
					if (isDone()) { 
						System.out.println("Dropping resubmit of " + h.ID + " to " + h.target + " after submission error -- we are done");
					} else if (time > MAX_SUBMISSION_TIME) { 
						System.out.println("Dropping resubmit of " + h.ID + " to " + h.target + " since previous attempt took: " +
								(time/1000) + " seconds");
					} else if (!done) { 						
						System.out.println("Resubmitting " + h.ID + " to " + h.target + " after submission error (with delay)");
						h.increaseDelay(30);
						h.delaySubmision();
						addJobToSubmit(h);
					} 
				} else if (h.hashCrashed()) {
					
					if (isDone()) { 
						System.out.println("Dropping resubmit of " + h.ID + " to " + h.target + " after crash -- we are done");
					} else if (h.getRuntime() < 60000) { 
						System.out.println("Resubmitting " + h.ID + " to " + h.target + " after crash (with delay)");
						h.increaseDelay(30);
						h.delaySubmision();
						addJobToSubmit(h);
					} else { 
						System.out.println("Resubmitting " + h.ID + " to " + h.target + " after crash (without delay)");
						addJobToSubmit(h);
					}
				} else { 
					System.out.println("Job " + h.ID + " on " + h.target + " is finished");
				}
			}
		}
	}
	
	public synchronized void waitUntilDone() {
		while (!done) { 
			try { 
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
		}		
	}

	public void waitUntilActive(String ID, long timeout) throws Exception {
		
		long end = System.currentTimeMillis() + timeout;
		
		while (active.containsKey(ID)) { 
			try { 
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
			
			if (System.currentTimeMillis() > end) { 
				throw new Exception("Failed to start " + ID + " in given time!");
			}			
		}
	}
	
	public void run() { 

		while (!isDone()) { 

			processStoppedJobs();
			
			if (!isDone()) { 
				processedDelayedJobs();
			
				synchronized (this) {
					try { 
						wait(TIMEOUT);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}
	}


	
	
}
