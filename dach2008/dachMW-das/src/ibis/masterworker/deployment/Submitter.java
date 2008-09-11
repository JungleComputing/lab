package ibis.masterworker.deployment;

import org.apache.log4j.Logger;
import org.gridlab.gat.GAT;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.resources.Job;
import org.gridlab.gat.resources.ResourceBroker;

public class Submitter extends Thread {

	private final JobController controller;	
	private final int number;	
	
	private Logger logger = Logger.getLogger("masterworker.deployment.submitter");
	
	public Submitter(JobController controller, int number) {
		this.controller = controller;
		setDaemon(true);
		this.number = number;
	}
	
	private void submit(JobHandler h) { 
		
		long time = System.currentTimeMillis();
		
		logger.info("Submitter(" + number + ") - attempting to submit job " + h.ID);
		
		ResourceBroker broker = null;
		
		try {
			broker = GAT.createResourceBroker(h.target);
		} catch (GATObjectCreationException e) {
			time = System.currentTimeMillis() - time;
			
			logger.info("Submitter(" + number + ") - failed to create ResourceBroker (job " + h.ID + ") after " + time + " ms.");

			// More detailed info to stderr
			System.err.println("Submitter(" + number + ") - failed to create ResourceBroker (job " + h.ID + ") after " + time + " ms.");
			e.printStackTrace(System.err);
			h.submitFailed();
			return;
		}
		
		Job job = null;

		h.attemptSubmit();
		
		try {
			job = broker.submitJob(h.jobDescription, h, "job.status");
		} catch (GATInvocationException e) {
			time = System.currentTimeMillis() - time;
			logger.info("Submitter(" + number + ") - failed to submit job " + h.ID + " in " + time + " ms.", e);
			h.submitFailed();
			return;
		}
		
		h.setJob(job);

		time = System.currentTimeMillis() - time;
		
		logger.info("Submitter(" + number + ") - successfully submitted job " + h.ID + " in " + time + " ms.");		
	}
	
	public void run() {
		
		while (true) { 
			JobHandler h = controller.getJobToSubmit();
		
			if (h == null) { 
				// we're done
				return;
			}
		
			submit(h);
		}
	}

}
