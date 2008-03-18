
package ibis.videoplayer;

/**
 * Settings for the video player.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class Settings {

    /** The number of bytes in a frame. */
    public static final int FRAME_SAMPLE_COUNT = 10000;
    
    /** The number of frames in a fragment. */
    public static final int FRAME_FRAGMENT_COUNT = 25;
    
    // ----------------------

    static final boolean traceFetcher = true;
    static final boolean traceFragmentBuilder = true;
    static final boolean traceScaler = true;
}
