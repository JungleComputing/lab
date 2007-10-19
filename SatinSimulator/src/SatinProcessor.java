import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/**
 * @author Kees van Reeuwijk
 *
 */
public class SatinProcessor extends SimProcess
{
    private SatinProcessor workers[];
    private ProcessQueue workQueue;
    private final int procno;

    /** constructs a process... 
     * @param model The model the process belongs to.
     * @param workers The processors that participate in the computation.
     * @param procno The processor number.
     */
    public SatinProcessor( SatinSimulator model, SatinProcessor workers[], int procno )
    {
        super( model, "SatinProcess", true );
        this.workers = workers;
        this.procno = procno;
        workQueue = new ProcessQueue( model, "P" + procno + " Process queue", true, true );
    }

    /** describes this process's life cycle */
    @Override
    public void lifeCycle()
    {
        while( true ){
            SatinJob job = (SatinJob) workQueue.last();
            if( job != null ){
                workQueue.remove( job );
                System.out.println( "P" + procno+ ": Executing job " + job );
                job.activateAfter( this );
            }
            else {
                System.out.println( "P" + procno + ": Idle. TODO: try to steal work" );
                hold( new SimTime( 1.0 ) );
            }
        }
    }

    /** Put the given job on the work queue.
     * @param job The job to queue.
     */
    public void queueJob( SatinJob job )
    {
        workQueue.insert( job );
    }
}