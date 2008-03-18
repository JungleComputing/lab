/**
 * Builds a video fragment from the given range of frames.
 */
package ibis.videoplayer;

import java.util.Arrays;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.JobWaiter;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;
import ibis.videoplayer.FetchFrame.Frame;

/**
 * @author Kees van Reeuwijk
 *
 */
public final class BuildFragmentJob implements Job {
    /** */
    private static final long serialVersionUID = 6769001575637882594L;
    static final JobType jobType = new JobType( "BuildFragment" );
    private final int startFrame;
    private final int endFrame;

    BuildFragmentJob( int startFrame, int endFrame )
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

    /**
     * Runs this fragment building job.
     * @param node The node this job is running on.
     * @param taskId The task identifier this job belongs to.
     */
    @Override
    public void run( Node node, TaskIdentifier taskId )
    {
	JobWaiter waiter = new JobWaiter();

        if( Settings.traceFragmentBuilder ){
            System.out.println( "Collecting frames for fragment [" + startFrame + "..." + endFrame + "]" );
        }
        for( int frame=startFrame; frame<=endFrame; frame++ ) {
	    Job j = new FetchFrame( frame );
	    waiter.submit( node, j );
	}
	JobResultValue res[] = waiter.sync( node );
        if( Settings.traceFragmentBuilder ){
            System.out.println( "Building fragment [" + startFrame + "..." + endFrame + "]" );
        }
        int array[] = new int[Settings.FRAME_SAMPLE_COUNT*Settings.FRAME_FRAGMENT_COUNT];
        Arrays.fill( array, 42 );
        JobResultValue value = new Frame( array );
        if( Settings.traceFragmentBuilder ){
            System.out.println( "Sending fragment [" + startFrame + "..." + endFrame + "]" );
        }
        taskId.reportResult( node, value );
    }

}
