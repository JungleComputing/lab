package ibis.dachsatin;

import java.io.File;

import ibis.satin.Spawnable;

/**
 * The methods that are spawnable.
 * 
 * @author Kees van Reeuwijk
 *
 */
public interface ComparatorSatinInterface extends Spawnable {
    String compareAllPairs( ImagePair pairs[], int from, int to, File imageDirectory );
}
