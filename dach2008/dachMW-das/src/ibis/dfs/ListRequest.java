package ibis.dfs;

public class ListRequest extends Message {

	private static final long serialVersionUID = -8763258790089145025L;

	public final boolean relative;
	public final String [] locations;

	public ListRequest(final long messageID, final boolean relative, final String [] locations) {
		super(messageID);
		this.relative = relative;
		this.locations = locations;
	}
}
