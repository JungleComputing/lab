package ibis.dachsatin.deployment.cluster;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.resources.Job;
import org.gridlab.gat.resources.ResourceBroker;

public class Submitter extends Thread {

	private final JobController controller;	
	
	public Submitter(JobController controller) {
		this.controller = controller;
		setDaemon(true);
	}
	
	private void submit(JobHandler h) { 
		
		ResourceBroker broker = null;
	
		try {
			broker = GAT.createResourceBroker(h.target);
		} catch (GATObjectCreationException e) {
			System.err.println("Failed to create ReourceBroker");
			e.printStackTrace(System.err);
			h.submitFailed();
		}
		
		Job job = null;

		h.attemptSubmit();
		
		try {
			job = broker.submitJob(h.jobDescription, h, "job.status");
		} catch (GATInvocationException e) {
			// TODO Auto-generated catch block
			System.err.println("Failed to submit job!");
			e.printStackTrace(System.err);
			h.submitFailed();
		}
		
		h.setJob(job);
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
