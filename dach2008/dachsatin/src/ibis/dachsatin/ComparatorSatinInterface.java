package ibis.dachsatin;

import ibis.satin.Spawnable;

/**
 * The methods that are spawnable.
 * 
 * @author Kees van Reeuwijk
 *
 */
public interface ComparatorSatinInterface extends Spawnable {
    String compareAllPairs( ImagePair pairs[], int from, int to );
}
