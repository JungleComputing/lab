package ibis.dfs;

public class ErrorReply extends Message {

	private static final long serialVersionUID = -5914394879353533118L;
	
	public final String error;
	
	public ErrorReply(long messageID, String error) {
		super(messageID);
		this.error = error;
	}

}
