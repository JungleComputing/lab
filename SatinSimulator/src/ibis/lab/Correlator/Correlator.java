// File: Reference.java

package ibis.lab.Correlator;

/**
 * Real Satin program with similar behaviour to the one that is simulated.
 * Useful to compare real and simulated statistics.
 * 
 * @author Kees van Reeuwijk.
 */
public class Correlator extends ibis.satin.SatinObject implements
	CorrelatorSatinInterface {
    /** Contractual obligation. */
    private static final long serialVersionUID = 7331296260507993758L;

    private static long[] buildArray(int iter, int number) {
	long add = iter * 100000 + number * 1000;
	long res[] = new long[Constants.PACKET_SIZE];
	for (int i = 0; i < Constants.PACKET_SIZE; i++) {
	    res[i] = i + add;
	}
	return res;
    }

    /**
     * Given two arrays, return a pair, containing the arrays with the elements
     * at input interleaved.
     * 
     * @param a
     *                One of the two input arrays.
     * @param b
     *                The second of the two input arrays.
     * @return A pair containing the two output arrays.
     */
    @Override
    public Pair correlate(long[] a, long[] b) {
	long resa[] = new long[Constants.PACKET_SIZE];
	long resb[] = new long[Constants.PACKET_SIZE];
	int ixa = 0;
	int ixb = 0;

	for (int i = 0; i < Constants.PACKET_SIZE; i += 2) {
	    resa[i] = a[ixa++];
	    resa[i + 1] = b[ixb++];
	}
	for (int i = 0; i < Constants.PACKET_SIZE; i += 2) {
	    resb[i] = a[ixa++];
	    resb[i + 1] = b[ixb++];
	}
	return new Pair(resa, resb);
    }

    /**
     * Given an array, return the sum of its elements.
     * 
     * @param a
     *                The array to sum.
     * @return The sum.
     */
    private static long sum(long a[]) {
	long res = 0;

	for (long v : a) {
	    res += v;
	}
	return res;
    }

    /**
     * The method invoked to start the program.
     * 
     * @param args
     *                Command-line arguments
     */
    public static void main(String[] args) {
	Correlator r = new Correlator();
	long total = 0;

	System.out.println("Started");
	long start = System.currentTimeMillis();
	for (int i = 0; i < Constants.RUN_SIZE; i++) {
	    long a[] = buildArray(i, 0);
	    long b[] = buildArray(i, 1);
	    long c[] = buildArray(i, 2);
	    long d[] = buildArray(i, 3);

	    Pair cab = r.correlate(a, b);
	    Pair ccd = r.correlate(c, d);
	    r.sync();
	    Pair cab1 = r.correlate(cab.a, ccd.a);
	    Pair ccd1 = r.correlate(cab.a, ccd.b);
	    r.sync();
	    total += sum(cab1.a);
	    total += sum(cab1.b);
	    total += sum(ccd1.a);
	    total += sum(ccd1.b);
	}
	r.sync();
	long stop = System.currentTimeMillis();
	System.out.println("Run time: " + (stop - start) + " ms");

    }

}
