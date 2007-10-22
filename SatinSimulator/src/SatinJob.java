import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/** A job. */
public class SatinJob extends SimProcess
{
    private SatinProcessor processor;
    private final SatinSimulator model;
    private final int depth;
    private static final boolean TRACE_JOBS = true;

    /**
     * Constructs a new SatinJob.
     * @param model The model this job belongs to.
     * @param name The name of the job
     * @param trace Trace execution of this job?
     * @param processor The processor the job belongs to.
     * @param depth The recursion depth of the satin jobs.
     */
    public SatinJob( SatinSimulator model, String name, boolean trace, SatinProcessor processor, int depth )
    {
        super( model, name, trace );
        this.processor = processor;
        this.model = model;
        this.depth = depth;
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
            SatinJob joba = new SatinJob( model, "job" + depth + "a", TRACE_JOBS, processor, depth-1 );
            processor.queueJob( joba );
            SatinJob jobb = new SatinJob( model, "job" + depth + "b", TRACE_JOBS, processor, depth-1 );
            processor.queueJob( jobb );
        }
        hold( new SimTime( model.getPostSpawnExecutionTime() ) );
        processor.activateAfter( this );
    }

    /** Assign this job to the given processor.
     * @param p The new owner of this job.
     */
    public void setProcessor( SatinProcessor p )
    {
        processor = p;
    }

}
