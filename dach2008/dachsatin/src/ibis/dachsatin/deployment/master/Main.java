package ibis.dachsatin.deployment.master;

import ibis.dachsatin.deployment.util.Cluster;
import ibis.dachsatin.deployment.util.JobController;
import ibis.dachsatin.deployment.util.JobHandler;
import ibis.dachsatin.deployment.util.Problem;
import ibis.dachsatin.util.FileUtils;
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

	private static String pool = null;
	private static String exec = "/home/dach/finder/dach.sh";
	private static String copy = "/bin/cp";
	private static String java = "/usr/local/jdk/bin/java";
	
	private static String mount = "/data/local/gfarm_v2/bin/gfarm2fs";
	private static String unmount = "/usr/bin/fusermount";
	private static String homeDir = "/home/dach004";

	private static int dryRun = -1;

	private static Server server;
	private static int serverPort = 5678;
	
	// These are generated once
	private static HashMap<String, String> properties = null;

	private static String classpath = null;

	private static String currentDir = System.getProperty("user.dir");

	private static String outputDir = null;

	private static LinkedList<String> clusterFiles = new LinkedList<String>();

	// This contains target hosts (usually listed in a file)
	private static LinkedList<Cluster> clusters = new LinkedList<Cluster>();

	// This contains all the problem IDs (listed on the command line)
	private static LinkedList<Problem> problems = new LinkedList<Problem>();

	// This will be created once the required number of threads (or jobs) is
	// known.
	private static JobController controller;

	// The next free job number
	private static int jobNo = 0;

	private static String getOuputDir() {

		File tmp = new File(currentDir + File.separator + "output");
		tmp.mkdir();

		if (!tmp.exists()) {
			return null;
		}

		return tmp.getPath();
	}

	private static String getClassPath() {

		if (classpath == null) {
			classpath = homeDir + "/lib/*:" + homeDir + "/lib/deploy/*:"
					+ homeDir + "/lib/ibis/*";
		}

		return classpath;
	}

	private static HashMap<String, String> getProperties() {

		if (properties == null) {
			properties = new HashMap<String, String>();
			properties.put("log4j.configuration", "file:" + homeDir 
					+ File.separator + "log4j.properties");
			properties.put("gat.adaptor.path", homeDir + File.separator + "lib" 
					+ File.separator + "deploy" + File.separator + "adaptors");
			properties.put("gat.debug", "");
		}

		return properties;
	}

	private static String getServerAddress() {
		return server.getLocalAddress();
	}

	private static String[] getArguments(Cluster c) {

		ArrayList<String> arguments = new ArrayList<String>();

		if (dryRun > 0) { 
			arguments.add("-dryRun");
			arguments.add(Integer.toString(dryRun-1));
		}
		
		arguments.add("-pool");
		arguments.add(pool);

		arguments.add("-cluster");
		arguments.add(c.name);

		arguments.add("-server");
		arguments.add(getServerAddress());

		arguments.add("-exec");
		arguments.add(exec);

		arguments.add("-copy");
		arguments.add(copy);

		arguments.add("-mount");
		arguments.add(mount);

		arguments.add("-unmount");
		arguments.add(unmount);

		arguments.add("-threads");
		arguments.add(Integer.toString(submitThreads));

		arguments.add("-id");
		arguments.add(c.getID());

		arguments.add("-home");
		arguments.add(homeDir);

		arguments.add("-java");
		arguments.add(java);
		
		arguments.add("-targets");
		arguments.add(c.file);

		for (Problem p : problems) {
			arguments.add("-problem");
			arguments.add(p.ID);
			arguments.add(p.directory);
		}

		return arguments.toArray(new String[arguments.size()]);
	}

	private static void submit(Cluster c) throws GATObjectCreationException,
			URISyntaxException {

		c.setID("DACH-CLUSTER-" + (jobNo++) + "@" + c.master);

		System.out.println("Submitting " + c.getID());

		// SoftwareDescription sd = new SoftwareDescription();
/*
		GATContext context = new GATContext();
		SecurityContext security = new CertificateSecurityContext(new URI(System.getProperty("user.home") + 
				File.separator + ".ssh" + File.separator + "id_dsa"), null, "dach004", null);
		context.addSecurityContext(security);
	*/	
		JavaSoftwareDescription sd = new JavaSoftwareDescription();

		sd.setJavaMain("ibis.dachsatin.deployment.cluster.Main");
		sd.setJavaSystemProperties(getProperties());
		sd.setJavaArguments(getArguments(c));
		sd.setJavaClassPath(getClassPath());
		sd.setExecutable(java);
		
		sd.addAttribute("sandbox.root", homeDir);
		sd.addAttribute("sandbox.useroot", "true");
		sd.addAttribute("sandbox.delete", "false");

		JobDescription jd = new JobDescription(sd);

		JobHandler h = new JobHandler(controller, c.getID(), jd, new URI(
				"any://" + c.master));
		
		if (dryRun == 0) { 
			System.err.println("DryRun -- NOT submitting job " + c.getID() + " to " + c.master);
		} else { 
			controller.addJobToSubmit(h);
		}
	}
	
	private static void createServer() { 
		
		 Properties properties = new Properties();

		 //properties.setProperty(ServerProperties.START_HUB, "false");
		 //properties.setProperty(ServerProperties.HUB_ONLY, "true");
		 //properties.setProperty(ServerProperties.HUB_ADDRESSES, args[i]);
		 //properties.setProperty(ServerProperties.HUB_ADDRESS_FILE, file);
		 
		 properties.put(ServerProperties.PORT, Integer.toString(serverPort));
		 properties.setProperty(ServerProperties.PRINT_EVENTS, "true");
	     properties.setProperty(ServerProperties.PRINT_ERRORS, "true");
	     properties.setProperty(ServerProperties.PRINT_STATS, "true");
	     // properties.setProperty(ServerProperties.REMOTE, "true");

	     try {
	    	 server = new Server(properties);
	     } catch (Throwable t) {
	    	 System.err.println("Could not start Ibis Server: " + t);
	    	 System.exit(1);
	     }
	     
	     System.out.println("Create Ibis server on: " + server.getLocalAddress());
	}
	
	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {
			
			if (args[i].equals("-dryRun") && i != args.length-1) { 
    			dryRun = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-copy") && i != args.length - 1) {
				copy = args[++i];
			} else if (args[i].equals("-pool") && i != args.length - 1) {
				pool = args[++i];
			} else if (args[i].equals("-exec") && i != args.length - 1) {
				exec = args[++i];
			} else if (args[i].equals("-java") && i != args.length - 1) {
				java = args[++i];
			} else if (args[i].equals("-mount") && i != args.length - 1) {
				mount = args[++i];
			} else if (args[i].equals("-unmount") && i != args.length - 1) {
				unmount = args[++i];
			} else if (args[i].equals("-thread") && i != args.length - 1) {
				submitThreads = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-home") && i != args.length - 1) {
				homeDir = args[++i];
			} else if (args[i].equals("-problem") && i != args.length - 2) {
				String ID = args[++i];
				String dir = args[++i];
				problems.add(new Problem(ID, dir));
			} else if (args[i].equals("-cluster") && i != args.length - 1) {
				clusterFiles.add(args[++i]);
			} else {
				System.err.println("Unknown or incomplete option: " + args[i]);
				System.exit(1);
			}
		}

		
		if (pool == null) {
			
			try { 
				pool = FileUtils.createUniqueName();
			} catch (Exception e) {
				pool = "dach004-0";
			}
			
			System.out.println("Generated pool name: " + pool);
		}

		if (copy == null) {
			System.err.println("Copy executable not set!");
			System.exit(1);
		}

		if (exec == null) {
			System.err.println("DACH executable not set!");
			System.exit(1);
		}
		
		if (java == null) {
			System.err.println("Java executable not set!");
			System.exit(1);
		}

		if (mount == null) {
			System.err.println("Mount executable not set!");
			System.exit(1);
		}

		if (unmount == null) {
			System.err.println("unmount executable not set!");
			System.exit(1);
		}

		if (problems.size() == 0) {
			System.err.println("No problems specified!");
			System.exit(1);
		}

		if (clusterFiles.size() == 0) {
			System.err.println("No target cluster files specified!");
			System.exit(1);
		}

		createServer();
		
		
		for (String f : clusterFiles) {
			try {
				clusters.add(Cluster.read(f));
			} catch (Exception e) {
				System.err.println("Failed to read cluster file: " + f);
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		outputDir = getOuputDir();

		if (outputDir == null) {
			System.err.println("Failed to create output dir!");
			System.exit(1);
		}

		if (submitThreads > clusters.size()) {
			submitThreads = clusters.size();
		}

		controller = new JobController(submitThreads);

		System.out.println("Starting Master for " + clusters.size()
				+ " clusters: ");

		for (Cluster c : clusters) {

			System.out.println("   " + c);

			try {
				submit(c);
			} catch (GATObjectCreationException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		while (controller.hasJobs()) {

			LinkedList<JobHandler> stopped = controller.getStoppedJobs();

			if (stopped != null) {

				for (JobHandler h : stopped) {
					if (h.submissionError() || h.hashCrashed()) {
						System.out.println("Resubmitting " + h.ID + " to "
								+ h.target);
						controller.addJobToSubmit(h);
					} else {
						System.out.println("Job " + h.ID + " on " + h.target
								+ " is finished");
					}
				}
			}
		}
		
		server.end(10000);

	}

}
