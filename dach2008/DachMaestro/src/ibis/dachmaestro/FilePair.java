package ibis.dachmaestro;

import java.io.File;
import java.io.Serializable;

/**
 * A pair of images that should be compared.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class FilePair implements Serializable {

	/** Contractual obligation. */
	private static final long serialVersionUID = -3325234510023855038L;

	/** The local filename of the 'before' file. */
	public final File before;

	/** The local filename of the 'after' file. */
	public final File after;

	FilePair(final File before, final File after) {
		this.before = before;
		this.after = after;
	}
}
