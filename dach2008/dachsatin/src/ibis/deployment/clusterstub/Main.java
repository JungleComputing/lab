package ibis.deployment.clusterstub;

import java.util.LinkedList;

import org.gridlab.gat.resources.Job.JobState;

import ibis.dachsatin.Problem;
import ibis.deploy.Application;
import ibis.deploy.Deployer;
import ibis.deploy.Grid;
import ibis.deploy.Job;
import ibis.deploy.SubJob;

public class Main {

	private static String [] getArguments(LinkedList<Problem> problems) { 
		
		String [] result = new String[problems.size() * 3 + 1];
		
		result[0] = "-v";
		
		int index = 1;
		
		for (Problem p : problems) { 
			result[index++] = "-p";
			result[index++] = p.ID;
			result[index++] = p.directory;
		}

		return result;
	}
	
	
    public static void main(String[] args) {

    	String cluster = null;    	
    	String pool = null;
    	String server = null;
    	
    	LinkedList<Problem> problems = new LinkedList<Problem>(); 
    	
    	for (int i=0;i<args.length;i++) { 
    		
    		if (args[i].equals("-pool") && i != args.length-1) { 
    			pool = args[++i];
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
    	    	
    	if (problems.size() == 0) { 
    		System.err.println("No problems specified!");
    		System.exit(1);
    	}
    	
    	
    	
        try {
            // first create a new deployer object
            Deployer deployer = new Deployer();

            // then we've to create the application description, we can do that
            // by loading a properties file or by creating an Application object
            Application application = new Application(
            		"DACH2008-Satin",         // name of the application
                    "ibis.dachsatin.Main",    // main class
                    new String[] { },         // java options 
                    
                    new String[] { "-Dibis.server.address=" + server,        // java system properties 
            				       "-Dibis.pool.name=" + pool, 
            				       "-Dsatin.detailedStats", 
            				       "-Dlog4j.configuration=file:log4j.properties" },  
            				       
                    getArguments(problems), // java arguments
                    
                    // HIERO -- JaSON
                    
                    new String[] { "sleep", "sleep/log4j.properties" }, // pre stage 

                    null, // post stage
                    null // ibis server pre stage
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
