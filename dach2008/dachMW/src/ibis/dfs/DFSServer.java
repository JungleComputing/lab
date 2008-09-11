package ibis.dfs;

import ibis.ipl.IbisIdentifier;
import ibis.simpleComm.SendPipe;
import ibis.simpleComm.SimpleCommunication;
import ibis.simpleComm.Upcall;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class DFSServer implements Upcall {

	private static int MAX_BUFFER_SIZE = 128*1024;
	
	private static final Logger logger = Logger.getLogger("ibis.dfs.server");
	
	private final SimpleCommunication comm;
	private final String root;
	private final String name;
	
	private boolean done = false;
	
	private IbisIdentifier nameservice;
	
	public DFSServer(String root, String name) throws Exception {
		this.root = root;
		this.name = name;
		comm = SimpleCommunication.get("DACH", this);
		
		nameservice = comm.getElectionResult("DFSNameService", 60000);
		
		if (nameservice == null) { 
			throw new Exception("Failed to find the DFSNameService!");
		}

		if (!comm.send(nameservice, new RegisterHost(0, name))) { 
			throw new Exception("Failed to register at the DFSNameService!");
		}
	}
	
	public DFSServer(String root, String name, String serverAddress, String hubAddresses, 
			String pool) throws Exception {
		this.root = root;
		this.name = name;
		
		comm = SimpleCommunication.create("DACH", this, serverAddress, hubAddresses, pool);
		
		nameservice = comm.getElectionResult("DFSNameService", 60000);
	
		if (nameservice == null) { 
			throw new Exception("Failed to find the DFSNameService!");
		}

		if (!comm.send(nameservice, new RegisterHost(0, name))) { 
			throw new Exception("Failed to register at the DFSNameService!");
		}
	}
	
	public boolean upcall(IbisIdentifier src, Object o) {
		
		if (o instanceof ListRequest) { 
			handleListRequest(src, (ListRequest) o);
			return true;
		} else if (o instanceof GetRequest) { 
			handleGetRequest(src, (GetRequest) o);
			return true;
		}

		return false;
	}

	public void died(IbisIdentifier src) { 
		
		// For easy termination ?
		if (src.equals(nameservice)) { 
			done();
		}
	}
	
	private void handleGetRequest(IbisIdentifier src, GetRequest request) {
		
		logger.info("Server got get request from " + src + " " + request.file + " " + request.relative);
		
		long start = System.currentTimeMillis();
		long bytes = 0;
		
		BufferedInputStream in = null;
		SendPipe pipe = null;
		File f = null;
		
		try { 
			if (request.relative) { 
				f = new File(root + File.separator + request.file);
			} else { 
				f = new File(request.file);
			}

			if (!f.exists() || !f.canRead() || !f.isFile()) { 
				if (!comm.send(src, new ErrorReply(request.messageID, "Cannot access " + f.getPath()))) { 
					logger.warn("Failed to return ErrorReply to " + src);
				}
				return;
			}

			try {
				in = new BufferedInputStream(new FileInputStream(f));
			} catch (FileNotFoundException e) {
				logger.warn("Failed to open file inputstream for " + f.getPath());
			
				if (!comm.send(src, new ErrorReply(request.messageID, "Cannot open " + f.getPath()))) { 
					logger.warn("Failed to return ErrorReply to " + src);
				}
				
				return;
			}
		
			pipe = comm.createSendPipe(src, request.pipeID);
		
			if (pipe == null) { 
				logger.warn("Failed to create sendPipe to " + src + " - " + request.pipeID);

				if (!comm.send(src, new ErrorReply(request.messageID, "Cannot create pipe " 
						+ request.pipeID + " for sending file " + f.getPath()))) { 
					logger.warn("Failed to return GetReply to " + src);
				}

				return;
			}
		
			long total = f.length();

			if (!comm.send(src, new GetReply(request.messageID, total, MAX_BUFFER_SIZE))) { 
				logger.warn("Failed to return GetReply to " + src);
				return;
			}
		
			byte [] buffer = new byte[MAX_BUFFER_SIZE];

			try { 
				while (bytes < total) { 
				
					int read = in.read(buffer, 0, MAX_BUFFER_SIZE);

					if (read == -1) { 
						logger.warn("Unexpected end of file: " + f.getPath());
						return;
					}

					pipe.send(buffer, 0, read);
					bytes += read;
				} 
			} catch (Exception e) {
				logger.warn("Failed to fully send " + f.getPath() + " to " + src + " - " + request.pipeID);
			}	
		
		} finally { 
			try {
				if (in != null) { 
					in.close();
				} 
			} catch (Exception e) {
				logger.warn("Failed to close inputstream: " + f);
			}
			
			if (pipe != null) { 
				pipe.close();
			}
		}
		
		long end = System.currentTimeMillis();
		
		long mbit = (bytes*8) / ((end-start) * 1000);
		
		logger.info("File transfer of " + f.getPath() + " took " 
				+ (end-start) + " ms for " + bytes + " bytes (" + mbit + " MBit/s)");
	}

	private List<FileInfo> getFiles(File dir) { 
		
		LinkedList<FileInfo> result = new LinkedList<FileInfo>();
		
		File [] tmp = dir.listFiles();
		
		for (File f : tmp) { 
			if (f.exists() && f.canRead() && f.isFile()) {
				FileInfo fi = new FileInfo(f.getName(), f.length());
				fi.addReplicaHost(name);	
				result.add(fi);
			}
		}
		
		return result;
	}
	
	private void handleListRequest(IbisIdentifier src, ListRequest request) {
	
		logger.info("Server got list request: " + request.messageID + " " + Arrays.toString(request.locations));
		
		Message reply = null;
		List<FileInfo> result = new LinkedList<FileInfo>();
		
		for (String location : request.locations) {
			File f = null;

			if (request.relative) { 
				f = new File(root + File.separator + location);
			} else { 
				f = new File(location);
			}
			
			if (!f.exists() || !f.canRead()) {
				reply = new ErrorReply(request.messageID, "Cannot access file " + f);
				break;
			} else if (f.isFile()) { 
				FileInfo tmp = new FileInfo(f.getName(), f.length());
				tmp.addReplicaHost(name);		
				result.add(tmp);
			} else if (f.isDirectory()) { 
				result.addAll(getFiles(f));
			} else { 
				// Can this ever happen ?
				reply = new ErrorReply(request.messageID, "Problem while accessing file " + f);
				break;
			}
		}
			
		if (reply == null) { 
			reply = new ListReply(request.messageID, result);
		}
		
		if (!comm.send(src, reply)) { 
			logger.warn("Failed to send ListReply to " + src);
		}
	}
	
	private synchronized void done() {
		done = true;
	}
	
	private synchronized boolean getDone() {
		return done;
	}
	
	private void start() {
		while (!getDone()) { 
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore
			}
			
			logger.info("Server alive!");
		}
	}
	
	public static void main(String [] args) { 
		
		String name = null;
		String cluster = null;
		String id = null;
		
		String root = null;
		String serverAddress = null;
		String hubAddresses = null;
		String pool = null;
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-maxbuf") && i < args.length-1) { 
				MAX_BUFFER_SIZE = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-node") && i < args.length-1) { 
				name = args[++i].trim();
			} else if (args[i].equals("-cluster") && i < args.length-1) { 
				cluster = args[++i].trim();
			} else if (args[i].equals("-di") && i < args.length-1) { 
				id = args[++i].trim();
			} else if (args[i].equals("-root") && i < args.length-1) { 
				root = args[++i].trim();
			} else if (args[i].equals("-server") && i < args.length-1) { 
				serverAddress = args[++i].trim();
			} else if (args[i].equals("-hubs") && i < args.length-1) { 
				hubAddresses = args[++i].trim();
			} else if (args[i].equals("-pool") && i < args.length-1) { 
				pool = args[++i].trim();
			} else { 
				logger.error("Unknown or incomplete option: " + args[i]);
			}
		}

		if (name == null || name.length() == 0) {
			logger.error("Name not set!");
			System.exit(1);
		}
		
		if (serverAddress == null || serverAddress.length() == 0) {
			logger.error("ServerAddress not set!");
			System.exit(1);
		}
		
		if (pool == null || pool.length() == 0) {
			logger.error("Pool not set!");
			System.exit(1);
		}
		
		if (root == null || root.length() == 0) {
			logger.error("Root directory not set!");
			System.exit(1);
		}

		try {
			new DFSServer(root, name, serverAddress, hubAddresses, pool).start();
		} catch (Exception e) {
			logger.error("Failed to start DFSServer", e);
		}		
	}
	
}
