package ibis.dachmaestro;

import java.io.Serializable;

/**
 * A pair of images that should be compared.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class ImagePair implements Serializable {

	/** Contractual obligation. */
	private static final long serialVersionUID = -3325234510023855038L;

	/** The 'before' image. */
	public final Image before;

	/** The 'after' image. */
	public final Image after;

	ImagePair(final Image before, final Image after) {
		this.before = before;
		this.after = after;
	}
}
