package ibis.videoplayer;

import java.util.Arrays;

import ibis.maestro.Job;
import ibis.maestro.JobContext;
import ibis.maestro.JobType;

/**
 * A job to fetch a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class FrameFetcher implements Job {
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch. */
    private final int frameno;
    private static final JobType jobType = new JobType( "FrameFetcher" );

    FrameFetcher( int frameno )
    {
	this.frameno = frameno;
    }

    @Override
    public JobType getType() {
	// TODO: Auto-generated method stub
	return jobType;
    }

    @Override
    public void run(JobContext context)
    {
	int filler = frameno;
	
	int array[] = new int[Settings.FRAME_SAMPLE_COUNT];
	Arrays.fill( array, filler );
    }
}
