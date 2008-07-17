package ibis.dachmaestro;

import ibis.maestro.CompletionListener;
import ibis.maestro.Job;
import ibis.maestro.JobList;
import ibis.maestro.LabelTracker;
import ibis.maestro.Node;
import ibis.maestro.Service;

import java.io.File;
import java.util.ArrayList;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Main {

    private static void usage() { 
	System.err.println("Usage: Main <directory>");
	System.exit(1);
    }
    private static class Listener implements CompletionListener
    {
	private final LabelTracker labelTracker = new LabelTracker();
	private boolean sentFinal = false;

	/** Handle the completion of task 'j': the result is 'result'.
	 * @param id The task that was completed.
	 * @param result The result of the task.
	 */
	@Override
	public void jobCompleted( Node node, Object id, Object result )
	{
	    if( !(id instanceof LabelTracker.Label)) {
		System.err.println( "Internal error: Object id is not a tracker label: " + id );
		System.exit( 1 );
	    }
	    labelTracker.returnLabel( (LabelTracker.Label) id );
	    if( sentFinal && labelTracker.allAreReturned() ) {
		System.out.println( "I got all job results back; stopping test program" );
		node.setStopped();
	    }
	}

	Object getLabel() {
	    return labelTracker.nextLabel();
	}

	void setFinished() {
	    sentFinal = true;	    
	}
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main( String[] args )
    {
	boolean goForMaestro = false;

	if (args.length == 0) { 
	    usage();
	}

	try {
	    File dir = null;
	    boolean verbose = false;
	    String command = null;

	    for (int i=0;i<args.length;i++) { 

		if (args[i].equals("-v") || args[i].equals("--verbose")) { 
		    verbose = true;
		} else if (args[i].equals("-c") || args[i].equals("--command")) {
		    if (command == null) { 
			command = args[++i];
		    } else { 
			System.err.println("Command already specified!");
			System.exit(1);
		    }
		} else if (args[i].equals("-d") || args[i].equals("--directory")) { 
		    if (dir == null) {  
			dir = new File(args[++i]);
		    } else { 
			System.err.println("Directory already specified!");
			System.exit(1);
		    }    			
		} else { 
		    usage();
		}
	    }

	    if (command == null) { 
		System.err.println("No command specified!");
		System.exit(1);	
	    }

	    if( dir != null ) {
		goForMaestro = true;
		if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) { 
		    System.err.println("Directory " + dir + " cannot be accessed!");
		    System.exit(1);
		}
	    }
	    JobList jobs = new JobList();
	    Job job = jobs.createJob(
		    "comparison",
		    new Comparator( command )
	    );
	    Listener listener = new Listener();

	    ArrayList<Pair> pairs = FindPairs.getPairs( dir, verbose );

	    if (pairs.isEmpty() ) { 
		System.err.println("No pairs found in directory " + args[0]);
		System.exit(1);
	    }

	    if (verbose) { 
		System.out.printf("Starting comparison of " + pairs.size() + " pairs.");
	    }

	    Node node = new Node( jobs, goForMaestro );

	    System.out.println( "Node created" );
	    long startTime = System.nanoTime();
	    if( node.isMaestro() ) {
		for( Pair pair: pairs ) {
		    Object label = listener.getLabel();
		    job.submit( node, pair, label, listener );
		}
		listener.setFinished();
		System.out.println( "Jobs submitted" );
	    }
	    node.waitToTerminate();
	    long stopTime = System.nanoTime();
	    System.out.println( "Duration of this run: " + Service.formatNanoseconds( stopTime-startTime ) );
	}
	catch (Exception x) {
	    System.out.println( "main(): caught exception:" + x );
	    x.printStackTrace();
	}
    }

}
