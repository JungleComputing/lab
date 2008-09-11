package ibis.dfs;

import ibis.ipl.IbisIdentifier;

public class LookupReply extends Message {

	private static final long serialVersionUID = 4359485140577841385L;

	public final String hostname;
	public final IbisIdentifier id;

	public LookupReply(final long messageID, final String hostname, final IbisIdentifier id) {
		super(messageID);
		this.hostname = hostname;
		this.id = id;
	}
}
