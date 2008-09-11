package ibis.masterworker.deployment;

import org.apache.log4j.Logger;
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

	private final static int MAX_DELAY_TIME = 180;
	
	private final JobController controller;	
	
	public final String outputDir;
	
	public final URI target;
	public final String ID;
    public final JobDescription jobDescription;
    
    private int attempt = -1;
    private long delay = 0;
    
    private long lastSubmissionTime = 0;
    private long nextSubmissionTime = 0;
    
    private boolean hasRun = false;
    
    private long startRunning = 0;    
    private long runningTime = 0;
    
    private long startSubmit = 0;    
    private long submissionTime = 0;
    
    private String prefix;
    
    private Job job;
    
    private JobState lastState = JobState.INITIAL;
    
    private Logger logger = Logger.getLogger("masterworker.deployment.handler");
	
    public JobHandler(JobController controller, String ID, JobDescription jobDescription, 
    		URI target, String outputDir, String prefix) {
    	this.controller = controller;
    	this.ID = ID;
        this.jobDescription = jobDescription;
        this.target = target;
        this.outputDir = outputDir;
        this.prefix = prefix;
    }
   
    public synchronized void increaseDelay(int seconds) { 
    	delay += seconds;
    	
    	if (delay > MAX_DELAY_TIME) { 
    		delay = MAX_DELAY_TIME;
    	}
    }
    
    public synchronized void delaySubmision() { 
    	nextSubmissionTime = System.currentTimeMillis() + delay*1000;
    }
    
    public synchronized long nextSubmissionTime() { 
    	return nextSubmissionTime;
    }
    
    public synchronized void submitFailed() { 
    	
    	lastState = JobState.SUBMISSION_ERROR;
    	
    	if (startSubmit != 0) { 
    		submissionTime = System.currentTimeMillis() - startSubmit;
    	}
    	
    	controller.stopped(ID);
    }
 
    public void jobStopped() { 
    	controller.stopped(ID);
    }
    
    public void jobRunning() { 
    	controller.running(ID);
    }
 
    public synchronized boolean hashCrashed() { 
    
    	if (lastState == JobState.STOPPED) { 
    		if (job != null) { 
    			try {
    				return (job.getExitStatus() != 0);
    			} catch (GATInvocationException e) {
    				logger.warn("Could not retrieve exit status of Job " + ID + "." + attempt, e);
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
    
    	startSubmit = lastSubmissionTime = System.currentTimeMillis();
    	nextSubmissionTime = 0;
    	
    	hasRun = false;    	
    	startRunning = 0;
    	runningTime = 0;
    	submissionTime = 0;
    	
    	try {
 			File stdout = GAT.createFile(outputDir + File.separator + "stdout." 
 					+ prefix + "." + ID + "." + attempt);
			jobDescription.getSoftwareDescription().setStdout(stdout);

			File stderr = GAT.createFile(outputDir + File.separator + "stderr." 
					+ prefix + "." + ID + "." + attempt);
			jobDescription.getSoftwareDescription().setStderr(stderr);
    	
    	} catch (GATObjectCreationException e) {
			System.err.println("Failed to reset stdout/stderr of job " + ID + "." + attempt);
		}
    }
    
    public synchronized void processMetricEvent(MetricEvent event) {
    	JobState state = (JobState) event.getValue();
    
		logger.info("JOB " + ID + "." + attempt + ": " + lastState);
	    
    	if (state != lastState) { 
    		switch (state) { 
    		case PRE_STAGING:
    		case POST_STAGING:
    		case SCHEDULED:
    		case ON_HOLD:
    		case UNKNOWN:
    		case INITIAL:
    			// Any need to handle these ?
    			break;

    		case RUNNING:
    			jobRunning();
    			hasRun = true;
    			startRunning = System.currentTimeMillis(); 
    			break;
    			
    		case STOPPED:
    			jobStopped();
    			
    			if (hasRun) { 
    				runningTime = System.currentTimeMillis() - startRunning;    			
    			} 
    			
    			break;
    			
    		case SUBMISSION_ERROR:
    			submitFailed();
    			break;		
    		}
    	}
    	
    	lastState = state;
    	
    	logger.info("Got event: JOB " + ID + "." + attempt + " -- " + lastState);
    }
    
    public synchronized boolean stopped() { 
    	return lastState == JobState.STOPPED; 
    }
 
    public synchronized boolean submissionError() { 
    	return lastState == JobState.SUBMISSION_ERROR; 
    }
    
    public synchronized boolean hasRun() { 
    	return hasRun;
    }

    public synchronized long lastSubmissionTime() { 
    	return lastSubmissionTime;
    }
    
	public synchronized void setJob(Job job) {
		this.job = job;
	}

	public synchronized Job getJob() {
		return job;
	}

	public synchronized long getSubmissionTime() {
		return submissionTime;
	}
	
	public synchronized int getAttempts() {
		return (attempt + 1);
	}
	
	public synchronized long getRuntime() {
		
		if (hasRun) { 
			if (lastState == JobState.STOPPED) { 
				return runningTime;
			} else { 
				return System.currentTimeMillis() - startRunning;
			}
		} 
		
		return 0;
	}
}
