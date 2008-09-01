package ibis.dachmaestro;

import ibis.maestro.Job;
import ibis.maestro.JobCompletionListener;
import ibis.maestro.JobList;
import ibis.maestro.LabelTracker;
import ibis.maestro.Node;
import ibis.maestro.Utils;
import ibis.maestro.LabelTracker.Label;
import ibis.util.RunProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class OneProblemProgram
{
    private static final Random rng = new Random();
    private static int submittedPairs = 0;
    private static int returnedPairs = 0;
    private static File resultFileName = null;
    private static PrintStream resultFile = null;
    private static File oracleHome;
    private static String handle;
    private static File separateResultDir = null;

    private static final String DEFAULT_ORACLE_HOME = "/home/dach911";
    private static final String DEFAULT_PROBLEMS_DIR = "/tmp/dach001";
    private static final String oracleName = "dach_api/dach_api";

    private static void usage()
    { 
        System.err.println( "Usage: Main <problem>" );
        System.exit(1);
    }
    
    private static void writeSeparateResultFile( Node node, String result, String label )
    {
        PrintStream s = null;
        FileOutputStream fos = null;

        try {
            File f = new File( separateResultDir, "result-" + label + ".txt" );
            fos = new FileOutputStream( f );
            s = new PrintStream( fos );
            s.append( result );
        }
        catch( IOException e ){
            node.reportInternalError( "I/O error: " + e.getLocalizedMessage() );
        }
        finally {
            if( s != null ){
                s.close();
            }
            if( fos != null ){
                try {
                    fos.close();
                }
                catch( IOException x ){
                    // Nothing to do.
                }
            }
        }
    }

    private static class Listener implements JobCompletionListener
    {
        private final LabelTracker labelTracker = new LabelTracker();
        private final HashSet<LabelTracker.Label> returnedResults = new HashSet<LabelTracker.Label>();
        private boolean sentFinal = false;
        private int failures = 0;

        /** Handle the completion of task with identifier 'id': the result is 'result'.
         * @param node The node we're running this on.
         * @param id The task that was completed.
         * @param resultObject The result of the task.
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void jobCompleted( Node node, Object id, Object resultObject )
        {
            if( !(id instanceof LabelTracker.Label) ){
                node.reportInternalError( "Object id is not a tracker label but a " + resultObject.getClass() + ": " + id );
                System.exit( 1 );
            }
            if( !(resultObject instanceof Result) ) {
                node.reportInternalError( "Internal error: result is not a Result but a " + resultObject.getClass() + ": " + resultObject );
                System.exit( 1 );
            }
            LabelTracker.Label label = (LabelTracker.Label) id;
            labelTracker.returnLabel( label );
            Result result = (Result) resultObject;
            if( result.error == null ) {
                // All went well.
                if( returnedResults.contains( label ) ) {
                    node.reportProgress( "Duplicate result ignored" );
                }
                else {
                    synchronized( resultFile ) {
                        resultFile.append( result.result );
                    }
                    writeSeparateResultFile( node, result.result, result.pair.label );
                    returnedPairs++;
                    node.reportProgress( "Problem " + label + " took " + Utils.formatNanoseconds( result.computeTime ) );
                }
            }
            else {
                node.reportError( "Comparison failed: " + result.error );
                failures++;
            }
            long issuedLabels = labelTracker.getIssuedLabels();
            long returnedLabels = labelTracker.getReturnedLabels();
            node.reportProgress( "I now have " + returnedLabels + " of " + issuedLabels + " pairs, " + failures + " failures" );
            if( (issuedLabels-returnedLabels)<20 ) {
                Label[] labels = labelTracker.listOutstandingLabels();
                
                node.reportProgress( "Still missing: " + Arrays.deepToString( labels ) );
            }
            returnedResults.add( label );
            if( sentFinal && labelTracker.allAreReturned() ) {
                node.reportProgress( "I got all job results back; stopping program" );
                node.setStopped();
                resultFile.close();
                reportResultToOracle( node );
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
     * @param l
     * @return
     */
    private static String joinStringList( String[] l )
    {
        String cmd = "";

        for( String c: l ) {
            if( !cmd.isEmpty() ) {
                cmd += ' ';
            }
            cmd += c;
        }
        return cmd;
    }

    private static boolean submitProblem( Node node, String problem, File problemsDir, Job compareJob, boolean verbose, Listener listener )
    {
        String problemSet = problem;

        String command [] = {
            oracleHome + "/" + oracleName,
            "--get_problem",
            problemSet
        };

        node.reportProgress( "Executing " + joinStringList(command) );
        RunProcess p = new RunProcess( command );
        p.run();

        int exit = p.getExitStatus();

        node.reportProgress( "Execution finished" );
        if( exit != 0 ) {
            String cmd = joinStringList( command );

            node.reportError(
                "command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() ) );
            return false;
        }
        String oracleOutput = new String( p.getStdout() ).trim();
        node.reportProgress( "oracle output for problem '" + problemSet + "' is '" + oracleOutput + "'" );
        String words[] = oracleOutput.split( " " );
        handle = words[0];
        File directory = new File( problemsDir, words[1].trim() );
        node.reportProgress( "Getting problem pairs from directory " + directory );
        PriorityQueue<FilePair> pairs = FindPairs.getPairs( directory, verbose );

        if (pairs.isEmpty() ) { 
            node.reportError( "No pairs found in directory " + directory );
            return false;
        }

        node.reportProgress( "There are " + pairs.size() + " pairs" );

        try{
            double lbl = rng.nextDouble();

            separateResultDir = new File( "results-" + handle + "-" + problemSet + "-" + lbl );
            resultFileName = new File( "result-" + handle + "-" + problemSet + "-" + lbl + ".txt" );
            resultFile = new PrintStream( new FileOutputStream( resultFileName ) );
        }
        catch( IOException e ){
            node.reportInternalError( "I/O error: " + e.getLocalizedMessage() );
            return false;
        }

        if (verbose) {
            node.reportProgress( "Starting comparison of " + pairs.size() + " pairs." );
        }

        while( !pairs.isEmpty() ) {
            FilePair pair = pairs.remove();
            Object label = listener.getLabel();
            compareJob.submit( node, pair, label, listener );
            submittedPairs++;
        }
        listener.setFinished();
        return true;
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    @SuppressWarnings("synthetic-access")
    public static void main( String[] args )
    {
        boolean goForMaestro = false;
        String problem = null;

        try {
            boolean verbose = false;
            String command = null;
            String oracleHomeName = DEFAULT_ORACLE_HOME;
            int waitNodes = 0;
            String problemsDirName = DEFAULT_PROBLEMS_DIR;

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
                else if (args[i].equals("-w") || args[i].equals("--waitnodes")) {
                    waitNodes = Integer.parseInt(  args[++i] );
                }
                else if (args[i].equals("-p") || args[i].equals("--problemsdir")) {
                    problemsDirName = args[++i];
                }
                else {
                    System.out.println( "Problemset '" + args[i] + "'" );
                    if( problem != null ) {
                        System.err.println( "More than one problem specified: " + args[i] + " and " + problem );
                        System.exit( 1 );
                    }
                    problem = args[i];
                    goForMaestro = true;
                }
            }
            if (command == null) {
                command = System.getenv( "DACHCOMPARATOR" );

                if (command == null ) {
                    System.err.println( "No comparison command given, and DACHCOMPARATOR environement variable is not set" );
                    System.exit( 1 );	
                }
            }
            oracleHome = new File( oracleHomeName );
            File problemsDir = new File( problemsDirName );

            if (command == null) {
                command = System.getenv( "DACHCOMPARATOR" );

                if (command == null ) {
                    System.err.println( "No comparison command given, and DACHCOMPARATOR environement variable is not set" );
                    System.exit( 1 );	
                }
            }

            JobList jobs = new JobList();
            Job job = jobs.createJob(
                "comparison",
                new FileComparatorTask( command )
            );
            Listener listener = new Listener();


            Node node = new Node( jobs, goForMaestro );

            System.out.println( "Node created" );
            long startTime = System.nanoTime();
            if( node.isMaestro() ) {
                boolean goodToSubmit = true;
                if( waitNodes>0 ) {
                    node.reportProgress( "Waiting for " + waitNodes + " ready nodes" );
                    long deadline = 5*60*1000; // 5 minutes in ms
                    int n = node.waitForReadyNodes( waitNodes, deadline );
                    node.reportProgress( "Continuing; there are now " + n + " ready nodes" );
                    if( n*3<waitNodes ) {
                        node.reportProgress( "That's less than a third of the required nodes (" + waitNodes + "); giving up" );
                        goodToSubmit = false;
                    }
                }
                if( goodToSubmit ) {
                    boolean ok = submitProblem( node, problem, problemsDir, job, verbose, listener );
                    if( ok ) {
                        node.reportProgress( "Jobs submitted" );
                    }
                    else {
                        node.reportError( "Could not submit jobs" );
                        node.setStopped();
                    }
                }
            }
            node.waitToTerminate();
            long stopTime = System.nanoTime();
            node.reportProgress( "Duration of this run: " + Utils.formatNanoseconds( stopTime-startTime ) );
        }
        catch (Exception x) {
            System.out.println( "main(): caught exception:" + x );
            x.printStackTrace();
        }
    }

    private static void reportResultToOracle( Node node )
    {
        String reportCommand [] = {
            oracleHome + "/" + oracleName,
            "--check_ans",
            handle,
            resultFileName.getAbsolutePath()
        };

        RunProcess p = new RunProcess( reportCommand );
        p.run();

        int exit = p.getExitStatus();

        if( exit != 0 ) {
            String cmd = joinStringList( reportCommand );

            node.reportError( "ERROR: command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
            + " stderr: " + new String( p.getStderr() ) );
            return;
        }
        String verdict = new String( p.getStdout() );
        node.reportProgress( "Oracle verdict: " + verdict );
    }

}
