package ibis.dachsatin.deployment.cluster;

import java.io.File;
import java.util.LinkedList;

import org.gridlab.gat.resources.JobDescription;
import org.gridlab.gat.resources.SoftwareDescription;
import org.gridlab.gat.resources.Job.JobState;

import ibis.deploy.Application;
import ibis.deploy.Deployer;
import ibis.deploy.Grid;
import ibis.deploy.Job;
import ibis.deploy.SubJob;

public class Main {

	private static String cluster = null;    	
	private static String pool = null;
	private static String server = null;
	private static String exec = "/home/dach/finder/dach.sh";
	private static String copy = "/bin/cp";
	private static String localID = null;
	private static String mount = "/data/local/gfarm_v2/bin/gfarm2fs";
	private static String unmount = "/usr/bin/fusermount";
	private static String outputDir = null;
	
	private static LinkedList<Problem> problems = new LinkedList<Problem>(); 
	
	private static String getOuputDir() { 

		String dir = System.getProperty("user.dir");
		
		File tmp = new File(dir + File.separator + "output");
		tmp.mkdir();
		
		if (!tmp.exists()) { 
			return null;
		}
		
		return tmp.getPath();
	}
	
	private static String [] getOptions() { 
		return new String[] { "-classpath ", "./lib/ibis/*.jar" };  
	}
	
	private static String [] getProperties() { 
		return new String[] { 
				"-Dibis.server.address=" + server,  
			    "-Dibis.pool.name=" + pool, 
			    "-Dsatin.detailedStats", 
			    "-Ddach.executable=" + exec, 
			    "-Ddach.copy=" + copy, 
			    "-Ddach.master.ID=" + localID, 
			    "-Ddach.dir.output=" + outputDir, 
			    "-Dlog4j.configuration=file:log4j.properties" };
	}
	
	private static String [] getArguments() { 
		
		String [] result = new String[problems.size() * 3 + 2];
		
		result[0] = "ibis.dachsatin.Main";
		result[1] = "-v";
		
		int index = 1;
		
		for (Problem p : problems) { 
			result[index++] = "-p";
			result[index++] = p.ID;
			result[index++] = p.directory;
		}

		return result;
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
    		} else if (args[i].equals("-problem") && i != args.length-2) {
    			String ID = args[++i];
    			String dir = args[++i];
    			problems.add(new Problem(ID, dir));
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
    	
    	outputDir = getOuputDir();
    	
    	if (outputDir == null) { 
    		System.err.println("Failed to create output dir!");
    		System.exit(1);
    	}
    		
    		
    /*
    	try { 
    		new Main().start();
    	} catch (Exception e) {
    		System.err.println("Main failed!");
    		e.printStackTrace(System.err);
    	}
    	*/
    	
    	
        try {
        	
            // first create a new deployer object
            Deployer deployer = new Deployer();

            // then we've to create the application description, we can do that
            // by loading a properties file or by creating an Application object
            Application application = new Application(
            		"DACH2008-Worker",         // name of the application
                    "ibis.deployment.wapper.Wrapper",    // main class
                   
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
        }
    }

}
