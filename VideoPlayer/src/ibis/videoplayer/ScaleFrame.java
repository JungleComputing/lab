package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.JobWaiter;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

import java.util.Arrays;

/**
 * A job to fetch a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class ScaleFrame implements Job {
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch. */
    private final int frameno;
    private static final JobType jobType = new JobType( "FetchFrame" );

    ScaleFrame( int frameno )
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
	Job j = new FetchFrame( frameno );
	waiter.submit( node, j );
	waiter.sync( node );
    }
}
