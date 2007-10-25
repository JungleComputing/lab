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
    private SatinProcessor processors[];
    private ProcessQueue workQueue;
    private final double slowdown;
    private double idleTime;
    private final int procno;

    /** constructs a process... 
     * @param model The model the process belongs to.
     * @param processors The processors that participate in the computation.
     * @param procno The processor number.
     * @param slowdown The computation time multiplier of this processor.
     */
    public SatinProcessor( SatinSimulator model, SatinProcessor processors[], int procno, double slowdown )
    {
        super( model, "P"+procno, true );
        this.model = model;
        this.processors = processors;
        this.procno = procno;
        this.slowdown = slowdown;
        workQueue = new ProcessQueue( model, "job queue P" + procno, true, true );
        idleTime = 0.0;
    }

    /** describes this process's life cycle */
    @Override
    public void lifeCycle()
    {
        while( true ){
            SatinJob job = (SatinJob) workQueue.last();
            if( job != null ){
                workQueue.remove( job );
                sendTraceNote( "Executing job " + job );
                job.activateAfter( this );
                passivate();
            }
            else {
                int victim = model.getStealVictim( procno );
                if( victim<0 ) {
                    break;
                }
                stealWork( processors[victim] );
                passivate();
            }
        }
    }

    private void stealWork( SatinProcessor victim )
    {
	SatinJob job = (SatinJob) victim.workQueue.first();
	if( job == null ) {
	    double sleepTime = 0.1;
	    
	    idleTime += sleepTime;
	    activate( new SimTime( sleepTime ) );
	}
	else {
	    job.setProcessor( this );
	    queueJob( job );
            sendTraceNote(  "steals a job " + job + " from P" + victim.procno );
            double stealTime = model.getStealTime();
            activate( new SimTime( 0.0 ) );
	}
    }

    /** Put the given job on the work queue.
     * @param job The job to queue.
     */
    public void queueJob( SatinJob job )
    {
        workQueue.insert( job );
    }

    /**
     * Returns the idle time of this processor.
     * @return The idle time.
     */
    public double getIdleTime()
    {
	return idleTime;
    }

    /** Returns the slowdown of this processor. The slowdown is
     * the multiplication factor for execution times on this processor.
     * Slow processors, compared to the reference, have a factor
     * above 1, fast processors have a slowdown below 1.
     * 
     * @return The slowdown of this processor.
     */
    public double getSlowdown()
    {
	return slowdown;
    }
}