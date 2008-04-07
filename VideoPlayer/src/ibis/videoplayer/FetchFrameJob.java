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
    private static final Random rng = new Random();

    FetchFrameJob( int frameno )
    {
        this.frameno = frameno;
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
    public JobType getType() {
        return buildJobType();
    }

    /** Runs this job. */
    @Override
    public void run( Node node, TaskIdentifier taskid )
    {
        short r[] = new short[Settings.FRAME_SAMPLE_COUNT];
        short g[] = new short[Settings.FRAME_SAMPLE_COUNT];
        short b[] = new short[Settings.FRAME_SAMPLE_COUNT];
        for( int i=0; i<r.length; i++ ){
            r[i] = (short) (rng.nextInt() & 0xFFFF);
            g[i] = (short) (rng.nextInt() & 0xFFFF);
            b[i] = (short) (rng.nextInt() & 0xFFFF);
        }
        JobResultValue value = new Frame( frameno, r, g, b );
        if( Settings.traceFetcher ){
            System.out.println( "Building frame " + frameno );
        }
        taskid.reportResult( node, value );
    }
}
