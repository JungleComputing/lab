package ibis.simpleComm;

import ibis.server.Server;
import ibis.server.ServerProperties;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class SimpleServer {

	private static final Logger logger = Logger.getLogger("ibis.simpleComm");
	
	private Server server;
	
	public SimpleServer() throws Exception { 
		this(null);
	}

	public SimpleServer(String hubAddresses, boolean hubonly) throws Exception { 

		logger.info("Creating ibis server (" + (hubonly ? "hubonly)" : "server+hub)"));
		
		Properties properties = new Properties();

		if (isSet(hubAddresses)) { 
			properties.setProperty(ServerProperties.HUB_ADDRESSES, hubAddresses);
		}

		if (hubonly) { 
			properties.setProperty(ServerProperties.HUB_ONLY, "true");
		}
		
		properties.setProperty(ServerProperties.START_HUB, "true");	
		properties.setProperty(ServerProperties.PRINT_EVENTS, "true");
		properties.setProperty(ServerProperties.PRINT_ERRORS, "true");
		properties.setProperty(ServerProperties.PRINT_STATS, "true");
		properties.put(ServerProperties.PORT, "0");
	
		try {
			server = new Server(properties);
		} catch (Throwable t) {
			throw new Exception("Could not start Ibis Server: ", t);
		}

		logger.info("Created Ibis server on: " + server.getLocalAddress());

		getHubAddresses();

		logger.info("Known hubs: " + hubAddresses);
	}
	
	public SimpleServer(String hubAddresses) throws Exception { 
		this(hubAddresses, false);
	}
	
	private boolean isSet(String s) { 
		return (s != null && s.trim().length() > 0);
	}
	
	public String getServerAddress() {
		
		if (server == null) {
			return null;
		}
		
		return server.getLocalAddress();
	}
	
	public String getHubAddresses() { 

		if (server == null) { 
			return null;
		}

		String hubAddresses = null;
		
		// Update the hubs
		String [] tmp = server.getHubs();

		if (tmp != null && tmp.length > 0) { 
			hubAddresses = tmp[0];

			for (int i=1;i<tmp.length;i++) { 
				hubAddresses += "," + tmp[i];
			}
		}	

		return hubAddresses;
	}

	public static String generatePool() throws IOException { 
	
		File tmp1 = File.createTempFile("pool-", "", null);
		String uniqueID = tmp1.getName();
		tmp1.delete();

		logger.info("Generated pool name: " + uniqueID);
		return uniqueID;
	}
}
