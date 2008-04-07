package ibis.videoplayer;

import ibis.maestro.JobType;

import java.io.Serializable;

/**
 * An action to decompress a frame. We fake decompressing a video frame
 * by simply doubling the frame and repeating the content.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class DecompressFrameAction implements Serializable
{
    private static final long serialVersionUID = -3938044583266505212L;

    /** How many times should I repeat the fake decompression loop to approximate
     * the real decompression process.
     */
    private static final int REPEAT = 4;

    /** The frame to decompress. */
    private final Frame frame;

    DecompressFrameAction( Frame frame )
    {
	this.frame = frame;
    }

    /**
     * Returns the type of this job.
     * @return The job type.
     */
    public JobType getType() {
	return new JobType( 2, "DecompressFrameAction" );
    }

    /** Runs this job.
     * @return The decompressed frame.
     */
    public Frame run()
    {
	short r[] = new short[frame.r.length*REPEAT];
	short g[] = new short[frame.g.length*REPEAT];
	short b[] = new short[frame.b.length*REPEAT];
	int outix = 0;

        if( Settings.traceDecompressor ){
            System.out.println( "Decompressing frame " + frame.frameno );
        }
	for( int ix=0; ix<frame.r.length; ix++ ){
	    short v = frame.r[ix];
	    for( int i=0; i<REPEAT; i++ ) {
		r[outix++] = v;
	    }
	}
	for( int ix=0; ix<frame.g.length; ix++ ){
	    short v = frame.g[ix];
	    for( int i=0; i<REPEAT; i++ ) {
		g[outix++] = v;
	    }
	}
	for( int ix=0; ix<frame.b.length; ix++ ){
	    short v = frame.b[ix];
	    for( int i=0; i<REPEAT; i++ ) {
		b[outix++] = v;
	    }
	}
	return new Frame( frame.frameno, r, g, b );
    }
}
