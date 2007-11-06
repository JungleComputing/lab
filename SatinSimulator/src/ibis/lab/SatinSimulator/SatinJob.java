package ibis.lab.SatinSimulator;

import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/** A job. */
public class SatinJob extends SimProcess
{
    private static final boolean TRACE_JOBS = true;
    private static int spawns = 0;
    private static double appTime = 0.0;
    private final int childNo;
    private boolean[] childrenReady;
    private final int depth;
    private final SatinSimulator model;
    private final SatinJob parent;
    private static final int JOB_CONTEXT_SIZE = 1000;
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
        spawns++;
    }

    boolean childrenAreReady()
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
        double preSpawnExecutionTime = model.getPreSpawnExecutionTime( processor.getSlowdown() );
        hold( new SimTime( preSpawnExecutionTime ) );
        appTime += preSpawnExecutionTime;
        final double preSyncExecutionTime = model.getPreSyncExecutionTime( processor.getSlowdown() );
        if( depth>0 ){
            final SatinJob joba = new SatinJob( model, "job" + depth + "a", TRACE_JOBS, processor, depth-1, this, 0 );
            processor.queueJob( joba );
            final SatinJob jobb = new SatinJob( model, "job" + depth + "b", TRACE_JOBS, processor, depth-1, this, 1 );
            processor.queueJob( jobb );
            childrenReady = new boolean[2];
            hold( new SimTime( preSyncExecutionTime ) );
            processor.sync( this );
        }
        else {
            hold( new SimTime( preSyncExecutionTime ) );            
        }
        appTime += preSyncExecutionTime;
        final double postSyncExecutionTime = model.getPostSyncExecutionTime( processor.getSlowdown() );
        hold( new SimTime( postSyncExecutionTime ) );
        appTime += postSyncExecutionTime;
        if( parent == null ){
            sendTraceNote( "Root job has finished" );
            model.rootHasFinished();
        }
        else {
            parent.registerChildCompleted( childNo );
        }
        processor.activateAfter( this );
    }

    /** Tell the job that the child with the given number is ready.
     *
     * @param no The child that is ready.
     */
    public void registerChildCompleted( final int no )
    {
        childrenReady[no] = true;
    }

    /** Assign this job to the given processor.
     * @param p The new owner of this job.
     */
    public void setProcessor( final SatinProcessor p )
    {
        processor = p;
    }

    /** Returns the number of spawns that have been done.
     * @return The number of spawns.
     */
    public static int getSpawns()
    {
        return spawns;
    }
    
    /** Returns the total time spent on the application.
     * (As opposed to overhead or idlin.
     * @return The total application time.
     */
    public static double getAppTime()
    {
        return appTime;
    }

    /** Returns the size of the context of this job.
     * @return The number of bytes in the context of this job.
     */
    public int getContextSize()
    {
        return JOB_CONTEXT_SIZE;
    }
}
