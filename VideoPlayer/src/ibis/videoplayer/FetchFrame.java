package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.ReportResultJob;
import ibis.maestro.TaskIdentifier;

import java.util.Arrays;

/**
 * A job to fetch a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class FetchFrame implements Job {
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch. */
    private final int frameno;
    private static final JobType jobType = new JobType( "FetchFrame" );

    FetchFrame( int frameno )
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
    
    static class FrameIdentifier implements TaskIdentifier {
        private static final long serialVersionUID = -4017660605155497064L;
        final int frameno;
        
        FrameIdentifier( int frameno )
        {
            this.frameno = frameno;
        }
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
    public void run(Node context, TaskIdentifier taskid )
    {
        int filler = frameno;

        int array[] = new int[Settings.FRAME_SAMPLE_COUNT];
        Arrays.fill( array, filler );
        JobResultValue value = new Frame( array );
        Job j = new ReportResultJob( new FrameIdentifier( frameno ), value );
        context.submit( j, taskid );
    }
}
