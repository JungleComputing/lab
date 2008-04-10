package ibis.videoplayer;

import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

/**
 * A job to fetch and scale a frame.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class ScaleFrameJob implements Job {
    private static final long serialVersionUID = -3938044583266505212L;

    /** The frame to fetch and scale. */
    private final Frame frame;

    ScaleFrameJob( Frame frame )
    {
	this.frame = frame;
    }

    static JobType buildJobType()
    {
	return new JobType( 3, "ScaleFrameJob" );
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
	if( Settings.traceScaler ){
	    System.out.println( "Scaling frame " + frame.frameno );
	}
	short outr[] = new short[frame.r.length/4];
	{
	    int ix = 0;
	    short r[] = frame.r;

	    for( int i=0; i<r.length; i += 4 ){
		short v = (short) ((r[i] + r[i+1] + r[i+2] + r[i+3])/4);
		outr[ix++] = v;
	    }
	}
	short outg[] = new short[frame.g.length/4];
	{
	    int ix = 0;
	    short g[] = frame.g;

	    for( int i=0; i<g.length; i += 4 ){
		short v = (short) ((g[i] + g[i+1] + g[i+2] + g[i+3])/4);
		outg[ix++] = v;
	    }
	}
	short outb[] = new short[frame.b.length/4];
	{
	    int ix = 0;
	    short b[] = frame.b;

	    for( int i=0; i<b.length; i += 4 ){
		short v = (short) ((b[i] + b[i+1] + b[i+2] + b[i+3])/4);
		outb[ix++] = v;
	    }
	}
	JobResultValue value = new Frame( frame.frameno, frame.width/2, frame.height/2, outr, outg, outb );
	if( Settings.traceFetcher ){
	    System.out.println( "Scaling frame " + frame.frameno );
	}
	taskid.reportResult( node, value );
    }
}
