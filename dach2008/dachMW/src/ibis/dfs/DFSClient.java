package ibis.dfs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import dach.DACHJob;
import dach.DACHResult;
import dach.MiscUtils;

import ibis.ipl.IbisIdentifier;
import ibis.simpleComm.ReceivePipe;
import ibis.simpleComm.SimpleCommunication;
import ibis.simpleComm.Upcall;

public class DFSClient implements Upcall {
	
	private static final int DEFAULT_LOOKUP_TIMEOUT = 60000;

	private static final int DEFAULT_REPLY_TIMEOUT = 65000;
	private static final int DEFAULT_NEXT_MESSAGE_TIMEOUT = 30000;
	
	private static final Logger logger = Logger.getLogger("ibis.dfs.client");

	// Local copy operation executable
	public static String cpExec = "/bin/cp";
	
	private final SimpleCommunication comm;

	private long requestID;
	private IbisIdentifier nameService;
	
	private final String node;
	private final String location;
	
	private HashMap<Long, Object> replies = new HashMap<Long, Object>();
	
	public DFSClient(String node, String location) throws Exception { 
		
		this.node = node;
		this.location = location;
		
		comm = SimpleCommunication.get("DACH", this);
		
		nameService = comm.getElectionResult("DFSNameService", DEFAULT_LOOKUP_TIMEOUT);
		
		if (nameService == null) { 
			throw new Exception("Failed to find the DFSNameService!");
		}
	}
		
	public DFSClient(String node, String location, String serverAddress, String hubAddresses, String pool) throws Exception { 
		this.node = node;
		this.location = location;
		
		comm = SimpleCommunication.create("DACH", this, serverAddress, hubAddresses, pool);
		
		nameService = comm.getElectionResult("DFSNameService", DEFAULT_LOOKUP_TIMEOUT);
		
		if (nameService == null) { 
			throw new Exception("Failed to find the DFSNameService!");
		}
	}

	public boolean copy(String path, FileInfo info, String localFile, StringBuilder out, boolean local) { 
		if (local) {
			// This is a local file!
			return localCopy(path, info, localFile, out);
		} else {
			return remoteCopy(path, info, localFile, out);
		}
	}
	
	private boolean remoteCopy(String path, FileInfo info, String localFile, StringBuilder out) { 
			
		long start = System.currentTimeMillis();
		
		logger.warn("Copying remote file: " + path + " to local file " + localFile);
		
		Set<String> replicas = info.replicaHosts;
		
		boolean success = false;
	
		LinkedList<String> selection = new LinkedList<String>(replicas);
		Collections.shuffle(selection);
		
		for (String replica : selection) { 
			try { 
				get(replica, path, false, localFile);
				success = true;
				break;
			} catch (Exception e) {
				logger.warn("Failed to copy file " + path + " from replica " + replica, e);
			}
		}
		
		long end = System.currentTimeMillis();
		
		if (!success) { 
			String error = "Failed to remotely copy file " + path + " in " 
				+ (end-start) + "ms. from any of its replicas: " + replicas;
			logger.warn(error);
			out.append(error);
			return false;
		}		
		
		File f = new File(localFile);
		
		if (!f.exists() || !f.canRead() || (f.length() != info.size)) { 
			String error = "Remotely copy file " + path + " was corrupted!" + (end-start);
			logger.warn(error);
			out.append(error);
			return false;
		}	
		
		String message = "Remote copying " + path + " took " + (end-start) + " ms.";
		logger.info(message);
		out.append(message);
		return true;		
	}
	
	private boolean localCopy(String path, FileInfo info, String localFile, StringBuilder out) { 
		
		long start = System.currentTimeMillis();
		
		logger.warn("Copying local file: " + path + " to local file " + localFile);
		
		if (!MiscUtils.fileExists(path)) { 
			logger.warn("Input file " + path + " not found\n");
			return false;
		}
		
		StringBuilder stdout = new StringBuilder();
		StringBuilder stderr = new StringBuilder();

		int exit = MiscUtils.run(new String [] { cpExec, path, localFile }, stdout, stderr); 

		if (exit != 0) {
			String error = "Failed to locally copy file " + path + " (stdout: " 
				+ stdout + ") (stderr: " + stderr + ")\n";
			logger.warn(error);
			out.append(error);
			return false;
		}
		
		long end = System.currentTimeMillis();
		
		String message = "Local copying " + path + " took " + (end-start) + " ms.";
		
		logger.info(message);
		out.append(message);
		return true;		
	}
	
	private synchronized long getRequestID() { 
		return requestID++;
	}
	
