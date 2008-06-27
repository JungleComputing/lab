/**
 * 
 */
package ibis.dachsatin;

import ibis.satin.SatinObject;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk
 *
 */
public class Comparator extends SatinObject implements ComparatorSatinInterface {
    /** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;

    /**
     *
     * @param pairs The list of pairs to compare.
     * @return The list of comparison results.
     */
    public String compareAllPairs(ImagePair[] pairs, int from, int to  ){
        if( from+1 == to ){
            return pairs[from].compare();
        }
        int mid = (from+to)/2;
        String resa = compareAllPairs(pairs, from, mid );
        String resb = compareAllPairs(pairs, mid, to );
        sync();
        return resa+resb;
    }

}
