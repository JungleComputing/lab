package ibis.dachsatin.deployment.cluster;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.io.File;
import org.gridlab.gat.monitoring.MetricEvent;
import org.gridlab.gat.monitoring.MetricListener;
import org.gridlab.gat.resources.Job;
import org.gridlab.gat.resources.JobDescription;
import org.gridlab.gat.resources.Job.JobState;

public class JobHandler implements MetricListener {

	private final JobController controller;	
	
	public final URI target;
	public final String ID;
    public final JobDescription jobDescription;
    
    public int attempt = -1;
    
    private Job job;
    
    private JobState lastState = JobState.INITIAL;

    public JobHandler(JobController controller, String ID, JobDescription jobDescription, URI target) {
    	this.controller = controller;
    	this.ID = ID;
        this.jobDescription = jobDescription;
        this.target = target;
    }

    public synchronized void submitFailed() { 
    	lastState = JobState.SUBMISSION_ERROR;
    	controller.stopped(ID);
    }
 
    public synchronized void jobStopped() { 
    	controller.stopped(ID);
    }
 
    public synchronized boolean hashCrashed() { 
    
    	if (lastState == JobState.STOPPED) { 
    		if (job != null) { 
    			try {
    				return (job.getExitStatus() != 0);
    			} catch (GATInvocationException e) {
    				System.out.println("Could not retrieve exit status of Job " + ID + "." + attempt);
    				e.printStackTrace();
    				
    			}
    		}
    	} 
    	
    	// TODO what should we return here ??
    	return false;
    }
    
    public synchronized void attemptSubmit() { 
    	lastState = JobState.INITIAL;
    	job = null;
    	attempt++;
    	
    	try {
			File stdout = GAT.createFile("out." + ID + "." + attempt);
			jobDescription.getSoftwareDescription().setStdout(stdout);

			File stderr = GAT.createFile("err." + ID + "." + attempt);
			jobDescription.getSoftwareDescription().setStderr(stderr);
    	
    	} catch (GATObjectCreationException e) {
			System.err.println("Failed to reset stdout/stderr of job " + ID + "." + attempt);
		}
    }
    
    public synchronized void processMetricEvent(MetricEvent event) {
    	JobState state = (JobState) event.getValue();
    
		System.out.println("JOB " + ID + "." + attempt + ": " + lastState);
	    
    	if (state != lastState) { 
    		switch (state) { 
    		case PRE_STAGING:
    		case POST_STAGING:
    		case RUNNING:
    		case SCHEDULED:
    		case ON_HOLD:
    		case UNKNOWN:
    		case INITIAL:
    			// Any need to handle these ?
    			break;
    			
    		case STOPPED:
    			jobStopped();
    			break;
    			
    		case SUBMISSION_ERROR:
    			submitFailed();
    			break;		
    		}
    	}
    	
    	lastState = state;
    	
    	System.out.println("Got event: JOB " + ID + "." + attempt + " -- " + lastState);
    }
    
    public synchronized boolean stopped() { 
    	return lastState == JobState.STOPPED; 
    }
 
    public synchronized boolean submissionError() { 
    	return lastState == JobState.SUBMISSION_ERROR; 
    }
 
	public synchronized void setJob(Job job) {
		this.job = job;
	}

	public synchronized Job getJob() {
		return job;
	}

}
