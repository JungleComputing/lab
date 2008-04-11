
package ibis.videoplayer;

/**
 * Settings for the video player.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class Settings {    
    /** The number of frames in a fragment. */
    public static final int FRAME_FRAGMENT_COUNT = 30;
    
    // ----------------------

    static final boolean traceFetcher = false;
    static final boolean traceFragmentBuilder = true;
    static final boolean traceScaler = true;
    static final boolean traceDecompressor = true;

    static boolean traceActions = true;

    static final int FRAME_HEIGHT = 100*2;
    static final int FRAME_WIDTH = 160*2;
}
