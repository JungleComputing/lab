package ibis.lab.Correlator;

import ibis.satin.Spawnable;

/**
 * @author Kees van Reeuwijk
 *
 */
public interface CorrelatorSatinInterface extends Spawnable {
    /**
     * Given two arrays, compute the correlation between the two.
     * For this dummy program that is simply a reshuffle of the
     * array elements.
     * @param a Array a
     * @param b Array b
     * @return The arrays, reshuffled.
     */
    Pair correlate(long[] a, long[] b);
}
