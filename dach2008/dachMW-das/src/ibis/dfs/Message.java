package ibis.dfs;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = -7502851585312848374L;
	
	public final long messageID;
	
	public Message(final long messageID) {
		super();
		this.messageID = messageID;
	}
}
