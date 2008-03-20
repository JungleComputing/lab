package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

import java.util.Arrays;

/**
 * A job to fetch a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class DecompressFrameJob implements Job {
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch. */
    private final int frameno;
    static final JobType jobType = new JobType( "FetchFrame" );

    DecompressFrameJob( int frameno )
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

    /** Runs this job. */
    @Override
    public void run( Node node, TaskIdentifier taskid )
    {
        int filler = frameno;

        int array[] = new int[Settings.FRAME_SAMPLE_COUNT];
        Arrays.fill( array, filler );
        JobResultValue value = new Frame( array );
        if( Settings.traceFetcher ){
            System.out.println( "Building frame " + frameno );
        }
        taskid.reportResult( node, value );
    }
}
