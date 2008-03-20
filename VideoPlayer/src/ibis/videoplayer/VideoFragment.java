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
    int array[];

    public VideoFragment( int startFrame, int endFrame, int[] array )
    {
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.array = array;
    }    
}
