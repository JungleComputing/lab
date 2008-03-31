package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

/**
 * A job to fetch and scale a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class ScaleFrameJob implements Job {
    private static final long serialVersionUID = -3938044583266505212L;
    /** How often do we repeat the loop to fake a more elaborate scaler. */
    private static final int REPEAT = 4;

    /** The frame to fetch and scale. */
    private final Frame frame;

    ScaleFrameJob( Frame frame )
    {
	this.frame = frame;
    }

    static JobType buildJobType()
    {
	return new JobType( 3, "FetchFrameJob" );
    }

    /**
     * Returns the type of this job.
     * @return The job type.
     */
    @Override
    public JobType getType() {
	return buildJobType();
    }

    /** Runs this job. */
    @Override
    public void run( Node node, TaskIdentifier taskid )
    {
        if( Settings.traceScaler ){
            System.out.println( "Scaling frame " + frame.frameno );
        }
        // FIXME: implement this properly.
        int outarray[] = new int[Settings.FRAME_SAMPLE_COUNT/4];
        for( int n=0; n<REPEAT; n++ ){
            int ix = 0;
            int array[] = frame.array;

            for( int i=0; i<array.length; i += 4 ){
                int v = (array[i] + array[i+1] + array[i+2] + array[i+3])/2;
                outarray[ix++] = v;
            }
        }
        JobResultValue value = new Frame( frame.frameno, outarray );
        if( Settings.traceFetcher ){
            System.out.println( "Scaling frame " + frame.frameno );
        }
        taskid.reportResult( node, value );
    }
}
