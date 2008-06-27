package ibis.dachsatin;
import ibis.util.RunProcess;

import java.io.File;
import java.io.Serializable;

/**
 * A pair of images that should be compared.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class ImagePair implements Serializable {
    /** Contractual obligation. */
    private static final long serialVersionUID = -3325234510023855038L;

    /** The local filename of the 'before' image. */
    final String before;
    
    /** The local filename of the 'after' image. */
    final String after;
    
    ImagePair( final String before, final String after )
    {
        this.before = before;
        this.after = after;
    }

    static String compareImages( String before, String after, File imageDirectory )
    {
        String res = null;

        String comparatorExecutable = System.getenv( "DACHCOMPARATOR" );
        if( comparatorExecutable == null ) {
            comparatorExecutable = "/usr/bin/diff";
        }
        System.out.println( "Comparing '" + before + "' and '" + after + "'" );
        String command[] = {
                comparatorExecutable,
                new File( imageDirectory, before ).getAbsolutePath(),
                new File( imageDirectory, after ).getAbsolutePath()
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
            System.err.println( "Comparison command '" + cmd + "' failed:" );
            System.err.println( new String( p.getStdout() ) );
            System.err.println( new String( p.getStderr() ) );
            return null;
        }
        res = new String( p.getStdout() );
        return res;

    }

    /**
     * Compares the two images, and returns the verdict.
     * @return The result of the comparison.
     */
    String compare( File imageDirectory )
    {
        return compareImages( before, after, imageDirectory );
    }
}
