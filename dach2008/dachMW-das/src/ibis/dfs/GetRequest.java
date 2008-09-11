package ibis.dfs;

public class GetRequest extends Message {

	private static final long serialVersionUID = 1L;

	public final String file;
	public final boolean relative;
	
	public final long from;
	public final long to;
	
	public final String pipeID;
	
	public GetRequest(final long messageID, final boolean relative, final String file, 
			final long from, final long to, final String pipeID) {

		super(messageID);
		this.relative = relative;
		this.file = file;
		this.from = from;
		this.to = to;
		this.pipeID = pipeID;
	}
}
