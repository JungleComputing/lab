package ibis.masterworker;

import java.io.Serializable;

public class FailedJobs implements Serializable {

	private static final long serialVersionUID = -4713578018914107566L;
	
	public String node;	
	public final Long [] IDs;

	protected FailedJobs(final String node , final Long [] IDs) { 
		this.IDs = IDs;
		this.node = node;
	}
}
