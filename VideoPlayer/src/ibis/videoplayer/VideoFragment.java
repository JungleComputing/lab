package ibis.videoplayer;

import ibis.maestro.JobResultValue;

/** A video fragment.
 * 
 * @author reeuwijk
 *
 */
public class VideoFragment implements JobResultValue 
{
    private static final long serialVersionUID = -791160275253169225L;
    int startFrame;
    int endFrame;
    short r[];
    short g[];
    short b[];

    /**
     * Constructs a new video fragment.
     * @param startFrame The start frame of the fragment.
     * @param endFrame The end frame of the fragment.
     * @param r Red components
     * @param g Green components
     * @param b Blue components
     */
    public VideoFragment( int startFrame, int endFrame, short r[], short g[], short b[] )
    {
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.r = r;
        this.g = g;
        this.b = b;
    }

}
