package ibis.dachmaestro;

import java.io.File;
import java.io.IOException;

import ibis.maestro.Node;
import ibis.maestro.Task;
import ibis.maestro.Service;
import ibis.util.RunProcess;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class ImageComparatorTask implements Task {

    /** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;
    private final String exec;
    private final File scratchDirectory;

    ImageComparatorTask( String exec, File scratchDirectory ) throws Exception
    {
	this.exec = exec;
	this.scratchDirectory = scratchDirectory;
    }

    private Result compare( ImagePair pair )
    {
        File beforeFile;
        File afterFile;

        System.out.println( "Comparing files '" + pair.before + "' and '" + pair.after + "'" );
        try {
            beforeFile = pair.before.write( scratchDirectory );
        }
        catch( IOException x ) {
            String error = "Could not write file '" + pair.before.file + "': " + x.getLocalizedMessage();
            return new Result( null, 0l, error );
        }
        try {
            afterFile = pair.after.write( scratchDirectory );
        }
        catch( IOException x ) {
            String error = "Could not write file '" + pair.after.file + "': " + x.getLocalizedMessage();
            return new Result( null, 0l, error );
        }
        long startTime = System.nanoTime();

        String command [] = {
            exec,
            beforeFile.getAbsolutePath(),
            afterFile.getAbsolutePath()
        };

        RunProcess p = new RunProcess( command );
        p.run();
        beforeFile.delete();
        afterFile.delete();
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
	if( !(in instanceof ImagePair) ) {
	    System.err.println( "Internal error: ImageComparatorTask requires an ImagePair, but got a " + in.getClass() );
	}
	ImagePair pair = (ImagePair) in;
	return compare( pair );
    }

}
