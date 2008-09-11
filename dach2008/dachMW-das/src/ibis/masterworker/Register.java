package ibis.masterworker;

import java.io.Serializable;

public class Register implements Serializable {

	private static final long serialVersionUID = -4260221017826532125L;
	
	public final String node;
	public final int maxConcurrentSteals;
	
	public Register(final String node, final int maxConcurrentSteals) {
		super();
		this.node = node;
		this.maxConcurrentSteals = maxConcurrentSteals;
	}
}
