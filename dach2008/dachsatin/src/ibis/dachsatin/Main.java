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
    private static void readPairs( ArrayList<ImagePair> l, File f )
    {
        l.add( new ImagePair( "a", "b" ) );
        l.add( new ImagePair( "c", "d" ) );
        l.add( new ImagePair( "e", "f" ) );
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main( String[] args )
    {
        ArrayList<ImagePair> l = new ArrayList<ImagePair>();        
        readPairs( l, new File( "dummy" ) );
        ImagePair res[] = new ImagePair[l.size()];
        l.toArray( res );
        Comparator c = new Comparator();
        long start = System.currentTimeMillis();
        String result = c.compareAllPairs( res, 0, res.length );
        System.out.println( result );
    }

}
