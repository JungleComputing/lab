package ibis.dfs.tests;

import ibis.dfs.DFSClient;
import ibis.dfs.FileInfo;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;


public class DFSTest {

	private static final Logger logger = Logger.getLogger("ibis.dfs.test");
		
	private DFSClient client;
	
	public DFSTest( String node, String location, String serverAddress, String hubAddresses, String pool) throws Exception { 
		client = new DFSClient(node, location, serverAddress, hubAddresses, pool);
	}

	public void start(String server, String path, String localFile, boolean relative, int rank, int nodes, int repeat) throws Exception {
		
		List<FileInfo> tmp = client.list(server, path, relative);
		
		if (tmp.size() == 0) { 
			logger.warn("No files found!");
			return;
		}
		
		if (tmp.size() < rank) { 
			logger.warn("No enough files found!");
			return;
		}
		
		FileInfo [] info = tmp.toArray(new FileInfo[tmp.size()]); 
		
		StringBuilder out = new StringBuilder();
		
		for (int i=0;i<repeat;i++) { 

			int index = rank;
			
			while (index < info.length) { 
				
				FileInfo f = info[index];
				
				long start = System.currentTimeMillis();
				
				boolean result = client.copy(path + File.separator + f.name, 
						f, localFile, out, false);
				
				long end = System.currentTimeMillis();
				
				if (result) { 
					long mbit = (f.size*8) / ((end-start) * 1000);
					
					System.out.println("File transfer took " + (end-start) + " ms for " 
							+ f.size + " bytes (" + mbit + " MBit/s)");					
				} else { 
					System.out.println("Failed to copy file! " + out);
				}
				
				index += nodes;
			}
		}
	}
	
	public static void main(String [] args) { 

		String path = null;
		String fileServer = null;		
		String localFile = null;

		String node = null;
		String location = null;

		String serverAddress = null;
		String hubAddresses = null;
		String pool = null;

		int rank = 0;
		int size = 1;
		int repeat = 1;
		
		for (int i=0;i<args.length;i++) { 
			if (args[i].equals("-ibisserver") && i < args.length-1) { 
				serverAddress = args[++i].trim();
			} else if (args[i].equals("-fileserver") && i < args.length-1) { 
				fileServer = args[++i].trim();
			} else if (args[i].equals("-path") && i < args.length-1) { 
				path = args[++i].trim();
			} else if (args[i].equals("-node") && i < args.length-1) {
				node = args[++i].trim();
			} else if (args[i].equals("-location") && i < args.length-1) { 
				location = args[++i].trim();
			} else if (args[i].equals("-localFile") && i < args.length-1) { 
				localFile = args[++i].trim();			
			} else if (args[i].equals("-hubs") && i < args.length-1) { 
				hubAddresses = args[++i].trim();
			} else if (args[i].equals("-rank") && i < args.length-1) { 
				rank = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-size") && i < args.length-1) { 
				size = Integer.parseInt(args[++i]);				
			} else if (args[i].equals("-pool") && i < args.length-1) { 
				pool = args[++i].trim();
			} else { 
				logger.error("Unknown or incomplete option: " + args[i]);
				System.exit(1);
			}
		}

		if (path == null || path.length() == 0) {
			logger.error("No path supplied!");
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

		if (localFile == null || localFile.length() == 0) {
			logger.error("LocalFile not set!");
			System.exit(1);
		}
		
		if (pool == null || pool.length() == 0) {
			logger.error("Pool not set!");
			System.exit(1);
		}

		try {
			DFSTest test = new DFSTest(node, location, serverAddress, hubAddresses, pool);
			test.start(fileServer, path, localFile, false, rank, size, repeat);
		} catch (Exception e) {
			logger.error("Failed to start DFSServer", e);
		}		
	}
}