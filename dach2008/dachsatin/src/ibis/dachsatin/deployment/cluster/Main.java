package ibis.dachsatin.deployment.cluster;

import ibis.dachsatin.deployment.util.Cluster;
import ibis.dachsatin.deployment.util.JobController;
import ibis.dachsatin.deployment.util.JobHandler;
import ibis.dachsatin.deployment.util.Problem;
import ibis.server.Server;
import ibis.server.ServerProperties;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.resources.JavaSoftwareDescription;
import org.gridlab.gat.resources.JobDescription;

public class Main {

	// These are set (or overwitten) by command line arguments
	private static int submitThreads = 15;
	
	private static String cluster = null;    	
	private static String pool = null;
	private static String serverAddress = null;
	
	private static String exec = "/home/dach/finder/dach.sh";
	private static String copy = "/bin/cp";
	private static String location = "/data/local/gfarm_v2/bin/gfwhere";
	
	private static String java = "/usr/local/jdk/bin/java";
	private static String localID = null;
	private static String homeDir = "/home/dach004";
	private static String dataDir = "/tmp/dach004/dfs";
	private static String tmpDir = "/tmp/dach004";
	
	private static String hubs = null;
	
	private static int dryRun = -1;
	
	// These are generated once
	private static String [] arguments = null;
	private static String classpath = null;
	
	private static String outputDir = null;
	private static String targetFile = null;
	
	// This contains target hosts (usually listed in a file)
	private static Cluster targets; 
	
	// This contains all the problem IDs (listed on the command line)
	private static LinkedList<Problem> problems = new LinkedList<Problem>(); 
	
	// This will be created once the required number of threads (or jobs) is known.
	private static JobController controller;
	
	private static Server server;
	//private static int serverPort = 5678;
	
	private static long start;
	
	private static String getOuputDir() { 
				
		System.out.println("Creating output dir: " + homeDir + File.separator + "output");
		
		File tmp = new File(homeDir + File.separator + "output");

		boolean ok = tmp.mkdirs();
		
		System.out.println("Creating dir result: " + ok);

		if (!tmp.exists()) { 
			return null;
		}

		return tmp.getPath();
	}
	
	private static String getClassPath() {
		
		if (classpath == null) { 
			classpath = homeDir +  "/lib/*:" + homeDir + "/lib/ibis/*:" + homeDir;
		} 

		return classpath;
	}
	
	private static HashMap<String, String> getProperties(String ID, String host) { 
	
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("ibis.server.address", serverAddress);  
		properties.put("ibis.hub.addresses", hubs);  
		properties.put("ibis.pool.name", pool); 
		properties.put("dach.executable", exec); 
		properties.put("dach.location", location); 
		properties.put("dach.copy", copy); 
		properties.put("dach.dir.output", outputDir);
		properties.put("dach.dir.data", dataDir);
		properties.put("dach.dir.tmp", tmpDir);
		properties.put("dach.machine.id", ID);
		properties.put("dach.host", host);
		properties.put("satin.alg", "MW");
		properties.put("satin.lazyMaster", "true");
			
		properties.put("log4j.configuration", "file:" + homeDir	+ File.separator + "log4j.properties");
		
		return properties;
	}
	
	private static String [] getArguments() { 

		if (arguments == null) { 
			ArrayList<String> args = new ArrayList<String>();

			if (dryRun > 0) { 
				args.add("-dryRun");
				args.add(Integer.toString(dryRun-1));
			}
	
			args.add("-v");

			for (Problem p : problems) { 
				args.add("-p");
				args.add(p.ID);
				args.add(p.directory);
			}

			arguments = args.toArray(new String [args.size()]);
		}
		
		return arguments;
	}
		
