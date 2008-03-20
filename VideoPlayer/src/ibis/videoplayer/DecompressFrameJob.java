package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

/**
 * A job to decompress a frame. We fake decompressing a video frame
 * by simply doubling the frame and repeating the content.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class DecompressFrameJob implements Job {
    private static final long serialVersionUID = -3938044583266505212L;
    
    /** How many times should I repeat the fake decompression loop to approximate
     * the real decompression process.
     */
    private static final int REPEAT = 4;

    /** The frame to decomress. */
    private final Frame frame;
    static final JobType jobType = new JobType( "DecompressFrame" );

    DecompressFrameJob( Frame frame )
    {
        this.frame = frame;
    }

    /**
     * Returns the type of this job.
     * @return The job type.
     */
    @Override
    public JobType getType() {
        return jobType;
    }

    /** Runs this job. */
    @Override
    public void run( Node node, TaskIdentifier taskid )
    {
        int array[] = new int[frame.array.length*2];

        for( int n=0; n<REPEAT; n++ ){
            int outix = 0;

            for( int ix=0; ix<frame.array.length; ix++ ){
                int v = frame.array[ix];
                array[outix++] = v;
                array[outix++] = v;
            }
        }
        JobResultValue value = new Frame( frame.frameno, array );
        if( Settings.traceFetcher ){
            System.out.println( "Decompressing frame " + frame.frameno );
        }
        taskid.reportResult( node, value );
    }
}
