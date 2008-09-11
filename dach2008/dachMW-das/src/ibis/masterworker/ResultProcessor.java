package ibis.masterworker;

public interface ResultProcessor {
	public void success(Job job, Result result);	
	public boolean failed(Job job, Result result);	
	public boolean needMoreJobs();
}
