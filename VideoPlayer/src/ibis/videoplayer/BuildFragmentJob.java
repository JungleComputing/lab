/**
 * Builds a video fragment from the given range of frames.
 */
package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.JobWaiter;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

/**
 * @author Kees van Reeuwijk
 *
 */
public final class BuildFragmentJob implements Job {
    /** */
    private static final long serialVersionUID = 6769001575637882594L;
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
	return buildJobType();
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
            System.out.println( "Collecting frames for fragment [" + startFrame + ".." + endFrame + "]" );
        }
        for( int frame=startFrame; frame<=endFrame; frame++ ) {
	    Job j = new FetchFrameJob( frame );
	    waiter.submit( node, j );
	}
	JobResultValue res[] = waiter.sync( node );
        int szr = 0;
        int szg = 0;
        int szb = 0;
        for( int i=0; i<res.length; i++ ){
            Frame frame = (Frame) res[i];
            szr += frame.r.length;
            szg += frame.g.length;
            szb += frame.b.length;
        }
        short r[] = new short[szr];
        short g[] = new short[szg];
        short b[] = new short[szb];
        int ixr = 0;
        int ixg = 0;
        int ixb = 0;
        for( int i=0; i<res.length; i++ ){
            Frame frame = (Frame) res[i];
            System.arraycopy( frame.r, 0, r, ixr, r.length );
            ixr += r.length;
            System.arraycopy( frame.g, 0, g, ixg, g.length );
            ixb += g.length;
            System.arraycopy( frame.b, 0, b, ixb, b.length );
            ixb += b.length;
        }
        if( Settings.traceFragmentBuilder ){
            System.out.println( "Building fragment [" + startFrame + "..." + endFrame + "]" );
        }
        JobResultValue value = new VideoFragment( startFrame, endFrame, r, g, b );
        if( Settings.traceFragmentBuilder ){
            System.out.println( "Sending fragment [" + startFrame + "..." + endFrame + "]" );
        }
        taskId.reportResult( node, value );
    }

    static JobType buildJobType()
    {
	return new JobType( 0, "BuildFragment" );
    }

}
