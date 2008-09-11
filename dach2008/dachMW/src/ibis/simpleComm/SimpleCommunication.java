package ibis.simpleComm;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisProperties;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

public class SimpleCommunication implements MessageUpcall, ReceivePortConnectUpcall, RegistryEventHandler {

	private static final Logger logger = Logger.getLogger("ibis.simpleComm");

	public static final int DEFAULT_TIMEOUT = 30000;
	
	public static final int DEFAULT_CONNECT_TIMEOUT = 30000;
	
	private static final PortType singleObjectPortType =
		new PortType(
				PortType.COMMUNICATION_RELIABLE,
				PortType.SERIALIZATION_OBJECT,
				PortType.RECEIVE_AUTO_UPCALLS,
				PortType.CONNECTION_MANY_TO_ONE, 
				PortType.CONNECTION_UPCALLS);
	
	private static final PortType pipePortType =
		new PortType(
				PortType.COMMUNICATION_RELIABLE,
				PortType.SERIALIZATION_DATA,
				PortType.RECEIVE_EXPLICIT,
				PortType.RECEIVE_TIMEOUT,
				PortType.CONNECTION_ONE_TO_ONE,
				PortType.CONNECTION_UPCALLS);

	private static final IbisCapabilities ibisCapabilities = 
		new IbisCapabilities(
				IbisCapabilities.MALLEABLE,
				IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED, 
				IbisCapabilities.ELECTIONS_STRICT);

	private static final HashMap<String, SimpleCommunication> all = 
		new HashMap<String, SimpleCommunication>();
	
	private final LinkedList<Upcall> upcalls = new LinkedList<Upcall>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private Ibis ibis;
	private ReceivePort receive;
	private final Registry registry;

	private String serverAddress;
	private String hubAddresses;
	private String pool;
	
	private int pipeNo = 0;
	
	private Set<IbisIdentifier> participants = Collections.synchronizedSet(new HashSet<IbisIdentifier>());
	
	private class Connection { 	
		
		public final IbisIdentifier target;
		
		public SendPort sp;
		
		public Connection(final IbisIdentifier target) {
			super();
			this.target = target;
		}	

		private boolean connect() { 
			
			logger.warn("Connecting to " + target);
			
			try { 
				sp = ibis.createSendPort(singleObjectPortType);
			} catch (Exception e) {
				logger.warn("Failed to created sendport!", e);
				return false;
			}

			if (target != null) { 
				try {
					sp.connect(target, "rp", DEFAULT_TIMEOUT, true);
				} catch (ConnectionFailedException e) {
					logger.warn("Failed to connect to " + target, e);
					maybeDead(target);
					sp = null;
					return false;
				}
			}
			
			return true;
		}
		
		public synchronized boolean close() {
		
			if (sp == null) { 
				return true;
			}
			
			logger.warn("Closing connection to " + target);
			
			try { 
				sp.close();
			} catch (Exception e) {
				logger.warn("Failed to close connection to " + target, e);			
				return false;
			} finally { 
				sp = null;
			}
			
			return true;
		}
		
		public synchronized boolean send(Object data) { 
			
			if (sp == null && !connect()) { 
				return false;
			}
			
			WriteMessage wm = null;
			
			try { 
				wm = sp.newMessage();
				wm.writeObject(data);
				wm.finish();
				return true;
			} catch (IOException e) {
			
				logger.warn("Failed to send message to " + target, e);			
			
				if (wm != null) { 
					wm.finish(e);
				}
			
				close();
				maybeDead(target);
				return false;
			} 
		}
	}
	
	private final LinkedHashMap<IbisIdentifier, Connection> connectionCache = new LinkedHashMap<IbisIdentifier, Connection>();
	
