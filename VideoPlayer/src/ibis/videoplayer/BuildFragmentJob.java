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

    static class DecompressFrameJob implements Job {
        private static final long serialVersionUID = -3938044583266505212L;

        DecompressFrameAction action;

        DecompressFrameJob( Frame frame )
        {
            this.action = new DecompressFrameAction( frame );
        }

        static JobType buildJobType()
        {
            return new JobType( 2, "DecompressFrameJob" );
        }

        /**
         * Returns the type of this job.
         * @return The job type.
         */
        @Override
        public JobType getType()
        {
            return buildJobType();
        }

        /** Runs this job. */
        @Override
        public void run( Node node, TaskIdentifier taskid )
        {
            Frame frame = action.run();
	    node.submit( new ColorCorrectFrameJob( frame ), taskid );
        }
    }


    static class ColorCorrectFrameJob implements Job {
        private static final long serialVersionUID = -3938044583266505212L;

        ColorCorrectAction action;

        ColorCorrectFrameJob( Frame frame )
        {
            this.action = new ColorCorrectAction( frame );
        }

        /**
         * Returns the type of this job.
         * @return The job type.
         */
        @Override
        public JobType getType()
        {
            return buildJobType();
        }

        static JobType buildJobType()
        {
            return new JobType( 3, "ColorCorrectFrameJob" );
        }

        /** Runs this job. */
        @Override
        public void run( Node node, TaskIdentifier taskid )
        {
            Frame frame = action.run();
	    node.submit( new ScaleFrameJob( frame ), taskid );
        }
    }

    static class ScaleFrameJob implements Job {
        private static final long serialVersionUID = -3938044583266505212L;

        ScaleFrameAction action;

        ScaleFrameJob( Frame frame )
        {
            this.action = new ScaleFrameAction( frame );
        }

        static JobType buildJobType()
        {
            return new JobType( 4, "ScaleFrameJob" );
        }

        /**
         * Returns the type of this job.
         * @return The job type.
         */
        @Override
        public JobType getType()
        {
            return buildJobType();
        }

        /** Runs this job. */
        @Override
        public void run( Node node, TaskIdentifier taskid )
        {
            Frame frame = action.run();
            taskid.reportResult( node, frame );
        }
    }

    static class FetchFrameJob implements Job {
        private static final long serialVersionUID = -3938044583266505212L;

        FetchFrameAction action;

        FetchFrameJob( int frameno )
        {
            this.action = new FetchFrameAction( frameno );
        }

        static JobType buildJobType()
        {
            return new JobType( 1, "FetchFrameJob" );
        }

        /**
         * Returns the type of this job.
         * @return The job type.
         */
        @Override
        public JobType getType()
        {
            return buildJobType();
        }

        /** Runs this job. */
        @Override
        public void run( Node node, TaskIdentifier taskid )
        {
            if( Settings.traceFetcher ){
                System.out.println( "Building frame " + action );
            }
            Frame frame = action.run();
	    node.submit( new DecompressFrameJob( frame ), taskid );
        }
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
        if( Settings.traceFragmentBuilder ){
            System.out.println( "Building fragment [" + startFrame + "..." + endFrame + "]" );
        }
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
            System.arraycopy( frame.r, 0, r, ixr, frame.r.length );
            ixr += frame.r.length;
            System.arraycopy( frame.g, 0, g, ixg, frame.g.length );
            ixg += frame.g.length;
            System.arraycopy( frame.b, 0, b, ixb, frame.b.length );
            ixb += frame.b.length;
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
