package ibis.dachmaestro;

import ibis.maestro.CompletionListener;
import ibis.maestro.Job;
import ibis.maestro.JobList;
import ibis.maestro.LabelTracker;
import ibis.maestro.Node;
import ibis.maestro.Service;
import ibis.util.RunProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
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

    private static final String DEFAULT_ORACLE_HOME = "/home/dach911";
    private static final String DEFAULT_PROBLEMS_DIR = "/tmp/dach001";
    private static final String oracleName = "dach_api/dach_api";

    private static void usage()
    { 
        System.err.println( "Usage: Main <problem>" );
        System.exit(1);
    }

    private static class Listener implements CompletionListener
    {
        private final LabelTracker labelTracker = new LabelTracker();
        private final HashSet<LabelTracker.Label> returnedResults = new HashSet<LabelTracker.Label>();
        private boolean sentFinal = false;

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
                System.err.println( "Internal error: Object id is not a tracker label but a " + resultObject.getClass() + ": " + id );
                System.exit( 1 );
            }
            if( !(resultObject instanceof Result) ) {
                System.err.println( "Internal error: result is not a Result but a " + resultObject.getClass() + ": " + resultObject );
                System.exit( 1 );
            }
            LabelTracker.Label label = (LabelTracker.Label) id;
            Result result = (Result) resultObject;
            if( result.error == null ) {
                // All went well.
                if( returnedResults.contains( label ) ) {
                    System.out.println( "Duplicate result ignored" );
                }
                else {
                    resultFile.append( result.result );
                    returnedPairs++;
                    System.out.println( "Problem " + label + " took " + Service.formatNanoseconds( result.computeTime ) );
                    System.out.println( "I now have " + returnedPairs + " of " + submittedPairs + " solutions" );
                    returnedResults.add( label );
                }
            }
            else {
                System.err.println( "Comparison failed: " + result.error );
            }
            labelTracker.returnLabel( label );
            if( sentFinal && labelTracker.allAreReturned() ) {
                System.out.println( "I got all job results back; stopping program" );
                node.setStopped();
                resultFile.close();
                reportResultToOracle();
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

        System.out.println( "Executing " + joinStringList(command) );
        RunProcess p = new RunProcess( command );
        p.run();

        int exit = p.getExitStatus();

        System.out.println( "Execution finished" );
        if( exit != 0 ) {
            String cmd = joinStringList( command );

            System.err.println(
                "command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() ) );
            return false;
        }
        String oracleOutput = new String( p.getStdout() ).trim();
        System.out.println( "oracle output for problem '" + problemSet + "' is '" + oracleOutput + "'" );
        String words[] = oracleOutput.split( " " );
        handle = words[0];
        File directory = new File( problemsDir, words[1].trim() );
        System.out.println( "Getting problem pairs from directory " + directory );
        PriorityQueue<FilePair> pairs = FindPairs.getPairs( directory, verbose );

        if (pairs.isEmpty() ) { 
            System.err.println( "No pairs found in directory " + directory );
            return false;
        }

        System.out.println( "There are " + pairs.size() + " pairs" );

        try{
            resultFileName = new File( "result-" + problemSet + "-" + rng.nextDouble() + ".txt" );
            resultFile = new PrintStream( new FileOutputStream( resultFileName ) );
        }
        catch( IOException e ){
            System.err.println( "I/O error: " + e.getLocalizedMessage() );
            return false;
        }

        if (verbose) { 
            System.out.println( "Starting comparison of " + pairs.size() + " pairs." );
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
                    boolean ok = submitProblem( node, problem, problemsDir, job, verbose, listener );
                    if( ok ) {
                        System.out.println( "Jobs submitted" );
                    }
                    else {
                        System.err.println( "Could not submit jobs" );
                        node.setStopped();
                    }
                }
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

    /**
     * @param oracleHome
     */
    private static void reportResultToOracle()
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

            System.err.println( "ERROR: command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
            + " stderr: " + new String( p.getStderr() ) );
            System.exit( 1 );
        }
        String verdict = new String( p.getStdout() );
        System.out.println( "Oracle verdict: " + verdict );
    }

}