	private SimpleCommunication(Upcall upcall, String serverAddress, 
			String hubAddresses, String pool) throws Exception { 
		// Constructor for the worker
		this.pool = pool;
		
		upcalls.add(upcall);
		
		if (!isSet(pool)) { 
			throw new Exception("Pool name not set!");
		}

		if (!isSet(serverAddress)) { 
			throw new Exception("Server address not set!");
		} else { 
			this.serverAddress = serverAddress;
		}

		this.hubAddresses = hubAddresses;

		logger.info("Creating SimpleCommunication");

		Properties properties = new Properties();
		properties.put("ibis.pool.name", pool); 

		if (hubAddresses != null && hubAddresses.trim().length() > 0) { 
			properties.setProperty(IbisProperties.HUB_ADDRESSES, hubAddresses);
		}

		properties.setProperty(IbisProperties.SERVER_ADDRESS, serverAddress);

		inititalizeIbis(properties);

		// Elect the master
		registry = ibis.registry();
		registry.enableEvents();
	}

	public void addUpcall(Upcall upcall) { 
		
		lock.writeLock().lock();
		upcalls.add(upcall);
		lock.writeLock().unlock();
	}
		
	public IbisIdentifier elect(String name, long timeout) { 
		try {
			return registry.elect(name);
		} catch (Exception e) {
			logger.warn("Election failed!", e);
			return null;
		}
	}
	
	public IbisIdentifier getElectionResult(String name, long timeout) { 
		try { 
			return registry.getElectionResult(name, timeout);
		} catch (Exception e) {
			logger.warn("GetElectionResult failed!", e);
			return null;
		}
	}
	
	
	
