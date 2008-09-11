package ibis.masterworker;

import java.io.Serializable;

public abstract class Result implements Serializable {

	public final long ID;
	
	private String node;	
	protected long time; 
	private boolean failed = false; 
	
	protected Result(final long ID, final String node) { 
		this.ID = ID;
		this.node = node;
	}
	
	public void setFailed(boolean value) { 
		failed = value;
	}
	
	public boolean getFailed() { 
		return failed;
	}
	
	public void setTime(long value) { 
		time = value;
	}
	
	public long getTime() { 
		return time;
	}
	
	public void setNode(String value) { 
		node = value;
	}
	
	public String getNode() { 
		return node;
	}
	
}
