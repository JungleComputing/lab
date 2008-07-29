package ibis.dachsatin.worker;

import java.util.ArrayList;
import ibis.satin.Spawnable;

/**
 * The methods that are spawnable.
 * 
 * @author Kees van Reeuwijk
 *
 */
public interface ComparatorSatinInterface extends Spawnable {
    ArrayList<Result> compareAllPairs(ArrayList<Pair> pairs);
}
