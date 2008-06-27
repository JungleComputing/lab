package ibis.dachsatin;

import java.io.File;
import java.util.ArrayList;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class Main {

    /**
     * Given a filename, read a list of pairs from that file.
     * @param f The file  to read from.
     * @return The list of pairs.
     */
    private static ImagePair[] readPairs( File f )
    {
        ArrayList<ImagePair> l = new ArrayList<ImagePair>();

        l.add( new ImagePair( "a", "b" ) );
        l.add( new ImagePair( "c", "d" ) );
        l.add( new ImagePair( "e", "f" ) );
        ImagePair res[] = new ImagePair[l.size()];
        l.toArray( res );
        return res;
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main(String[] args) {
	System.out.println( "Hello world" );
    }

}