	/*
	private static JobDescription createJobDescription() { 
		 
		SoftwareDescription sd = new SoftwareDescription();
	    sd.setExecutable("/bin/hostname");
	        File stdout = GAT.createFile("hostname.txt");
	        sd.setStdout(stdout);

	        JobDescription jd = new JobDescription(sd);
	        ResourceBroker broker = GAT.createResourceBroker(new URI(args[0]));
	        Job job = broker.submitJob(jd);

	        while ((job.getState() != JobState.STOPPED)
	                && (job.getState() != JobState.SUBMISSION_ERROR)) {
	            Thread.sleep(1000);
	        }
	}
	
	
	private void start() { 
		
		
		
		
		
	}
	*/
	
	private static int jobNo = 0;
	
	private static String getID(String target) { 
		
		if (target == null) { 
			return "w." + jobNo++;			
		}
		
		target = target.trim();
		
		int index = target.indexOf(".");
		
		if (index <= 0) { 
			return "w." + jobNo++ + "." + target;
		}
		
		return "w." + jobNo++ + "." + target.substring(0, index); 
	}
	
	private static String getDomain(String host) { 
		
		if (host == null) { 
			return "unknown";
		}

		host = host.trim();

		int index = host.indexOf('.');

		if (index <= 0) { 
			return "unknown";
		}

		return host.substring(index+1);
	}
	
	private static void submit(String target) throws GATObjectCreationException, URISyntaxException { 
		
		String ID = localID + "." + getID(target);
	
		System.out.println("Submitting " + ID + " to " + target);
		
		//SoftwareDescription sd = new SoftwareDescription();
	    
		JavaSoftwareDescription sd = new JavaSoftwareDescription();
		
		sd.setJavaMain("ibis.dachsatin.worker.Main");
		sd.setJavaSystemProperties(getProperties(ID, target));
		sd.setJavaArguments(getArguments());
		sd.setJavaClassPath(getClassPath());
		
		sd.setExecutable(java);
		
		sd.addAttribute("sandbox.root", homeDir);
		sd.addAttribute("sandbox.useroot", "true");
		sd.addAttribute("sandbox.delete", "false");
		
		JobDescription jd = new JobDescription(sd);
        
        JobHandler h = new JobHandler(controller, ID, jd, new URI("any://" + target), outputDir);

        if (dryRun == 0) { 
        	System.err.println("DryRun -- NOT submitting job " + ID + " to " + target);
        } else { 
        	controller.addJobToSubmit(h);
        }
	}
	
	private static void createHub(String knownHubs) { 
		
		 Properties properties = new Properties();

		 properties.put(ServerProperties.PORT, "0");
		 properties.setProperty(ServerProperties.START_HUB, "true");
		 properties.setProperty(ServerProperties.HUB_ONLY, "true");
		 properties.setProperty(ServerProperties.HUB_ADDRESSES, knownHubs);
		 //properties.setProperty(ServerProperties.HUB_ADDRESS_FILE, file);
		 
		 properties.setProperty(ServerProperties.PRINT_EVENTS, "true");
	     properties.setProperty(ServerProperties.PRINT_ERRORS, "true");
	     properties.setProperty(ServerProperties.PRINT_STATS, "true");
	     // properties.setProperty(ServerProperties.REMOTE, "true");

	     try {
	    	 server = new Server(properties);
	     } catch (Throwable t) {
	    	 System.err.println("Could not start Ibis Hub: " + t);
	    	 System.exit(0);
	     }
	     
	     System.out.println("Create Ibis hub on: " + server.getLocalAddress());
	     
	     String [] hubs = server.getHubs();
	     
	     String tmp = null;
	     
	     if (hubs != null && hubs.length > 0) { 
	    	 tmp = hubs[0];
			
	    	 for (int h=1;h<hubs.length;h++) { 
	    		 tmp += "," + hubs[h];
	    	 }
	     }
	     
	     if (tmp != null && tmp.length() > 0) { 
	    	 Main.hubs = tmp;
	     }
	     
	     System.out.println("List of known hubs: " + Main.hubs);
	}
	
	private static int time() { 
		return (int)((System.currentTimeMillis() - start) / 1000);
	}
	
