package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

import java.util.Random;

/**
 * A job to fetch a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class FetchFrameJob implements Job {
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch. */
    private final int frameno;
    static final JobType jobType = new JobType( "FetchFrame" );
    private static final Random rng = new Random();

    FetchFrameJob( int frameno )
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
        int array[] = new int[Settings.FRAME_SAMPLE_COUNT];
        for( int i=0; i<array.length; i++ ){
            array[i] = rng.nextInt();
        }
        JobResultValue value = new Frame( frameno, array );
        if( Settings.traceFetcher ){
            System.out.println( "Building frame " + frameno );
        }
        taskid.reportResult( node, value );
    }
}
