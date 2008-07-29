package ibis.dachsatin.deployment.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;

import org.gridlab.gat.GAT;
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
	private static String localID = null;
	private static String mount = "/data/local/gfarm_v2/bin/gfarm2fs";
	private static String unmount = "/usr/bin/fusermount";

	// These are generated once
	private static HashMap<String,String> properties = null; 
	private static String [] arguments = null;
	private static String classpath = null;
	
	private static String currentDir = System.getProperty("user.dir");
	private static String outputDir = null;
	private static String targetFile = null;
	
	
	
	// This contains target hosts (usually listed in a file)
	private static LinkedList<String> targets = new LinkedList<String>();
	
	// This contains all the problem IDs (listed on the command line)
	private static LinkedList<Problem> problems = new LinkedList<Problem>(); 
	
	// This will be created once the required number of threads (or jobs) is known.
	private static JobController controller;
	
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
			classpath = currentDir +  "/lib/*:" + currentDir + "/lib/ibis/*";
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
			arguments = new String[problems.size() * 3 + 2];
		
			arguments[0] = "ibis.dachsatin.worker.Main";
			arguments[1] = "-v";
		
			int index = 2;

			for (Problem p : problems) { 
				arguments[index++] = "-p";
				arguments[index++] = p.ID;
				arguments[index++] = p.directory;
			}

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
	
	private static void readTargetFile() { 
		
		BufferedReader reader= null;
		
		try {
			reader = new BufferedReader(new FileReader(targetFile));
		
			String line = reader.readLine();
			
			while (line != null) { 
				
				line = line.trim();
				
				if (line.length() > 0) { 
					targets.add(line);
				}
					
				line = reader.readLine();	
			}
		} catch (Exception e) {
			System.err.println("Failed to read target file!");
			e.printStackTrace(System.err);
		} finally { 
			try { 
				if (reader != null) { 
					reader.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
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
		
		JobDescription jd = new JobDescription(sd);
        
        JobHandler h = new JobHandler(controller, ID, jd, new URI("any://" + target));

        controller.addJobToSubmit(h);
	}
	
    public static void main(String[] args) {
    	
    	for (int i=0;i<args.length;i++) { 
    		
    		if (args[i].equals("-id") && i != args.length-1) { 
    			localID = args[++i];
    		} else if (args[i].equals("-copy") && i != args.length-1) { 
    			copy = args[++i];
    		} else if (args[i].equals("-exec") && i != args.length-1) { 
    			exec = args[++i];
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
    		} else if (args[i].equals("-thread") && i != args.length-1) { 
    			submitThreads = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-problem") && i != args.length-2) {
    			String ID = args[++i];
    			String dir = args[++i];
    			problems.add(new Problem(ID, dir));
    		} else if (args[i].equals("-targets") && i != args.length-1) {
    			targetFile = args[++i];
    		} else { 
    			System.err.println("Unknown or incomplete option: " + args[i]);
    			System.exit(1);
    		}
    	}
    	
    	if (pool == null) { 
    		System.err.println("Ibis pool not set!");
    		System.exit(1);
    	}
    	
    	if (server == null) { 
    		System.err.println("Ibis server not set!");
    		System.exit(1);
    	}
    	
    	if (cluster == null) { 
    		System.err.println("Ibis cluster not set!");
    		System.exit(1);
    	}
    
    	if (localID == null) { 
    		System.err.println("Local ID not set!");
    		System.exit(1);
    	}
    	
    	if (copy == null) { 
    		System.err.println("Copy executable not set!");
    		System.exit(1);
    	}
    
    	if (exec == null) { 
    		System.err.println("DACH executable not set!");
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
    	
    	if (targetFile == null) { 
    		System.err.println("No target file specified!");
    		System.exit(1);
    	}
    	
    	readTargetFile();
    	
    	if (targets.size() == 0) { 
    		System.err.println("No targets specified!");
    		System.exit(1);
    	}
    	
    	outputDir = getOuputDir();
    	
    	if (outputDir == null) { 
    		System.err.println("Failed to create output dir!");
    		System.exit(1);
    	}
    	
    	if (submitThreads > targets.size()) { 
    		submitThreads = targets.size();
    	}
    		
    	controller = new JobController(submitThreads);
    	
    	System.out.println("Starting application on " + targets.size() + " nodes: ");
    	
    	for (String node : targets) { 
    		
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
    		
    	
    	
    	
    	
    	
    		
    /*
    	try { 
    		new Main().start();
    	} catch (Exception e) {
    		System.err.println("Main failed!");
    		e.printStackTrace(System.err);
    	}
    	*/
    	/*
    	
        try {
        	
            // first create a new deployer object
            Deployer deployer = new Deployer();

            // then we've to create the application description, we can do that
            // by loading a properties file or by creating an Application object
            Application application = new Application(
            		"DACH2008-Worker",         // name of the application
                    "ibis.dachsatin.deployment.wapper.Wrapper",    // main class
                   
                    getOptions(),    // java options   
                    getProperties(), // java system properties 
                    getArguments(),  // java arguments
                    
                    null,  // no pre stage (within cluster we have NFS) 
                    null,  // post stage (same)
                    null   // ibis server pre stage (not used)
            );
        
            // add this application description to the deployer. Now the
            // deployer knows about this application using its name
            deployer.addApplication(application);

            // add a grid to the deployer (this can also be done using a Grid
            // object, or by loading a properties file)
            deployer.addGrid("das3.properties");

            // request a grid from the deployer
            Grid das3 = deployer.getGrid("DAS-3");

            // create a job we want to run on this grid (called 'main job')
            Job job = new Job("main job");

            // this job consists of several sub jobs, the first of it is named
            // 'sub job 1'
            SubJob subJob1 = new SubJob("subjob1");
            // it runs the specified application
            subJob1.setApplication(application);
            // on the specified grid
            subJob1.setGrid(das3);
            // on the specific cluster
            subJob1.setCluster(das3.getCluster("VU"));
            // with this number of nodes
            subJob1.setNodes(Integer.parseInt(args[1]));
            // and this total number of cores
            subJob1.setCores(Integer.parseInt(args[2]));
            // we're done specifying the sub job and now add it to the main job
            job.addSubJob(subJob1);

            SubJob subJob2 = new SubJob("subjob2");
            // it runs the specified application
            subJob2.setApplication(application);
            // on the specified grid
            subJob2.setGrid(das3);
            // on the specific cluster
            subJob2.setCluster(das3.getCluster("VU"));
            // with this number of nodes
            subJob2.setNodes(Integer.parseInt(args[1]));
            // and this total number of cores
            subJob2.setCores(Integer.parseInt(args[2]));
            // we're done specifying the sub job and now add it to the main job
            job.addSubJob(subJob2);

            // now we're going to deploy the main job using our deployer
            deployer.deploy(job);

            // and we're polling for our job to be finished
            while (job.getStatus().get(JobState.STOPPED) < 1) {
                System.out.println("job.status:\n" + job.getStatus());
                Thread.sleep(1000);
            }

            System.out.println("Deployer end!");

            // to clean up everything we call the end method
            deployer.end();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    	
    	
    	
    }

}