	private void inititalizeIbis(Properties p) throws IbisCreationFailedException, IOException { 
		// Create an Ibis
		ibis = IbisFactory.createIbis(ibisCapabilities, p, true, this, singleObjectPortType, pipePortType);

		logger.info("Ibis created!");

		// Create the receive port for the broker and switch it on. 
		receive = ibis.createReceivePort(singleObjectPortType, "rp", this, this, null);
		receive.enableConnections();
		receive.enableMessageUpcalls();

		// Install a shutdown hook that terminates ibis. 
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					logger.info("Communication layer terminating (SH)");
					receive.close(DEFAULT_TIMEOUT);
					ibis.end();
				} catch (IOException e) {
					// Ignored
				}
			}
		});
	} 

	public String getServerAddress() {
		return serverAddress;
	}
	
	public String getHubAddresses() { 
		return hubAddresses;
	}

	public String getPool() { 
		return pool;
	}	

	public IbisIdentifier getIdentifier() { 
		return ibis.identifier();
	}
	
	public boolean maybeDead(IbisIdentifier id) { 
		try {
			registry.maybeDead(id);
		} catch (IOException e) {
			logger.warn("Failed to send maybeDead to registry!", e);
			return false;
		}

		return true;
	}

	private synchronized Connection getConnection(IbisIdentifier target) { 

		Connection c = connectionCache.get(target);

		if (c == null) { 
			logger.warn("Creating connection to " + target);
			c = new Connection(target);
			connectionCache.put(target, c);
		}
		
		return c;	
	}
	
	private synchronized void closeAllConnections() {
		for (Connection c : connectionCache.values()) { 
			c.close();
		}
	}
	
	public boolean send(IbisIdentifier target, Object data) {
		
		Connection c = getConnection(target);
	
		if (c == null) { 
			logger.warn("Failed to get connection to " + target);
			return false;		
		}
		
		return c.send(data);
	}	

	private synchronized String getUniquePipeID() {
		return "pipe-" + pipeNo++;		
	}
	
	public ReceivePipe createReceivePipe() { 
		
		String pipeID = getUniquePipeID();
		ReceivePipe pipe = new ReceivePipe(pipeID);
		
		ReceivePort rp = null;
		
		try { 
			rp = ibis.createReceivePort(pipePortType, pipeID);
			pipe.setReceivePort(rp);
			rp.enableConnections();
		} catch (Exception e) {
			logger.warn("Failed to create receive pipe " + pipeID, e);
	
			if (rp != null) { 
				try {
					rp.close(DEFAULT_TIMEOUT);
				} catch (IOException e1) {
					// ignore ?
				}
			}	
		
			return null;
		}
		
		return pipe;
	}
	
	public SendPipe createSendPipe(IbisIdentifier target, String pipeID) { 
		
		SendPort sp = null; 
		
		try { 
			sp = ibis.createSendPort(pipePortType);
			sp.connect(target, pipeID, DEFAULT_CONNECT_TIMEOUT, true);
			return new SendPipe(sp, target, pipeID);
		} catch (Exception e) {
			
			logger.warn("Failed to create send pipe to " + target + " - " + pipeID, e);
			
			if (sp != null) { 
				try {
					sp.close();
				} catch (IOException e1) {
					// ignore ?
				}
			}	
		
			return null;
		}
	}
	
	
	public void upcall(ReadMessage rm) throws IOException, ClassNotFoundException {

		Object o = rm.readObject();
		IbisIdentifier src = rm.origin().ibisIdentifier();
		rm.finish();

		lock.readLock().lock();
		
		for (Upcall u : upcalls) {
			try { 
				if (u.upcall(src, o)) { 
					return;
				}
			} catch (Throwable e) { 
				logger.warn("Upcall produced exception!", e);
			}
		}
	
		lock.readLock().unlock();
		
		// We can only arrive here if no one knew how to handle this message!
		logger.warn("Received unknown message type from " + src + ": " + o);	
	}

	// ReceivePortConnectUpcall
	public boolean gotConnection(ReceivePort rp, SendPortIdentifier sp) {
		logger.trace("Got connection from : " + sp.ibisIdentifier());
		return true;
	}

	public void lostConnection(ReceivePort rp, SendPortIdentifier sp, Throwable e) {
		logger.trace("Lost connection from : " + sp.ibisIdentifier());			
	}

	public void died(IbisIdentifier id) {
		logger.info("Worker died " + id);		
		participants.remove(id);
	}

	public void electionResult(String name, IbisIdentifier id) {
		logger.info("Election result " + name + " " + id);		
	}

	public void gotSignal(String signal) {
		logger.info("Signal " + signal);	
	}

	public void joined(IbisIdentifier id) {
		logger.info("Worker joined " + id);		
		participants.add(id);
	}

	public void left(IbisIdentifier id) {		
		logger.info("Worker left " + id);		
		participants.remove(id);
	}

	public int participants() {
		return participants.size();
	}

	public void end(long timeout) { 

		logger.info("Communication layer terminating");
		
		logger.info("Closing sendports");
		closeAllConnections();

		logger.info("Closing receiveports");
		try {
			receive.close(timeout);
		} catch (IOException e) {
			logger.warn("Failed to close receiveport", e);
		}

		logger.info("Ending ibis");
		try {
			ibis.end();
		} catch (IOException e) {
			logger.warn("Failed to end ibis", e);
		}
	}
	
	private boolean isSet(String s) { 
		return (s != null && s.trim().length() > 0);
	}
	
	private boolean checkSettings(String serverAddress, String pool) {
		return this.serverAddress.equals(serverAddress) && this.pool.equals(pool);
	}	
	
	public static SimpleCommunication create(String name, Upcall upcall, String serverAddress, 
			String hubAddresses, String pool) throws Exception {
		
		SimpleCommunication tmp = all.get(name);
		
		if (tmp != null) { 
			
			logger.info("SimpleCommunication("+ name + ") already exists! -- checking settings...");
			
			if (tmp.checkSettings(serverAddress, pool)) { 
				logger.info("SimpleCommunication("+ name + ") settings match");
				tmp.addUpcall(upcall);
				return tmp;	
			}
			
			throw new Exception("SimpleCommunication("+ name + ") already exists, but settings do not match!");
		}
			
		tmp = new SimpleCommunication(upcall, serverAddress, hubAddresses, pool);
		
		all.put(name, tmp);
		
		return tmp;
	}

	public static SimpleCommunication get(String name, Upcall upcall) throws Exception {
		
		SimpleCommunication tmp = all.get(name);
		
		if (tmp == null) { 
			throw new Exception("SimpleCommunication("+ name + ") does not exists!");
		}
	
		tmp.addUpcall(upcall);
		
		return tmp;
	}
}

