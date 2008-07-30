package ibis.dachsatin.deployment.cluster;

import ibis.dachsatin.deployment.util.Cluster;
import ibis.dachsatin.deployment.util.JobController;
import ibis.dachsatin.deployment.util.JobHandler;
import ibis.dachsatin.deployment.util.Problem;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.resources.JavaSoftwareDescription;
import org.gridlab.gat.resources.JobDescription;

public class Main {

	// These are set (or overwitten) by command line arguments
	private static int submitThreads = 15;
	
	private static String cluster = null;    	
	private static String pool = null;
	private static String server = null;
	private static String exec = "/home/dach/finder/dach.sh";
	private static String copy = "/bin/cp";
	private static String java = "/usr/local/jdk/bin/java";
	private static String localID = null;
	private static String mount = "/data/local/gfarm_v2/bin/gfarm2fs";
	private static String unmount = "/usr/bin/fusermount";
	private static String homeDir = "/home/dach004";
	
	private static int dryRun = -1;
	
	// These are generated once
	private static HashMap<String,String> properties = null; 
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
	
	private static HashMap<String, String> getProperties() { 
	
		if (properties == null) { 
			properties = new HashMap<String, String>();
			properties.put("ibis.server.address", server);  
			properties.put("ibis.pool.name", pool); 
			properties.put("satin.detailedStats", null);
			properties.put("dach.executable", exec); 
			properties.put("dach.copy", copy); 
			properties.put("dach.master.ID", localID); 
			properties.put("dach.dir.output", outputDir);
			properties.put("log4j.configuration", "file:log4j.properties");
		}
		
		return properties;
	}
	
	private static String [] getArguments() { 
		
		if (arguments == null) { 
			ArrayList<String> args = new ArrayList<String>();

			if (dryRun > 0) { 
				args.add("-dryRun");
				args.add(Integer.toString(dryRun-1));
			}
		
			args.add("-class");
			args.add("ibis.dachsatin.worker.Main");
			
			args.add("-mount");
			args.add(mount);
			
			args.add("-unmount");
			args.add(unmount);
			
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
	
	private static void submit(String target) throws GATObjectCreationException, URISyntaxException { 
		
		String ID = "DACH-" + jobNo++;
	
		System.out.println("Submitting " + ID + " to " + target);
		
		//SoftwareDescription sd = new SoftwareDescription();
	    
		JavaSoftwareDescription sd = new JavaSoftwareDescription();
		
		sd.setJavaMain("ibis.dachsatin.deployment.wrapper.Wrapper");
		sd.setJavaSystemProperties(getProperties());
		sd.setJavaArguments(getArguments());
		sd.setJavaClassPath(getClassPath());
		
		sd.setExecutable(java);
		
		sd.addAttribute("sandbox.root", homeDir);
		sd.addAttribute("sandbox.useroot", "true");
		sd.addAttribute("sandbox.delete", "false");
		
		JobDescription jd = new JobDescription(sd);
        
        JobHandler h = new JobHandler(controller, ID, jd, new URI("any://" + target));

        if (dryRun == 0) { 
        	System.err.println("DryRun -- NOT submitting job " + ID + " to " + target);
        } else { 
        	controller.addJobToSubmit(h);
        }
	}
	
    public static void main(String[] args) {
    	
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
    		} else if (args[i].equals("-mount") && i != args.length-1) { 
    			mount = args[++i];
    		} else if (args[i].equals("-unmount") && i != args.length-1) { 
    			unmount = args[++i];
    		} else if (args[i].equals("-cluster") && i != args.length-1) { 
    			cluster = args[++i];
    		} else if (args[i].equals("-server") && i != args.length-1) { 
    			server = args[++i];
    		} else if (args[i].equals("-home") && i != args.length-1) { 
    			homeDir = args[++i];    	
    		} else if (args[i].equals("-threads") && i != args.length-1) { 
    			submitThreads = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-problem") && i != args.length-2) {
    			String ID = args[++i];
    			String dir = args[++i];
    			problems.add(new Problem(ID, dir));
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
    	
    	if (server == null) { 
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
    	
    	if (mount == null) { 
    		System.err.println("Mount executable not set!");
    		System.exit(0);
    	}
    
    	if (unmount == null) { 
    		System.err.println("unmount executable not set!");
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
    	
    	if (submitThreads > targets.nodes.size()) { 
    		submitThreads = targets.nodes.size();
    	}
    		
    	controller = new JobController(submitThreads);
    	
    	System.out.println("Starting application on " + targets.nodes.size() + " nodes: ");
    	
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
    	
    	while (controller.hasJobs()) { 
    		
    		LinkedList<JobHandler> stopped = controller.getStoppedJobs();
    		
    		if (stopped != null) { 
 
    			for (JobHandler h : stopped) {
    				if (h.submissionError() || h.hashCrashed()) { 
    					System.out.println("Resubmitting " + h.ID + " to " + h.target);
    					controller.addJobToSubmit(h);
    				} else { 
    					System.out.println("Job " + h.ID + " on " + h.target + " is finished");
    				}
    			}
    		}
    	}
    }

}
