package ibis.dfs;

import java.util.HashMap;

import org.apache.log4j.Logger;

import ibis.ipl.IbisIdentifier;
import ibis.simpleComm.SimpleCommunication;
import ibis.simpleComm.Upcall;

public class DFSLocationService implements Upcall {
	
	private static final Logger logger = Logger.getLogger("ibis.dfs.nameservice");
	
	private final SimpleCommunication comm;
	
	private final HashMap<String, IbisIdentifier> table = new HashMap<String, IbisIdentifier>();
	
	public DFSLocationService() throws Exception { 
		comm = SimpleCommunication.get("DACH", this);
		
		IbisIdentifier tmp = comm.elect("DFSNameService", 60000);
		
		if (tmp == null || !tmp.equals(comm.getIdentifier())) { 
			throw new Exception("Failed to elect myself as the DFSNameService!");
		}		
	}
	
	public DFSLocationService(String serverAddress, String hubAddresses, String pool) throws Exception {
		comm = SimpleCommunication.create("DACH", this, serverAddress, hubAddresses, pool);
		
		IbisIdentifier tmp = comm.elect("DFSNameService", 60000);
		
		if (tmp == null || !tmp.equals(comm.getIdentifier())) { 
			throw new Exception("Failed to elect myself as the DFSNameService!");
		}		
	}
	
	public boolean upcall(IbisIdentifier src, Object o) { 
		
		if (o instanceof RegisterHost) { 
			handleRegisterHost(src, (RegisterHost) o);
			return true;
		} else if (o instanceof LookupHost) {
			handleLookupHost(src, (LookupHost) o);
			return true;
		}
		
		return false;
	}
	
	public void died(IbisIdentifier src) { 
		// ignore
	}
	
	private void handleLookupHost(IbisIdentifier src, LookupHost lookup) {
		
		LookupReply reply = null;
		
		synchronized (this) { 

			IbisIdentifier id = table.get(lookup.hostname);

			if (id == null) { 
				logger.warn("Lookup failed: " + lookup.hostname);
				reply = new LookupReply(lookup.messageID, lookup.hostname, null);
			} else { 
				reply = new LookupReply(lookup.messageID, lookup.hostname, id);
				logger.info("Lookup succeeded: " + lookup.hostname + " " + id);
			}
		}
		
		try { 
			comm.send(src, reply);
		} catch (Exception e) {
			logger.warn("Failed to return lookup reply to " + src);	
		}
	}

	private synchronized void handleRegisterHost(IbisIdentifier src, RegisterHost host) {
	
		IbisIdentifier old = table.remove(host.hostname);
		
		if (old != null) { 
			logger.warn("Overriding exisiting table entry: " + host.hostname + " " + old + " -> " + src);
		} else { 
			logger.info("Adding new table entry: " + host.hostname + " " + src);
		}
		
		table.put(host.hostname, src);
	}
	
	private void start() {
		
		while (true) { 
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore
			}
			
			logger.info("NameService table: " + table);
		}
	}
	
	public static void main(String [] args) { 
		
		String serverAddress = null;
		String hubAddresses = null;
		String pool = null;
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-server") && i < args.length-1) { 
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
		
		if (serverAddress == null || serverAddress.length() == 0) {
			logger.error("ServerAddress not set!");
			System.exit(1);
		}
		
		if (pool == null || pool.length() == 0) {
			logger.error("Pool not set!");
			System.exit(1);
		}
		
		try {
			new DFSLocationService(serverAddress, hubAddresses, pool).start();
		} catch (Exception e) {
			logger.error("Failed to start DFSServer", e);
		}		
	}
}
