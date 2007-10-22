import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/** A job. */
public class SatinJob extends SimProcess
{
    private SatinProcessor processor;
    private final SatinSimulator model;
    private final int depth;
    private static final boolean TRACE_JOBS = true;
    private SatinJob parent;
    private int children;

    /**
     * Constructs a new SatinJob.
     * @param model The model this job belongs to.
     * @param name The name of the job
     * @param trace Trace execution of this job?
     * @param processor The processor the job belongs to.
     * @param depth The recursion depth of the satin jobs.
     * @param parent Who is the parent of this job?
     */
    public SatinJob( SatinSimulator model, String name, boolean trace, SatinProcessor processor, int depth, SatinJob parent )
    {
	super( model, name, trace );
	this.processor = processor;
	this.model = model;
	this.depth = depth;
	this.parent = parent;
    }

    /**
     * The execution cycle of a Satin job. (Overrides method in superclass.)
     *
     */
    @Override
    public void lifeCycle()
    {
	hold( new SimTime( model.getPreSpawnExecutionTime() ) );
	if( depth>0 ){
	    SatinJob joba = new SatinJob( model, "job" + depth + "a", TRACE_JOBS, processor, depth-1, this );
	    processor.queueJob( joba );
	    SatinJob jobb = new SatinJob( model, "job" + depth + "b", TRACE_JOBS, processor, depth-1, this );
	    processor.queueJob( jobb );
	    children = 2;
	}
	hold( new SimTime( model.getPostSpawnExecutionTime() ) );
	processor.activateAfter( this );
	if( children>0 ) {
	    processor.enterWaitingList( this );
	    passivate();
	    processor.leaveWaitingList( this );
	}
	if( parent == null ){
	    sendTraceNote( "Root job has finished" );
	    model.rootHasFinished();
	}
	else {
	    ComputationEndEvent e = new ComputationEndEvent( model, "endEvent", true );
	    e.scheduleAfter( parent, processor );
	}
    }

    /** Assign this job to the given processor.
     * @param p The new owner of this job.
     */
    public void setProcessor( SatinProcessor p )
    {
	processor = p;
    }

    /** Register the fact that one of the children has finished. */
    public void decrementActiveChildren()
    {
	children--;
	if( processor.isInWaitingList( this ) && children<=0 ){
	    this.activate( new SimTime( 0.0 ) );
	}
    }
}
