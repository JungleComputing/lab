package ibis.dachmaestro;

import java.io.File;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Utility class that finds file pair in a given directory. 
 * 
 * @author Jason Maassen, Kees van Reeuwijk
 *
 */
public class FindPairs {

    private static final String FITS_EXTENSION = ".fits";

    private static void addFile( PriorityQueue<FilePair> pairs, HashMap<String, File> singles, File f, boolean verbose)
    {
	String name = f.getName();

	if( verbose ) { 
	    System.out.println( "Adding file: " + f.getName() );
	}

	// Remove the '.fits' extension.
	name = name.substring( 0, name.length()-5 ); 

	String label = name.substring( 0, name.length()-3 );
	if( name.endsWith( "t0" ) ) {
	    File tmp = singles.remove( label );

	    if (tmp != null) { 
		pairs.add( new FilePair( f, tmp, label ) );
	    } else { 
		singles.put( label, f );
	    }

	}
	else if( name.endsWith( "t1" ) ) { 
	    File tmp = singles.remove( label );

	    if (tmp != null) { 
		pairs.add( new FilePair( tmp, f, label ) );
	    } else { 
		singles.put( label, f );
	    }
	}
	else { 
	    if( verbose ){ 
		System.out.println( "Cannot handle file: " + f );
	    }
	}
    }

    /**
     * Given a directory, returns the pairs that occur in that directory.
     * @param directory The directory to examine.
     * @param verbose Trace the proceedings of this method?
     * @return The pairs in the directory.
     */
    static PriorityQueue<FilePair> getPairs( File directory, boolean verbose )
    {
	final HashMap<String, File> single = new HashMap<String, File>();

	final PriorityQueue<FilePair> pairs = new PriorityQueue<FilePair>();

	File [] files = directory.listFiles();
        if( files == null ){
            System.out.println( "Directory " + directory + " does not exist" );
            return pairs;
        }

	for( File f : files ) { 

	    if( f.getName().endsWith( FITS_EXTENSION ) ) { 
		addFile( pairs, single, f, verbose );
	    }
	    else {
		if (verbose) { 
		    System.out.println("Skipping: " + f);
		}
	    }
	}

	if (verbose && single.size() > 0) { 
	    System.out.println("Unpaired files:");

	    for (String k : single.keySet()) { 
		System.out.println( k );
	    }

	    single.clear();
	}

	return pairs;
    }
}
