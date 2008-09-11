package ibis.masterworker;

import java.io.Serializable;

public class Abort implements Serializable {

	private static final long serialVersionUID = 6153954186089977372L;

	public final long JobID;

	public Abort(final long jobID) {
		super();
		JobID = jobID;
	} 
}
