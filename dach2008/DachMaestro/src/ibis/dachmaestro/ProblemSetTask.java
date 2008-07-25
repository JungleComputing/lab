package ibis.dachmaestro;

import ibis.maestro.Job;
import ibis.maestro.MapReduceHandler;
import ibis.maestro.MapReduceTask;
import ibis.util.RunProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * 
 * A task that runs an entire dach problem set.
 *
 * @author Kees van Reeuwijk.
 */
class ProblemSetTask implements MapReduceTask
{
    private static final long serialVersionUID = 1L;
    private final Job compareJob;
    private final File oracleHome;
    private File resultFileName = null;
    private PrintStream resultFile = null;
    private final boolean verbose;

    /** The handle of this problem set run for the oracle. */
    private String handle = null;

    private String errorString = null;

    private static final String oracleName = "dach-api";

    private void reportError( String s )
    {
        errorString = s;
        System.err.println( s );
    }

    ProblemSetTask( Job compareJob, File oracleHome, boolean verbose )
    {
        this.compareJob = compareJob;
        this.oracleHome = oracleHome;
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
            return "ERROR: " + errorString;
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
            String cmd = "";

            for( String c: command ) {
                if( !cmd.isEmpty() ) {
                    cmd += ' ';
                }
                cmd += c;
            }

            return "ERROR: command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() );
        }
        String verdict = new String( p.getStdout() );

        return "OK: " + verdict;
    }

    /**
     * Given the name of a problem set, generate jobs for all comparison pairs.
     * @param input The name of the problem set.
     * @param arg1 The handler.
     */
    @Override
    public void map( Object input, MapReduceHandler arg1 )
    {
        String problemSet = (String) input;

        String command [] = {
            oracleHome + "/" + oracleName,
            "--get_problem",
            problemSet
        };

        RunProcess p = new RunProcess( command );
        p.run();

        int exit = p.getExitStatus();

        if( exit != 0 ) {
            String cmd = "";

            for( String c: command ) {
                if( !cmd.isEmpty() ) {
                    cmd += ' ';
                }
                cmd += c;
            }

            reportError(
                "command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() ) );
            return;
        }
        String oracleOutput = new String( p.getStdout() );
        String words[] = oracleOutput.split( " " );
        handle = words[0];
        File directory = new File( oracleHome, words[1] );
        if( !directory.exists() ) {
            reportError( "Problem directory '" + directory + "' does not exist" );
            return;
        }
        ArrayList<FilePair> pairs = FindPairs.getPairs( directory, verbose );

        if (pairs.isEmpty() ) { 
            System.err.println("No pairs found in directory " + directory );
            System.exit(1);
        }

        try{
            resultFileName = File.createTempFile( "result-" + problemSet, ".txt" );
            resultFileName.deleteOnExit();
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
        for( FilePair pair: pairs ) {
            arg1.submit( compareJob, pair, serial++ );
        }
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
    }

    /**
     * Returns the name of this task.
     * @return The name of  this task.
     */
    @Override
    public String getName()
    {
        return "Problem set";
    }

    /** Returns true iff this context can support this task.
     * We simply test whether the oracle is visible.
     * @return True iff we can run this task.
     */
    @Override
    public boolean isSupported()
    {
        File f = new File( oracleHome, oracleName );
        return f.exists();
    }

}
