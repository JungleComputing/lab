package dach;

import ibis.masterworker.Result;

public class DACHResult extends Result {

	private static final long serialVersionUID = 3559010524413137340L;
	
	public final String location;
	
	public final StringBuilder stdout = new StringBuilder();
	public final StringBuilder stderr = new StringBuilder();
	
	public byte [] result;
	
	public long transferTime;
	
	private long startTime;
	
	protected DACHResult(long ID, String node, String location) {
		super(ID, node);
		this.location = location;
	}
	
	public void setStartTime() {
		startTime = System.currentTimeMillis();
	}
	
	public void addTransferTime(long time) { 
		transferTime += time;
	}
	
	private long time() { 
		return System.currentTimeMillis() - startTime;
	}
	
	public void info(String text, long time) {
		stdout.append(time);
		stdout.append(", ");
		stdout.append(time());
		stdout.append(": ");
		stdout.append(text);
	}

	public void fatal(String text, long time) {
		stderr.append(time);
		stderr.append(", ");
		stderr.append(time());
		stderr.append(": ");
		stderr.append(text);
		setFailed(true);
		time = System.currentTimeMillis() - startTime;
	}
	
	public void error(String text, long time) { 
		stderr.append(time);
		stderr.append(", ");
		stderr.append(time());
		stderr.append(": ");
		stderr.append(text);
	}

	public void setResult(byte [] text, long t) { 
		result = text;		
		time = System.currentTimeMillis() - startTime;
		setFailed(false);
	}	
}
