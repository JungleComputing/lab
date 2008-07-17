package ibis.dachmaestro;

import ibis.maestro.Node;
import ibis.maestro.Task;
import ibis.util.RunProcess;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Comparator implements Task {

    /** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;
    private final String exec;
    
    Comparator( String exec ) throws Exception
    {
	this.exec = exec;
        exec = System.getenv("DACHCOMPARATOR");

        if (exec == null ) {
            throw new Exception( "No comparison command as parameter or in DACHCOMPARATOR" );
        }
    }

    private String compare( Pair pair )
    {
        System.out.println("Comparing '" + pair.before + "' and '" + pair.after + "'");

        String command [] = {
            exec,
            pair.before.getAbsolutePath(),
            pair.after.getAbsolutePath()
        };

        RunProcess p = new RunProcess(command);
        p.run();

        int exit = p.getExitStatus();

        if (exit != 0) {
            String cmd = "";

            for (String c: command) {
                if (!cmd.isEmpty()) {
                    cmd += ' ';
                }
                cmd += c;
            }

            return "Comparison command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
            + " stderr: " + new String(p.getStderr());
        }

        System.out.println("Completed '" + pair.before + "' and '" + pair.after + "'");

        return new String(p.getStdout());
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
	Pair pair = (Pair) in;
	return compare( pair );
    }

}