	private void get(IbisIdentifier server, String path, boolean relative, String localFile) throws Exception {
		
		long start = System.currentTimeMillis();
		
		long bytes = 0;
		
		BufferedOutputStream out = null;
		ReceivePipe pipe = null;
		
		try { 
			out = new BufferedOutputStream(new FileOutputStream(localFile));
			pipe = comm.createReceivePipe();
		
			if (pipe == null) { 
				throw new Exception("Failed to create receive pipe!");
			}
		
			long ID = getRequestID();
		
			GetRequest req = new GetRequest(ID, relative, path, 0, 0, pipe.getID());
		
			registerSingleRequest(ID);
		
			if (!comm.send(server, req)) { 
				clearRequest(ID);
				throw new Exception("Failed to send get request to server " + server);
			}
		
			Object reply = waitForReply(ID, DEFAULT_REPLY_TIMEOUT);
		
			if (reply == null) { 
				clearRequest(ID);
				throw new Exception("Server failed to reply to get request: " + server);
			}
		
			long fileSize = -1; 
			int bufferSize = -1;
			
			if (reply instanceof GetReply) { 
				GetReply tmp = ((GetReply) reply);
				fileSize = tmp.fileSize;
				bufferSize = tmp.bufferSize;
				
				logger.info("Server " + server + " will send " + path + " (" + fileSize 
						+ ") using " + bufferSize + " byte buffers");
				
			} else if (reply instanceof ErrorReply) { 
				throw new Exception("Server " + server + " returned error to get request (" 
						+ path + "): " + ((ErrorReply)reply).error);
			}
		
			if (!pipe.waitForConnection(DEFAULT_REPLY_TIMEOUT)) { 
				// no reply
				logger.warn("Failed to get pipe connection from server " + server + " for file " + path);
				pipe.close();
				return;
			}
			
			byte [] buffer = new byte[bufferSize];

			while (bytes != fileSize) { 

				int read = pipe.receive(buffer);

				if (read == -1) { 
					throw new Exception("Failed to read all data from server!");	
				}

				if (read > 0) { 
					out.write(buffer, 0, read);
				}

				bytes += read;
			}
		} finally {	
			if (out != null) { 
				try { 
					out.close();
				} catch (Exception e) {
					// ignore
				}
			}
			
			if (pipe != null) { 
				pipe.close();
			}
		}

		long end = System.currentTimeMillis();
		
		long mbit = (bytes*8) / ((end-start) * 1000);
		
		logger.info("File transfer took " + (end-start) + " ms for " 
				+ bytes + " bytes (" + mbit + " MBit/s)");
	}
	
	private void get(String server, String path, boolean relative, String localFile) throws Exception {
		
		long start = System.currentTimeMillis();
		
		IbisIdentifier id = getServer(server);
		
		if (id == null) { 
			throw new Exception("Failed to find server " + server);
		}
		
		long end = System.currentTimeMillis();
		
		logger.info("Lookup took " + (end-start) + " ms.");
		
		get(id, path, relative, localFile);
	}
	
	
	private List<FileInfo> list(IbisIdentifier server, String path, boolean relative) throws Exception { 
		
		long ID = getRequestID();
		
		ListRequest req = new ListRequest(ID, relative, new String [] { path });
		
		registerSingleRequest(ID);
		
		if (!comm.send(server, req)) { 
			clearRequest(ID);
			throw new Exception("Failed to send list request to server " + server);
		}
		
		Message m = (Message) waitForReply(ID, DEFAULT_REPLY_TIMEOUT);

		if (m == null) { 
			throw new Exception("Failed to get list reply from server");
		}

		if (m instanceof ListReply) {

			ListReply reply = (ListReply) m;


			if (!reply.hasResult()) { 
				return null;
			}

			if (reply.isFile()) { 
				LinkedList<FileInfo> tmp = new LinkedList<FileInfo>();
				tmp.add(reply.getSingleFileInfo());
				return tmp;
			} else { 
				return reply.getDirectoryInfo();
			}
			
		} else if (m instanceof ErrorReply) { 
			throw new Exception("Got error reply from the DFSNameService: " + ((ErrorReply) m).error);
		} else { 
			throw new Exception("Got unknown reply from the DFSNameService: " + m);
		}
	}
	
	private synchronized Object waitForReply(long ID, int timeout) {
		
		long end = System.currentTimeMillis() + timeout;
		
		Object tmp = replies.get(ID);
		
		while (tmp == null) { 
			
			long timeLeft = end - System.currentTimeMillis(); 
			
			if (timeLeft <= 0) { 
				break;
			}
			
			try { 
				wait(timeLeft);
			} catch (InterruptedException e) {
				// ignore
			}
		
			tmp = replies.get(ID);
		}
		
		return replies.remove(ID);
	}

	@SuppressWarnings("unchecked")
	private synchronized Object waitForMultiReply(long ID, int timeout) {
		
		long end = System.currentTimeMillis() + timeout;
	
		LinkedList<Message> tmp = (LinkedList<Message>) replies.get(ID);
		
		while (tmp.size() == 0) { 
			
			long timeLeft = end - System.currentTimeMillis(); 
			
			if (timeLeft <= 0) { 
				break;
			}
			
			try { 
				wait(timeLeft);
			} catch (InterruptedException e) {
				// ignore
			}
		}

		if (tmp.size() > 0) { 
			return tmp.removeFirst();
		}
		
		return null;
	}
	

