package ibis.dfs;

import java.util.List;

public class ListReply extends Message {

	private static final long serialVersionUID = 9138473601553274368L;

	private FileInfo singleFileInfo;
	private List<FileInfo> directoryInfo;
	
	public ListReply(final long messageID) {
		// File does not exists or is not accessible
		super(messageID);
	}
	
	public ListReply(final long requestID, final List<FileInfo> directoryInfo) {
		// File is a directory
		this(requestID);
		this.directoryInfo = directoryInfo;
	}

	public ListReply(final long requestID, final FileInfo singleFileInfo) {
		// File is a single accessible file
		this(requestID);
		this.singleFileInfo = singleFileInfo;
	}

	public boolean hasResult() { 
		return singleFileInfo != null || directoryInfo != null;
	}
	
	public boolean isDirectory() { 
		return directoryInfo != null;
	}
	
	public boolean isFile() { 
		return singleFileInfo != null;
	}
	
	public List<FileInfo> getDirectoryInfo() {
		return directoryInfo;
	}

	public FileInfo getSingleFileInfo() {
		return singleFileInfo;
	}
}
