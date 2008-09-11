package ibis.dfs;

public class RegisterHost extends Message {

	private static final long serialVersionUID = -8064880300618670603L;

	public final String hostname;

	public RegisterHost(final long messageID, final String hostname) {
		super(messageID);
		this.hostname = hostname;
	}
	
	
}
