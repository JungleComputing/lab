package ibis.dachmaestro;

import java.io.File;

import ibis.maestro.MapReduceHandler;
import ibis.maestro.MapReduceTask;
import ibis.maestro.Service;
import ibis.util.RunProcess;

public class ProblemSetTask implements MapReduceTask
{
    private final File oracleHome;
    private static final String oracleName = "dach-api";
    
    /** The handle of this problem set run for the oracle. */
    private String handle = null;
    
    private String errorString = null;

    private void reportError( String s )
    {
	errorString = s;
	System.err.println( s );
    }

    ProblemSetTask( File oracleHome )
    {
	this.oracleHome = oracleHome;
    }

    /**
     * Returns the result of this problem.
     * @return
     */
    @Override
    public Object getResult()
    {
	if( errorString == null ) {
	    return "OK";
	}
	else {
	    return "ERROR: " + errorString;
	}
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
	long startTime = System.nanoTime();

	String command [] = {
                oracleHome + "/" + oracleName,
                "--getproblem",
                problemSet
            };

            RunProcess p = new RunProcess(command);
            p.run();
            long time = System.nanoTime()-startTime;

            int exit = p.getExitStatus();

            if( exit != 0 ) {
                String cmd = "";

                for (String c: command) {
                    if (!cmd.isEmpty()) {
                        cmd += ' ';
                    }
                    cmd += c;
                }

                reportError(
                    "Oracle access command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
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
    }

    /**
     * Add the given result to our collection.
     * We write it to the result file.
     * @param id The identifier of the result. 
     * @param result The result.
     */
    @Override
    public void reduce( Object id, Object resultObject )
    {
	Result result = (Result) resultObject;
    }

    @Override
    public String getName() {
	// TODO: Auto-generated method stub
	return null;
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
