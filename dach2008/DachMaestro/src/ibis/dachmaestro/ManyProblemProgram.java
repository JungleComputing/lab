package ibis.dachmaestro;

import ibis.maestro.CompletionListener;
import ibis.maestro.Job;
import ibis.maestro.JobList;
import ibis.maestro.LabelTracker;
import ibis.maestro.Node;
import ibis.maestro.Service;

import java.io.File;
import java.util.LinkedList;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class ManyProblemProgram {

    private static void usage() { 
        System.err.println( "Usage: ManyProblemProgram <problem-name> .. <problem-name>" );
        System.exit( 1 );
    }
    private static class Listener implements CompletionListener
    {
        private final LabelTracker labelTracker = new LabelTracker();
        private boolean sentFinal = false;
        private StringBuilder resultString = new StringBuilder();

        /** Handle the completion of task with identifier 'id': the result is 'result'.
         * @param node The node we're running this on.
         * @param id The task that was completed.
         * @param resultObject The result of the task.
         */
        @Override
        public void jobCompleted( Node node, Object id, Object resultObject )
        {
            if( !(id instanceof LabelTracker.Label) ){
                System.err.println( "Internal error: Object id is not a tracker label but a " + resultObject.getClass() + ": " + id );
                System.exit( 1 );
            }
            if( !(resultObject instanceof Result) ) {
                System.err.println( "Internal error: result is not a Result but a " + resultObject.getClass() + ": " + resultObject );
                System.exit( 1 );
            }
            Result result = (Result) resultObject;
            if( result.error == null ) {
                // All went well.
                resultString.append( result.result );
            }
            else {
                System.err.println( "Comparison failed: " + result.error );
            }
            labelTracker.returnLabel( (LabelTracker.Label) id );
            if( sentFinal && labelTracker.allAreReturned() ) {
                System.out.println( "I got all job results back; stopping program" );
                node.setStopped();
            }
        }

        Object getLabel() {
            return labelTracker.nextLabel();
        }

        void setFinished() {
            sentFinal = true;	    
        }

        String getResult()
        {
            return resultString.toString();
        }
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    @SuppressWarnings("synthetic-access")
    public static void main( String[] args )
    {
        boolean goForMaestro = false;

        if (args.length == 0) { 
            usage();
        }

        try {
            String oracleHomeName = "/home/dach911";
            boolean verbose = false;
            String command = null;
            LinkedList<String> problems = new LinkedList<String>();

            for (int i=0;i<args.length;i++) { 

                if (args[i].equals("-v") || args[i].equals("--verbose")) { 
                    verbose = true;
                }
                else if (args[i].equals("-c") || args[i].equals("--command")) {
                    if (command == null) { 
                        command = args[++i];
                    }
                    else { 
                        System.err.println("Command already specified!");
                        System.exit(1);
                    }
                }
                else if (args[i].equals("-o") || args[i].equals("--oraclehome")) {
                    if (oracleHomeName == null) { 
                        oracleHomeName = args[++i];
                    }
                    else { 
                        System.err.println("Oracle home already specified!");
                        System.exit(1);
                    }
                }
                else {
                    problems.add( args[i] );
                }
            }

            if (command == null) {
                command = System.getenv( "DACHCOMPARATOR" );

                if (command == null ) {
                    System.err.println( "No comparison command given, and DACHCOMPARATOR environement variable is not set" );
                    System.exit( 1 );	
                }
            }
            File oracleHome = new File( oracleHomeName );

            JobList jobs = new JobList();
            Job job = jobs.createJob(
                "comparison",
                new FileComparatorTask( command )
            );
            Job problemSetJob = jobs.createJob(
                "run problem set",
                new ProblemSetTask( job, oracleHome, verbose )
            );
            Listener listener = new Listener();

            if( !problems.isEmpty() ) {
                if (verbose) { 
                    System.out.printf("Starting " + problems.size() + " problem sets");
                }
                goForMaestro = true;
            }

            Node node = new Node( jobs, goForMaestro );

            System.out.println( "Node created" );
            long startTime = System.nanoTime();
            if( node.isMaestro() ) {
                for( String problem: problems ) {
                    Object label = listener.getLabel();
                    problemSetJob.submit( node, problem, label, listener );
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
