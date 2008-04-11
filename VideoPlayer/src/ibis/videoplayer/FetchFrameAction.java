package ibis.videoplayer;

import java.io.Serializable;
import java.util.Random;

/**
 * A job to fetch a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class FetchFrameAction implements Serializable
{
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch. */
    private final int frameno;
    private static final Random rng = new Random();

    FetchFrameAction( int frameno )
    {
        this.frameno = frameno;
    }

    /** Runs this action.
     * @return The frame we have fetched.
     */
    public Frame run()
    {
        final int sz = Settings.FRAME_WIDTH*Settings.FRAME_HEIGHT;
        short r[] = new short[sz];
        short g[] = new short[sz];
        short b[] = new short[sz];
        for( int i=0; i<r.length; i++ ){
            r[i] = (short) (rng.nextInt() & 0xFFFF);
            g[i] = (short) (rng.nextInt() & 0xFFFF);
            b[i] = (short) (rng.nextInt() & 0xFFFF);
        }
        Frame frame = new Frame( frameno, Settings.FRAME_WIDTH, Settings.FRAME_HEIGHT, r, g, b );
        if( Settings.traceActions ) {
            System.out.println( "Fetched " + frame );
        }
	return frame;
    }
}
