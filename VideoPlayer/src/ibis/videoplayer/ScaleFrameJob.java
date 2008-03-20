package ibis.videoplayer;

import java.util.Arrays;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.JobWaiter;
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

    /** The frame to fetch and scale. */
    private final int frameno;
    static final JobType jobType = new JobType( "ScaleFrame" );

    ScaleFrameJob( int frameno )
    {
	this.frameno = frameno;
    }

    /**
     * Returns the type of this job.
     * @return The job type.
     */
    @Override
    public JobType getType() {
	return jobType;
    }

    static class Frame implements JobResultValue {
	private static final long serialVersionUID = 8797700803728846092L;
	final int array[];

	Frame( int array[] ){
	    this.array = array;
	}
    }

    /** Runs this job. */
    @Override
    public void run( Node node, TaskIdentifier taskid )
    {
	JobWaiter waiter = new JobWaiter();
	Job j = new FetchFrameJob( frameno );
	waiter.submit( node, j );
	waiter.sync( node );
        if( Settings.traceScaler ){
            System.out.println( "Scaling frame " + frameno );
        }
        // FIXME: implement this properly.
        int array[] = new int[Settings.FRAME_SAMPLE_COUNT/4];
        Arrays.fill( array, 0 );
        JobResultValue value = new Frame( array );
        if( Settings.traceFetcher ){
            System.out.println( "Scaling frame " + frameno );
        }
        taskid.reportResult( node, value );
    }
}
