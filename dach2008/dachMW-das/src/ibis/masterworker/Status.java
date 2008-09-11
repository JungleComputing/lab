package ibis.masterworker;

import java.io.Serializable;

public class Status implements Serializable {
	
	private static final long serialVersionUID = -1493325051163795382L;

	// Note: do I need these ?
	public final String location;
	public final String node;
	
	public final int activeWorkers;
	public final int totalWorkers;
	
	public Status(final String location, final String node, final int activeWorkers, 
			final int totalWorkers) {
		
		super();
		this.location = location;
		this.node = node;
		this.activeWorkers = activeWorkers;
		this.totalWorkers = totalWorkers;
	}
}
