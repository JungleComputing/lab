package ibis.lab.SatinSimulator;

import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/** A job. */
public class SatinJob extends SimProcess
{
    private static final boolean TRACE_JOBS = true;
    private final int childNo;
    private boolean[] childrenReady;
    private final int depth;
    private final SatinSimulator model;
    private final SatinJob parent;
    private SatinProcessor processor;

    /**
     * Constructs a new SatinJob.
     * @param model The model this job belongs to.
     * @param name The name of the job
     * @param trace Trace execution of this job?
     * @param processor The processor the job belongs to.
     * @param depth The recursion depth of the satin jobs.
     * @param parent Who is the parent of this job?
     * @param childNo What is the child number of this job?
     */
    public SatinJob( final SatinSimulator model, final String name, final boolean trace, final SatinProcessor processor, final int depth, final SatinJob parent, final int childNo )
    {
        super( model, name, trace );
        this.processor = processor;
        this.model = model;
        this.depth = depth;
        this.parent = parent;
        this.childNo = childNo;
    }

    private boolean childrenAreReady()
    {
        if( childrenReady == null ) {
            return true;
        }
        for( boolean ready: childrenReady ) {
            if( !ready ) {
                return false;
            }
        }
        return true;
    }
    /**
     * The execution cycle of a Satin job. (Overrides method in superclass.)
     */
    @Override
    public void lifeCycle()
    {
        hold( new SimTime( model.getPreSpawnExecutionTime( processor.getSlowdown() ) ) );
        if( depth>0 ){
            final SatinJob joba = new SatinJob( model, "job" + depth + "a", TRACE_JOBS, processor, depth-1, this, 0 );
            processor.queueJob( joba );
            final SatinJob jobb = new SatinJob( model, "job" + depth + "b", TRACE_JOBS, processor, depth-1, this, 1 );
            processor.queueJob( jobb );
            childrenReady = new boolean[2];
        }
        final double preSyncExecutionTime = model.getPreSyncExecutionTime( processor.getSlowdown() );
        hold( new SimTime( preSyncExecutionTime ) );
        processor.activateAfter( this );
        while( !childrenAreReady() ) {
            passivate();
        }
        final double postSyncExecutionTime = model.getPostSyncExecutionTime( processor.getSlowdown() );
        hold( new SimTime( postSyncExecutionTime ) );
        if( parent == null ){
            sendTraceNote( "Root job has finished" );
            model.rootHasFinished();
        }
        else {
            parent.registerChildCompleted( childNo );
        }
    }

    /** Tell the job that the child with the given number is ready.
     *
     * @param no The child that is ready.
     */
    public void registerChildCompleted( final int no )
    {
        childrenReady[no] = true;
        activate( new SimTime( 0.0 ) );
    }

    /** Assign this job to the given processor.
     * @param p The new owner of this job.
     */
    public void setProcessor( final SatinProcessor p )
    {
        processor = p;
    }

}
