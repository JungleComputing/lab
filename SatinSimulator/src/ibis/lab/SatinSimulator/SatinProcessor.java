package ibis.lab.SatinSimulator;

import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;

/**
 * @author Kees van Reeuwijk
 *
 * A Satin processor.
 */
public class SatinProcessor extends SimProcess
{
    private double idleTime;
    private final SatinSimulator model;
    private final SatinProcessor processors[];
    private final int procno;
    private final double slowdown;
    private final ProcessQueue workQueue;

    /** constructs a process...
     * @param model The model the process belongs to.
     * @param processors The processors that participate in the computation.
     * @param procno The processor number.
     * @param slowdown The computation time multiplier of this processor.
     */
    public SatinProcessor( final SatinSimulator model, final SatinProcessor processors[], final int procno, final double slowdown )
    {
        super( model, "P"+procno, true );
        this.model = model;
        this.processors = processors;
        this.procno = procno;
        this.slowdown = slowdown;
        workQueue = new ProcessQueue( model, "job queue P" + procno, true, true );
        idleTime = 0.0;
    }

    /**
     * Returns the cumulative idle time of this processor.
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

    /** describes this process's life cycle */
    @Override
    public void lifeCycle()
    {
        while( true ){
            final SatinJob job = (SatinJob) workQueue.last();
            if( job != null ){
                workQueue.remove( job );
                sendTraceNote( "Executing job " + job );
                job.activateAfter( this );
                passivate();
            }
            else {
                final int victim = model.getStealVictim( procno );
                if( victim<0 ) {
                    break;
                }
                stealWork( processors[victim] );
            }
        }
    }

    /** Put the given job on the work queue.
     * @param job The job to queue.
     */
    public void queueJob( final SatinJob job )
    {
        workQueue.insert( job );
    }

    private void stealWork( final SatinProcessor victim )
    {
        final SatinJob job = (SatinJob) victim.workQueue.first();
        if( job == null ) {
            final double sleepTime = 0.1;

            idleTime += sleepTime;
            activate( new SimTime( sleepTime ) );
            passivate();
        }
        else {
            victim.workQueue.remove( job );
            job.setProcessor( this );
            sendTraceNote(  "steals a job " + job + " from P" + victim.procno );
            final double stealTime = model.getStealTime( this, victim );
            activate( new SimTime( stealTime ) );
            passivate();
            queueJob( job );
        }
    }
}