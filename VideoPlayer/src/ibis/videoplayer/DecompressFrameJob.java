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

    DecompressFrameAction action;

    DecompressFrameJob( Frame frame )
    {
        this.action = new DecompressFrameAction( frame );
    }

    /**
     * Returns the type of this job.
     * @return The job type.
     */
    @Override
    public JobType getType()
    {
	return action.getType();
    }

    /** Runs this job. */
    @Override
    public void run( Node node, TaskIdentifier taskid )
    {
        JobResultValue value = action.run();
        taskid.reportResult( node, value );
    }
}
