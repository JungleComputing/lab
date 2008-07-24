package ibis.dachmaestro;

import ibis.maestro.AtomicTask;
import ibis.maestro.Node;
import ibis.maestro.Service;
import ibis.util.RunProcess;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class FileComparatorTask implements AtomicTask
{
    /** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;
    private final String exec;
    

    /**
     * Returns the name of this task.
     * @return The name.
     */
    @Override
    public String getName()
    {
	return "Compare files";
    }

    FileComparatorTask( String exec ) throws Exception
    {
	this.exec = exec;
    }

    private Result compare( FilePair pair )
    {
        System.out.println( "Comparing files '" + pair.before + "' and '" + pair.after + "'" );
        long startTime = System.nanoTime();

        // FIXME: specify temp directory.
        String command [] = {
            exec,
            pair.before.getAbsolutePath(),
            pair.after.getAbsolutePath()
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

            return new Result(
        	null,
        	time,
                "Comparison command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                    + " stderr: " + new String( p.getStderr() )
            );
        }

        System.out.println("Completed '" + pair.before + "' and '" + pair.after + "' in " + Service.formatNanoseconds( time ) );

        return new Result( new String( p.getStdout() ), time, null );
    }

    /**
     * Returns true iff we can support this task.
     * @return Whether we can support this task.
     */
    @Override
    public boolean isSupported()
    {
	return true;
    }

    /**
     * Runs this task. Specifically: given a pair, runs the comparator.
     * @param in The input.
     * @param node The node this runs on.
     * @return The comparison result.
     */
    @Override
    public Object run( Object in, Node node )
    {
	FilePair pair = (FilePair) in;
	return compare( pair );
    }

}
