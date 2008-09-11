package ibis.masterworker.deployment;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.resources.JavaSoftwareDescription;
import org.gridlab.gat.resources.JobDescription;

public class Deployment {
	
	private static final int DEFAULT_SUBMIT_THREADS = 15;

	private final List<Cluster> clusters;
	
	private final Logger logger = Logger.getLogger("masterworker.deployment.deployment");
	
	private final JobController controller;
	
	private final String homeDir;
	private final String outputDir;
	
	private final String workerMain; 
	private final String workerClasspath; 
	private final HashMap<String, String> workerProperties;
	private final LinkedList<String> defaultWorkerArgs;
	
	private int jobNo = 0;
	
	private final boolean dryRun = false;
	private final boolean headnodeOnly;
	private final String prefix; 
	
	public Deployment(List<Cluster> clusters, String homeDir, String outputDir, 
			String workerMain, String workerClasspath, HashMap<String, String> workerProperties, 
			LinkedList<String> defaultWorkerArgs, boolean headnodeOnly, String prefix) throws Exception {  

		super();
		
		this.homeDir = homeDir;
		this.outputDir = outputDir;

		this.workerMain = workerMain;
		this.workerClasspath = workerClasspath;
		this.workerProperties = workerProperties;
		this.defaultWorkerArgs = defaultWorkerArgs;
		this.headnodeOnly = headnodeOnly;
		this.prefix = prefix;	
		
		this.clusters = clusters;
		
		controller = new JobController(DEFAULT_SUBMIT_THREADS);
	}
	
	private String[] getArguments(Cluster c, String node) {
		
		LinkedList<String> args = new LinkedList<String>();
		args.addAll(defaultWorkerArgs);
		
		args.add("-node");
		args.add(node);
		
		args.add("-location");
		args.add(c.name);
		
		args.add("-cluster"); 
		args.add("./clusters/" + c.name + ".cluster");
		
		return args.toArray(new String[args.size()]);
	}

	private JobDescription createJobDesciption(String main, HashMap<String, String> p, 
			String [] arguments, String classpath, String java) {
		JavaSoftwareDescription sd = new JavaSoftwareDescription();

		sd.setJavaMain(main);
		sd.setJavaSystemProperties(p);
		sd.setJavaArguments(arguments);
		sd.setJavaClassPath(classpath);
		sd.setExecutable(java);
		sd.setJavaOptions(new String [] { "-Xmx2048M" } );

		sd.addAttribute("sandbox.root", homeDir);
		sd.addAttribute("sandbox.useroot", "true");
		sd.addAttribute("sandbox.delete", "false");

		return new JobDescription(sd);
	}	
	
	private void submitDirectly(Cluster c, String node, int worker) throws GATObjectCreationException,	URISyntaxException { 
		
		String ID = c.getID() + ".w." + worker + "." + getHostName(node);
		
		logger.info("   Submitting job " + ID + " directly to " + node);
		
		JobDescription jd = createJobDesciption(workerMain, workerProperties,  
				getArguments(c, node), workerClasspath, c.java);
		
		JobHandler h = new JobHandler(controller, ID, jd, new URI("any://" + node), outputDir, prefix);

        if (dryRun) { 
        	logger.warn("   DryRun -- NOT submitting job " + ID + " to " + node);
        } else { 
        	controller.addJobToSubmit(h);
        }
		
	}
	
	public static String getHostName(String target) { 

		target = target.trim();

		int index = target.indexOf(".");

		if (index <= 0) { 
			return target;
		}

		return target.substring(0, index); 
	}
		
	private void submitToHead(Cluster c) { 
		
		logger.info("   Starting application on headnode (" + c.master + ") of cluster " + c.name);
    	
		try {
			submitDirectly(c, c.master, 0);
		} catch (Exception e) {
			logger.warn("Failed to submit job!", e);
		}
	}
	
	private void submitToAll(Cluster c) { 
		
		logger.info("   Directly starting application on " + c.nodes.size() + " nodes of cluster " + c.name);
    	
		int worker = 0;
		
    	for (String node : c.nodes) { 
    		
    		logger.info("      " + node);
    		
    		try {
				submitDirectly(c, node, worker++);
			} catch (Exception e) {
				logger.warn("Failed to submit job!", e);
			}
    	}
	}
	
	private void submit(Cluster c) throws GATObjectCreationException,	URISyntaxException {

		c.setID("c." + (jobNo++) + "." + c.name);
		
		if (headnodeOnly) { 
			// We are not allowed to submit directly to the nodes. Instead, we start 
			// a wrapper on the headnode, which is then responsible for the rest of the 
			// submission
			submitToHead(c);
		} else { 
			// We can directly submit to the nodes
			submitToAll(c);
		}
	}
	
	public void deploy() { 
		
		logger.info("Starting workers on " + clusters.size() + " clusters: ");
		
		for (Cluster c : clusters) {

			logger.info("   " + c);

			try {
				submit(c);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}
	}
}
