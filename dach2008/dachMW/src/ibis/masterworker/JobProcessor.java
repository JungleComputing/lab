package ibis.masterworker;

public interface JobProcessor {
	public Result process(Job job); 
	public void abort(long jobID); 
}
