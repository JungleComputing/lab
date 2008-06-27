package ibis.dachsatin;
import java.io.Serializable;

/**
 * A pair of images that should be compared.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class ImagePair implements Serializable {
    /** Contractual obligation. */
    private static final long serialVersionUID = -3325234510023855038L;

    /** The local filename of the 'before' image. */
    final String before;
    
    /** The local filename of the 'after' image. */
    final String after;
    
    ImagePair( final String before, final String after )
    {
        this.before = before;
        this.after = after;
    }
    
    /**
     * Compares the two images, and returns the verdict.
     * @return The result of the comparison.
     */
    String compare()
    {
        // For this dummy implementation we just concatename the strings.
        return before + "+" + after +'\n';
    }
}
