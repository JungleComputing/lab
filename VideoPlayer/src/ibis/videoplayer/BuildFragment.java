/**
 * Builds a video fragment from the given range of frames.
 */
package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

/**
 * @author Kees van Reeuwijk
 *
 */
public final class BuildFragment implements Job {
    /** */
    private static final long serialVersionUID = 6769001575637882594L;
    private static final JobType jobType = new JobType( "BuildFragment" );
    private final int startFrame;
    private final int endFrame;

    BuildFragment( int startFrame, int endFrame )
    {
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    /**
     * Returns the type of this job.
     * @return The type of this job.
     */
    @Override
    public JobType getType()
    {
	return jobType;
    }

    @Override
    public void run( Node node, TaskIdentifier taskId )
    {
	JobWaiter waiter = new JobWaiter();
	for( int frame=startFrame; frame<=endFrame; frame++ ) {
	    Job j = new FetchFrame( frame );
	    waiter.submit( node, j );
	}
	waiter.sync();
    }

}
