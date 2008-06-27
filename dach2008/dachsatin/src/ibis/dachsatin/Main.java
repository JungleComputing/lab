package ibis.dachsatin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
        try {
            BufferedReader br = new BufferedReader( new FileReader( f ) );
            while( true ) {
                String line = br.readLine();
                if( line == null ) {
                    break;
                }
                String parts[] = line.split( " " );
                if( parts.length != 2 ) {
                    System.err.println( "Malformed image pair [" + line + "]: it has " + parts.length + " parts, not two" );
                    System.exit( 1 );
                }
                l.add( new ImagePair( parts[0], parts[1] ) );
            }
            br.close();
        }
        catch( FileNotFoundException x ) {
            System.err.println( "File '" + f + "' not found:" + x.getLocalizedMessage() );
            System.exit( 1 );
        }
        catch( IOException x ) {
            System.err.println( "Cannot read file: '" + f + "':" + x.getLocalizedMessage() );
            System.exit( 1 );
        }
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main( String[] args )
    {
        ArrayList<ImagePair> l = new ArrayList<ImagePair>();
        for( String fnm: args ) {
            readPairs( l, new File( fnm ) );
        }
        if( l.size() == 0 ) {
            System.err.println( "Empty image list??" );
            System.exit( 1 );
        }
        ImagePair res[] = new ImagePair[l.size()];
        l.toArray( res );
        Comparator c = new Comparator();
        String result = c.compareAllPairs( res, 0, res.length );
        System.out.println( result );
    }

}
