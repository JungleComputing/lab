package ibis.dachmaestro;

import ibis.maestro.JobSequence;
import ibis.maestro.ParallelJob;
import ibis.maestro.ParallelJobHandler;
import ibis.util.RunProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 
 * A task that runs an entire dach problem set.
 *
 * @author Kees van Reeuwijk.
 */
class ProblemSetTask implements ParallelJob
{
    private static final long serialVersionUID = 1L;
    private final JobSequence compareJob;
    private final File oracleHome;
    private final File problemsDir;
    private File resultFileName = null;
    private PrintStream resultFile = null;
    private int submittedPairs = 0;
    private int returnedPairs = 0;
    private final boolean verbose;
    private static final Random rng = new Random();

    /** The handle of this problem set run for the oracle. */
    private String handle = null;

    private String errorString = null;

    private static final String oracleName = "dach_api/dach_api";

    private void reportError( String s )
    {
        errorString = s;
        System.out.println( s );
    }

    ProblemSetTask( JobSequence compareJob, File oracleHome, File problemsDir, boolean verbose )
    {
        this.compareJob = compareJob;
        this.oracleHome = oracleHome;
        this.problemsDir = problemsDir;
        this.verbose = verbose;
    }

    /**
     * Returns the result of this problem.
     * In this case a string describing the status of the run.
     * TODO: also return the oracle verdict string.
     * @return The result.
     */
    @Override
    public Object getResult()
    {
        if( errorString != null ) {
            return "ERROR: " + errorString + "\n";
        }
        String command [] = {
            oracleHome + "/" + oracleName,
            "--check_ans",
            handle,
            resultFileName.getAbsolutePath()
        };

        RunProcess p = new RunProcess( command );
        p.run();

        int exit = p.getExitStatus();

        if( exit != 0 ) {
            String cmd = joinStringList( command );

            return "ERROR: command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() );
        }
        String verdict = new String( p.getStdout() );

        return "OK: " + verdict;
    }

    /**
     * Given the name of a problem set, generate jobs for all comparison pairs.
     * @param input The name of the problem set.
     * @param handler The handler.
     */
    @Override
    public void map( Object input, ParallelJobHandler handler )
    {
        String problemSet = (String) input;

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

            reportError(
                "command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() ) );
            return;
        }
        String oracleOutput = new String( p.getStdout() ).trim();
        System.out.println( "oracle output for problem '" + problemSet + "' is '" + oracleOutput + "'" );
        String words[] = oracleOutput.split( " " );
        handle = words[0];
        File directory = new File( problemsDir, words[1].trim() );
        System.out.println( "Getting problem pairs from directory " + directory );
        PriorityQueue<FilePair> pairs = FindPairs.getPairs( directory, verbose );

        if (pairs.isEmpty() ) { 
            reportError( "No pairs found in directory " + directory );
            return;
        }
        System.out.println( "I now have " + pairs.size() + " pairs" );

        try{
            long now = System.currentTimeMillis();
            resultFileName = new File( "result-" + problemSet + "-" + String.format( "%D-%tT", now, now ) + "-" + rng.nextDouble() + ".txt" );
            resultFile = new PrintStream( new FileOutputStream( resultFileName ) );
        }
        catch( IOException e ){
            reportError( "I/O error: " + e.getLocalizedMessage() );
            return;
        }

        if (verbose) { 
            System.out.printf("Starting comparison of " + pairs.size() + " pairs.");
        }
        int serial = 0;

        
        while( !pairs.isEmpty() ) {
            FilePair pair = pairs.remove();
            pair.serial = serial++;
            Integer label = pair.serial;
            handler.submit( pair, label, true, compareJob );
        }
        submittedPairs = serial;
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

    /**
     * Add the given result to our collection.
     * We write it to the result file.
     * @param id The identifier of the result. 
     * @param resultObject The result.
     */
    @Override
    public void reduce( Object id, Object resultObject )
    {
        Result result = (Result) resultObject;
        resultFile.append( result.result );
        returnedPairs++;
        System.out.println( "I now have " + returnedPairs + " of " + submittedPairs + " solutions" );
    }

    /** Returns true iff this context can support this task.
     * We simply test whether the oracle is visible.
     * @return True iff we can run this task.
     */
    @Override
    public boolean isSupported()
    {
        String hardwarename = System.getProperty( "hardwarename" );
        boolean supported = true;
        if( hardwarename.equals( "x86_64" ) ) {
            supported = false;
        }
        System.out.println( "ProblemSetTask: hardwarename=" + hardwarename + " supported=" + supported );
        return supported;
    }

}
