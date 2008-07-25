package ibis.dachsatin;

import java.util.ArrayList;
import ibis.satin.Spawnable;
import ibis.util.Pair;

/**
 * The methods that are spawnable.
 * 
 * @author Kees van Reeuwijk
 *
 */
public interface ComparatorSatinInterface extends Spawnable {
    ArrayList<Result> compareAllPairs(ArrayList<Pair> pairs);
}
