package ibis.dachmaestro;

import ibis.maestro.CompletionListener;
import ibis.maestro.Job;
import ibis.maestro.JobList;
import ibis.maestro.LabelTracker;
import ibis.maestro.Node;
import ibis.maestro.Service;
import ibis.maestro.LabelTracker.Label;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
        private final HashMap<LabelTracker.Label,String> results = new HashMap<LabelTracker.Label,String>();

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
            if( !(resultObject instanceof String) ) {
                System.err.println( "Internal error: result is not a string but a " + resultObject.getClass() + ": " + resultObject );
                System.exit( 1 );
            }
            String result = (String) resultObject;
            LabelTracker.Label label = (LabelTracker.Label) id;
            labelTracker.returnLabel( label );
            System.out.println( "Result for " + label + " is " + result );
            results.put( label, result );
            if( sentFinal && labelTracker.allAreReturned() ) {
                System.out.println( "I got all job results back; stopping program" );
                node.setStopped();
            }
        }

        Label getLabel() {
            return labelTracker.nextLabel();
        }

        void setFinished() {
            sentFinal = true;	    
        }
        
        HashMap<Label, String> getResults()
        {
            return results;
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
            ArrayList<Label> labels = new ArrayList<Label>();
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
                    oracleHomeName = args[++i];
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
                    System.out.println( "Starting " + problems.size() + " problem sets" );
                }
                goForMaestro = true;
            }

            Node node = new Node( jobs, goForMaestro );

            System.out.println( "Node created" );
            long startTime = System.nanoTime();
            if( node.isMaestro() ) {
                for( String problem: problems ) {
                    Label label = listener.getLabel();
                    problemSetJob.submit( node, problem, label, listener );
                    labels.add( label );
                }
                listener.setFinished();
                System.out.println( "Jobs submitted" );
            }
            node.waitToTerminate();
            HashMap<Label, String> l = listener.getResults();
            for( Label label: labels ) {
                String res = l.get( label );
                System.out.println( label + "->" + res );
            }
            long stopTime = System.nanoTime();
            System.out.println( "Duration of this run: " + Service.formatNanoseconds( stopTime-startTime ) );
        }
        catch (Exception x) {
            System.out.println( "main(): caught exception:" + x );
            x.printStackTrace();
        }
    }

}
