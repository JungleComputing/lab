package ibis.masterworker;

import java.io.Serializable;


public class StealReply implements Serializable {

	private static final long serialVersionUID = -64886564769339276L;
	
	public final Job job;
	public final boolean done;
	
	public final long stealID;
	
	public StealReply(final long stealID, final Job job, final boolean done) {
		super();
		this.stealID = stealID;
		this.job = job;
		this.done = done;
	}
	
}
