package ibis.masterworker;

import java.io.Serializable;

public class StealRequest implements Serializable {
	
	private static final long serialVersionUID = -1493325051163795382L;

	// Note: do I need these ?
	public final String location;
	public final String node;
	
	public final boolean preferSmall;
	
	public final long stealID;
	
	public StealRequest(final long stealID, final String location, final String node, 
			final boolean preferSmall) {
		super();
		
		this.stealID = stealID;
		this.location = location;
		this.node = node;
		this.preferSmall = preferSmall;
	}
}
