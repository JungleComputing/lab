package ibis.lab.Correlator;

/**
 * 
 * @author Kees van Reeuwijk
 * 
 * A wrapper for a pair of arrays.
 */
public class Pair {
    final long a[];
    final long b[];

    Pair(long a[], long b[]) {
	this.a = a;
	this.b = b;
    }
}
