package ibis.dachmaestro;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class that finds file pair in a given directory. 
 * 
 * @author Jason Maassen, Kees van Reeuwijk
 *
 */
public class FindPairs {

    private static void addFile( ArrayList<Pair> pairs, HashMap<String, File> singles, File f, boolean verbose)
    {
	String name = f.getName();

	if (verbose) { 
	    System.out.println("Adding file: " + f.getName());
	}

	// Remove the '.fits' extension.
	name = name.substring( 0, name.length()-5 ); 

	if( name.endsWith( "t0" ) ) {

	    String other = name.substring(0, name.length()-2) + "t1";

	    File tmp = singles.remove(other);

	    if (tmp != null) { 
		pairs.add( new Pair( f, tmp ) );
	    } else { 
		singles.put( name, f );
	    }

	}
	else if (name.endsWith("t1")) { 

	    String other = name.substring(0, name.length()-2) + "t0";

	    File tmp = singles.remove(other);

	    if (tmp != null) { 
		pairs.add( new Pair( tmp, f ) );
	    } else { 
		singles.put( name, f );
	    }
	}
	else { 
	    if (verbose) { 
		System.out.println("Cannot handle file: " + f);
	    }
	}
    }

    /**
     * Given a directory, returns the pairs that occur in that directory.
     * @param directory The directory to examine.
     * @param verbose Trace the proceedings of this method?
     * @return The pairs in the directory.
     */
    static ArrayList<Pair> getPairs( File directory, boolean verbose )
    {
	final HashMap<String, File> single = new HashMap<String, File>();

	final ArrayList<Pair> pairs = new ArrayList<Pair>();

	File [] files = directory.listFiles();

	for (File f : files) { 

	    if (f.getName().endsWith(".fits")) { 
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


