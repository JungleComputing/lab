package ibis.masterworker;

import java.util.List;

public interface JobQueue {
	
	public boolean abortJob(long jobID);
	public Job getJob(String location, boolean allowRemote);
	public Job getJob();
	public void addJob(Job job);
	public void addJobs(List<? extends Job> jobs);
	public int getLength();
}