	private synchronized void clearRequest(long ID) {
		replies.remove(ID);
	}

	private synchronized void registerSingleRequest(long ID) {
		logger.info("Register single req " + ID);
		replies.put(ID, null);
	}
	
	private synchronized void registerMultiRequest(long ID) {
		logger.info("Register multi req " + ID);
		replies.put(ID, new LinkedList<Message>());
	}

	@SuppressWarnings("unchecked")
	private synchronized void storeReply(long ID, Message reply) {
		
		if (!replies.containsKey(ID)) { 
			logger.warn("No one is waiting for reply " + ID + " " + reply);
			return;
		}
		
		LinkedList<Message> tmp = (LinkedList<Message>) replies.get(ID);
		
		if (tmp == null) { 
			// single reply
			replies.put(ID, reply);
		} else { 
			// multi reply
			tmp.addLast(reply);
		}
			
		notifyAll();
	}

	private IbisIdentifier getServer(String server) { 
		
		long ID = getRequestID();
		
		registerSingleRequest(ID);
		
		if (!comm.send(nameService, new LookupHost(ID, server))) { 
			clearRequest(ID);
			logger.warn("Failed to communicate with the DFSNameService!");
			return null;
		}
		
		Message m = (Message) waitForReply(ID, 15000);
	
		if (m == null) { 
			logger.warn("Failed to get reply from the DFSNameService!");
			return null;
		}
		
		if (m instanceof LookupReply) { 
			LookupReply reply = (LookupReply) m;
			return reply.id;
		} else if (m instanceof ErrorReply) { 
			logger.warn("Got error reply from the DFSNameService: " + ((ErrorReply) m).error);
			return null;
		} else { 
			logger.warn("Got unknown reply from the DFSNameService: " + m);
			return null;
		}
	}
	
	public List<FileInfo> list(String server, String path, boolean relative) throws Exception { 
		
		IbisIdentifier id = getServer(server);
		
		if (id == null) { 
			throw new Exception("Failed to find server \"" + server + "\"");
		}
		
		return list(id, path, relative);
	}

	public boolean upcall(IbisIdentifier src, Object o) {
		
		if (o instanceof Message) { 
			Message m = (Message) o;
			storeReply(m.messageID, m);
			return true;
		} 
		
		return false;
	}
	
	public void died(IbisIdentifier src) { 
		// ignore
	}
	
	public static void main(String [] args) { 
		
		String action = null;
		String file = null;
		String target = null;
		String localFile = null;
		
		String node = null;
		String location = null;
		
		String serverAddress = null;
		String hubAddresses = null;
		String pool = null;
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-list") && i < args.length-2) { 
				action = "list";
				target = args[++i].trim();
				file = args[++i].trim();
			} else if (args[i].equals("-get") && i < args.length-3) { 
				action = "get";
				target = args[++i].trim();
				file = args[++i].trim();
				localFile = args[++i].trim();
			} else if (args[i].equals("-server") && i < args.length-1) { 
				serverAddress = args[++i].trim();
			} else if (args[i].equals("-hubs") && i < args.length-1) { 
				hubAddresses = args[++i].trim();
			} else if (args[i].equals("-pool") && i < args.length-1) { 
				pool = args[++i].trim();
			} else { 
				logger.error("Unknown or incomplete option: " + args[i]);
				System.exit(1);
			}
		}

		if (action == null || action.length() == 0) {
			logger.error("No command supplied!");
			System.exit(1);
		}
		
		if (serverAddress == null || serverAddress.length() == 0) {
			logger.error("ServerAddress not set!");
			System.exit(1);
		}
		
		if (node == null || node.length() == 0) {
			logger.error("Node not set!");
			System.exit(1);
		}
		
		if (location == null || location.length() == 0) {
			logger.error("Location not set!");
			System.exit(1);
		}

		if (pool == null || pool.length() == 0) {
			logger.error("Pool not set!");
			System.exit(1);
		}
		
		boolean relative = false;
		
		if (file.startsWith("." + File.separator)) { 
			relative = true;
			file = file.substring(1);
		} else if (!file.startsWith(File.separator)) { 
			relative = true;
			file = File.separator + file;
		}
		
		try {
			DFSClient c = new DFSClient(node, location, serverAddress, hubAddresses, pool);
			
			if (action.equals("list")) { 
				List<FileInfo> result = c.list(target, file, relative);
				
				if (result == null) { 
					System.out.println("<empty>");
				} else { 
					for (FileInfo i : result) { 
						System.out.println(i.toString());
					}
				}
			} else if (action.equals("get")) { 
				c.get(target, file, relative, localFile);
			} else { 
				logger.warn("Unknown action!");
			}
			
		} catch (Exception e) {
			logger.error("Failed to start DFSServer", e);
		}		
	}
}
