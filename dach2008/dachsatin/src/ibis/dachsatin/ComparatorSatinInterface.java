package ibis.dachsatin;

import ibis.satin.Spawnable;
import ibis.util.Pair;

/**
 * The methods that are spawnable.
 * 
 * @author Kees van Reeuwijk
 *
 */
public interface ComparatorSatinInterface extends Spawnable {
    Result compareAllPairs(Pair [] pairs, String command);
}
