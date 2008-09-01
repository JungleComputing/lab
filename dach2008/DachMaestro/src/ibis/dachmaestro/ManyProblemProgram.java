package ibis.dachmaestro;

import ibis.maestro.Job;
import ibis.maestro.JobCompletionListener;
import ibis.maestro.JobList;
import ibis.maestro.LabelTracker;
import ibis.maestro.Node;
import ibis.maestro.Utils;
import ibis.maestro.LabelTracker.Label;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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

    private static final String DEFAULT_ORACLE_HOME = "/home/dach911";
    private static final String DEFAULT_PROBLEMS_DIR = "/tmp/dach001";

    private static void writeTextFile( File path, String text ) throws IOException
    {
        FileOutputStream stream = new FileOutputStream( path );
        PrintStream s = new PrintStream( stream );
        s.print( text );
        s.close();
        stream.close();
    }

    private static class Listener implements JobCompletionListener
    {
        private final LabelTracker labelTracker = new LabelTracker();
        private boolean sentFinal = false;
        private final HashMap<LabelTracker.Label,String> results = new HashMap<LabelTracker.Label,String>();

        /** Handle the completion of task with identifier 'id': the result is 'result'.
         * @param node The node we're running this on.
         * @param id The task that was completed.
         * @param resultObject The result of the task.
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void jobCompleted( Node node, Object id, Object resultObject )
        {
            System.out.println( "Job " + id + " completed" );
            if( !(id instanceof LabelTracker.Label) ){
                System.err.println( "Internal error: Object id is not a tracker label but a " + resultObject.getClass() + ": " + id );
                System.exit( 1 );
            }
            LabelTracker.Label label = (LabelTracker.Label) id;
            labelTracker.returnLabel( label );
            if( !(resultObject instanceof String) ) {
                System.err.println( "Internal error: result is not a string but a " + resultObject.getClass() + ": " + resultObject );
                System.exit( 1 );
            }
            String result = (String) resultObject;
            File resultFile = new File( "result-" + label + "-" + System.currentTimeMillis() + ".txt" );
            try {
                writeTextFile( resultFile, result );
            } catch (IOException e) {
                System.out.println( "Cannot write result to file " + resultFile );
            }
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

    private static void usage( PrintStream s ) { 
        s.println( "Usage: ManyProblemProgram <problem-name> .. <problem-name>" );
        s.println( " -c <command> Use the given command as comparator (default from DACHCOMPARATOR)" );
        s.println( " -h           Show this help text" );
        s.println( " -o <dir>     The directory where the oracle lives (default is " + DEFAULT_ORACLE_HOME + ")" );
        s.println( " -p <dir>     The directory where the problem directories live (default is " + DEFAULT_PROBLEMS_DIR + ")" );
        s.println( " -w <nodes>   Wait until there are at least this many nodes before submitting the problems" );
        s.println( " -v           Be verbose" );
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    @SuppressWarnings("synthetic-access")
    public static void main( String[] args )
    {
        boolean goForMaestro = false;

        try {
            String oracleHomeName = DEFAULT_ORACLE_HOME;
            String problemsDirName = DEFAULT_PROBLEMS_DIR;
            ArrayList<Label> labels = new ArrayList<Label>();
            boolean verbose = false;
            String command = null;
            int waitNodes = 0;
            LinkedList<String> problems = new LinkedList<String>();

            for (int i=0;i<args.length;i++) { 

                if (args[i].equals("-h") || args[i].equals("--help")) { 
                    usage( System.out );
                    System.exit( 0 );
                }
                else if (args[i].equals("-v") || args[i].equals("--verbose")) { 
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
                else if (args[i].equals("-w") || args[i].equals("--waitnodes")) {
                    waitNodes = Integer.parseInt(  args[++i] );
                }
                else if (args[i].equals("-p") || args[i].equals("--problemsdir")) {
                    problemsDirName = args[++i];
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
            File problemsDir = new File( problemsDirName );

            JobList jobs = new JobList();
            Job job = jobs.createJob(
                "comparison",
                new FileComparatorTask( command )
            );
            Job problemSetJob = jobs.createJob(
                "run problem set",
                new ProblemSetTask( job, oracleHome, problemsDir, verbose )
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
                boolean goodToSubmit = true;

                if( waitNodes>0 ) {
                    System.out.println( "Waiting for " + waitNodes + " ready nodes" );
                    long deadline = 5*60*1000; // 5 minutes in ms
                    int n = node.waitForReadyNodes( waitNodes, deadline );
                    System.out.println( "Continuing; there are now " + n + " ready nodes" );
                    if( n*3<waitNodes ) {
                        System.out.println( "That's less than a third of the required nodes (" + waitNodes + "); giving up" );
                        goodToSubmit = false;
                    }
                }
                if( goodToSubmit ) {
                    for( String problem: problems ) {
                        Label label = listener.getLabel();
                        problemSetJob.submit( node, problem, label, listener );
                        labels.add( label );
                    }
                    listener.setFinished();
                    System.out.println( "Jobs submitted" );
                }
                else {
                    System.out.println( "Emergency stop." );
                    node.setStopped();
                }
            }
            node.waitToTerminate();
            HashMap<Label, String> l = listener.getResults();
            for( Label label: labels ) {
                String res = l.get( label );
                System.out.println( label + "->" + res );
            }
            long stopTime = System.nanoTime();
            System.out.println( "Duration of this run: " + Utils.formatNanoseconds( stopTime-startTime ) );
        }
        catch (Exception x) {
            System.out.println( "main(): caught exception:" + x );
            x.printStackTrace();
        }
    }

}
