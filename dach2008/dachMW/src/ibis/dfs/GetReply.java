package ibis.dfs;

public class GetReply extends Message {
	
	private static final long serialVersionUID = -5392514948261561619L;
	
	public final long fileSize;
	public final int bufferSize;
	
	public GetReply(final long messageID, final long fileSize, final int bufferSize) {
		super(messageID);
		this.fileSize = fileSize;
		this.bufferSize = bufferSize;
	}
}
