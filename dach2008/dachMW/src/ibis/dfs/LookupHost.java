package ibis.dfs;

import java.io.Serializable;

public class LookupHost extends Message {

	private static final long serialVersionUID = -4987199792404577334L;

	public final String hostname;

	public LookupHost(final long messageID, final String hostname) {
		super(messageID);
		this.hostname = hostname;
	}

	
}
