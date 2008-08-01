package ibis.dachmaestro;

import java.io.File;
import java.io.Serializable;

/**
 * A pair of images that should be compared.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class FilePair implements Serializable, Comparable<FilePair> {

    /** Contractual obligation. */
    private static final long serialVersionUID = -3325234510023855038L;

    /** The local filename of the 'before' file. */
    public final File before;

    /** The local filename of the 'after' file. */
    public final File after;

    FilePair( final File before, final File after ) {
	this.before = before;
	this.after = after;
    }

    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * 
     * Since this comparator implements ordering for the priority queue,
     * and since the least element is removed first, we return -1 if
     * this object has a larger size than the other one, and vice versa.
     * @param other The other file pair to compare to.
     * @return The comparison verdict.
     */
    @Override
    public int compareTo( FilePair other ) {
	long thisLength = this.totalLength();
	long otherLength = other.totalLength();
	int res = 0;
	if( thisLength>otherLength ) {
	    res = -1;
	}
	else if( thisLength<otherLength ) {
	    res = 1;
	}
	return res;
    }

    long totalLength()
    {
	return before.length() + after.length();
    }
}
