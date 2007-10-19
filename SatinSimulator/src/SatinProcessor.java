import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/**
 * @author Kees van Reeuwijk
 *
 */
public class SatinProcessor extends SimProcess
{
    private SatinSimulator model;
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
        super( model, "P"+procno, true );
        this.model = model;
        this.workers = workers;
        this.procno = procno;
        workQueue = new ProcessQueue( model, "P" + procno + " job queue", true, true );
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
                passivate();
            }
            else {
                int victim = model.getStealVictim( procno );

                workers[victim].requestWork( procno );
                passivate();
            }
        }
    }

    /** Request a job from this processor.
     * @param requester The processor to request work from.
     */
    private void requestWork( int requester )
    {
        SatinProcessor p = workers[requester];

        SatinJob job = (SatinJob) workQueue.first();
        if( job != null ) {
            workQueue.remove( job );
            // FIXME: model transmission time.
            job.setProcessor( p );
            p.queueJob( job );
            System.out.println( "P" + procno + ": P" + requester + " just stole job " + job );
            p.activate( new SimTime( 0.0 ) );
        }
        else {
            p.activate( new SimTime( 0.1 ) );
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