    public static void main(String[] args) {
    	
    	start = System.currentTimeMillis();
    	
    	for (int i=0;i<args.length;i++) { 
    		
    		if (args[i].equals("-dryRun") && i != args.length-1) { 
    			dryRun = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-id") && i != args.length-1) { 
    			localID = args[++i];
    		} else if (args[i].equals("-copy") && i != args.length-1) { 
    			copy = args[++i];
    		} else if (args[i].equals("-exec") && i != args.length-1) { 
    			exec = args[++i];
    		} else if (args[i].equals("-java") && i != args.length - 1) {
				java = args[++i];
			} else if (args[i].equals("-pool") && i != args.length-1) { 
    			pool = args[++i];
    		} else if (args[i].equals("-cluster") && i != args.length-1) { 
    			cluster = args[++i];
    		} else if (args[i].equals("-server") && i != args.length-1) { 
    			serverAddress = args[++i];
    		} else if (args[i].equals("-home") && i != args.length-1) { 
    			homeDir = args[++i];    	
    		} else if (args[i].equals("-hubs") && i != args.length-1) { 
    			hubs = args[++i];    	
    		} else if (args[i].equals("-threads") && i != args.length-1) { 
    			submitThreads = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-problem") && i != args.length-2) {
    			String ID = args[++i];
    			String dir = args[++i];
    			problems.add(new Problem(ID, dir, null));
    		} else if (args[i].equals("-targets") && i != args.length-1) {
    			targetFile = args[++i];
    		} else { 
    			System.err.println("Unknown or incomplete option: " + args[i]);
    			System.exit(0);
    		}
    	}
    	
    	if (pool == null) { 
    		System.err.println("Ibis pool not set!");
    		System.exit(0);
    	}
    	
    	if (serverAddress == null) { 
    		System.err.println("Ibis server not set!");
    		System.exit(0);
    	}
    	
    	if (cluster == null) { 
    		System.err.println("Ibis cluster not set!");
    		System.exit(0);
    	}
    
    	if (localID == null) { 
    		System.err.println("Local ID not set!");
    		System.exit(0);
    	}
    	
    	if (copy == null) { 
    		System.err.println("Copy executable not set!");
    		System.exit(0);
    	}
    
    	if (exec == null) { 
    		System.err.println("DACH executable not set!");
    		System.exit(0);
    	}
    	
    	if (java == null) {
			System.err.println("Java executable not set!");
			System.exit(0);
		}
    
    	if (problems.size() == 0) { 
    		System.err.println("No problems specified!");
    		System.exit(0);
    	}
    	
    	if (targetFile == null) { 
    		System.err.println("No target file specified!");
    		System.exit(0);
    	}
    	
    	if (hubs == null) { 
    		System.err.println("No hubs specified!");
    		System.exit(0);
    	}
    	
    	try { 
    		targets = Cluster.read(targetFile);
    	} catch (Exception e) { 
    		System.err.println("Failed to read target file " + targetFile);
    		e.printStackTrace(System.err);
    		System.exit(0);
    	}
    	
    	outputDir = getOuputDir();
    	
    	if (outputDir == null) { 
    		System.err.println("Failed to create output dir!");
    		System.exit(0);
    	}
    	
    	createHub(hubs);
    	
    	if (submitThreads > targets.nodes.size()) { 
    		submitThreads = targets.nodes.size();
    	}
    
    	controller = new JobController(submitThreads);
    	
    	System.out.println(time() + ": Starting application on " + targets.nodes.size() + " nodes: ");
    	
    	for (String node : targets.nodes) { 
    		
    		System.out.println("   " + node);
    		
    		try {
				submit(node);
			} catch (GATObjectCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
    		System.out.flush();
    	}
    	
    	System.out.println(time() + ": All submissions done, waiting ....");
    	
    	controller.waitUntilDone();		
    	
    	System.out.println(time() + ": Application seems to be finished!");
    	
    	System.out.println(time() + ": Killing server");
		
		server.end(10000);
		
		System.out.println(time() + ": Doing exit");

		System.exit(0);
    }

}
