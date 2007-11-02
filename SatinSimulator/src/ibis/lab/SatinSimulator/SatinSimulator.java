package ibis.lab.SatinSimulator;

import desmoj.core.dist.IntDistUniform;
import desmoj.core.dist.RealDistUniform;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimTime;

/**
 * @author Kees van Reeuwijk
 *
 */
public class SatinSimulator extends Model
{
    private static final double MIN_PRE_SPAWN_TIME = 50e-3;
    private static final double MAX_PRE_SPAWN_TIME = 50e-3;
    private static final double MIN_PRE_SYNC_TIME = 50e-3;
    private static final double MAX_PRE_SYNC_TIME = 50e-3;
    private static final double MAX_POST_SYNC_TIME = 50e-3;
    private static final double MIN_POST_SYNC_TIME = 50e-3;
    static final int NUMBER_PROCESSORS = 4;
    static final boolean showInReport = true;
    static final boolean showInTrace = true;
    private static final int START_LEVEL = 11;
    private static final double STEAL_TIME = 835e-6;
    private static final double MAX_EXEC_TIME = MAX_PRE_SPAWN_TIME + MAX_PRE_SYNC_TIME + MAX_POST_SYNC_TIME + STEAL_TIME;
    private static final double SIMULATION_TIME = (1.5*MAX_EXEC_TIME * (2<<START_LEVEL))/Math.sqrt( NUMBER_PROCESSORS );

    /** runs the model.
     * @param args Command-line parameters.
     */
    public static void main( final String[] args )
    {
        // create model and experiment
        final SatinSimulator model = new SatinSimulator();
        final Experiment exp = new Experiment( "Experiment" );
        // and connect them
        model.connectToExperiment( exp );

        // set experiment parameters
        exp.setShowProgressBar( true );
        final SimTime stopTime = new SimTime( SIMULATION_TIME );
        System.out.println( "Simulation time: " + stopTime );
        exp.tracePeriod( new SimTime(0.0), stopTime );
        exp.stop( stopTime );

        // start experiment
        exp.start();

        // generate report and shut everything off
        exp.report();
        {
            double totalIdle = 0.0;
            double maxIdle = 0;
            int maxIdleProcessor = -1;

            for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
                final double idleTime = model.processors[i].getIdleTime();
                System.out.println( "P" + i + ": idle time: " + idleTime );
                totalIdle += idleTime;
                if( idleTime>maxIdle ){
                    maxIdle = idleTime;
                    maxIdleProcessor = i;
                }
            }
            final double t = model.getFinishTime();
            System.out.println( SatinJob.getSpawns() + " spawns" );
            System.out.println( "SATIN: USEFUL_APP_TIME:    agv. per machine " + SatinJob.getAppTime()/NUMBER_PROCESSORS );
            System.out.println( "Average idle fraction: " + (totalIdle/(t*NUMBER_PROCESSORS)) );
            System.out.println( "Maximal idle fraction: " + (maxIdle/t) + " (P" + maxIdleProcessor + ")" );
        }
        System.out.println( "Finish time: " + model.getFinishTime() );

        exp.finish();
    }

    private boolean finished = false;
    private SimTime finishTime;
    private RealDistUniform postSyncExecutionTime;
    private RealDistUniform preSpawnExecutionTime;
    private RealDistUniform preSyncExecutionTime;
    private final SatinProcessor processors[] = new SatinProcessor[NUMBER_PROCESSORS];

    // define model components here

    private IntDistUniform stealVictim;

    /** constructs a model... */
    public SatinSimulator()
    {
        super( null, "SatinSimulator", showInReport, showInTrace );
    }

    /** returns a description of this model to be used in the report.
     * @return The description.
     */
    @Override
    public String description()
    {
        return "The Satin workstealing environment";
    }

    /** activate dynamic components */
    @Override
    public void doInitialSchedules()
    {
        for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
            double slowdown = 1.0;

            if( (i % 2)  == 1 ) {
                slowdown = 1.0;
            }
            final SatinProcessor p = new SatinProcessor( this, processors, i, slowdown );
            processors[i] = p;
        }
        final SatinJob rootjob = new SatinJob( this, "rootjob", true, processors[0], START_LEVEL, null, 0 );
        processors[0].queueJob( rootjob );
        for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
            processors[i].activate( new SimTime( 0.0 ) );
        }
    }

    /** Return the time the computation finished.
     * @return The finish time.
     */
    public double getFinishTime()
    {
        if( finishTime == null ){
            return 0.0;
        }
        return finishTime.getTimeValue();
    }

    /** Return the execution time of a process.
     * @param slowdown The slowdown for the executing processor.
     * @return The execution time.
     */
    public double getPostSyncExecutionTime( final double slowdown )
    {
        return slowdown*postSyncExecutionTime.sample();
    }

    /** Return the execution time of a process.
     * @param slowdown The slowdown for the executing processor.
     * @return The execution time.
     */
    public double getPreSpawnExecutionTime( final double slowdown )
    {
        return slowdown*preSpawnExecutionTime.sample();
    }

    /** Return the execution time of a process.
     * @param slowdown The slowdown for the executing processor.
     * @return The execution time.
     */
    public double getPreSyncExecutionTime( final double slowdown )
    {
        return slowdown*preSyncExecutionTime.sample();
    }

    /** Given two processors, return the time it takes to steal
     * a job from one to the other.
     * @param thief The process that steals the job.
     * @param victim The process from whom the job is stolen.
     * @return The time in seconds to steal the job.
     */
    public double getStealTime(final SatinProcessor thief, final SatinProcessor victim)
    {
        final double slowdown = Math.max( thief.getSlowdown(), victim.getSlowdown() );
        return slowdown*STEAL_TIME;
    }

    /** Given our own processor number, return the victim to
     * steal work from.
     * @param procno Our own processor number.
     * @return The processor to steal from.
     */
    public int getStealVictim( final int procno )
    {
        if( finished ) {
            return -1;
        }
        int res = (int) stealVictim.sample();
        if( res>=procno ) {
            res++;
        }
        return res;
    }

    /** initialise static components */
    @Override
    public void init()
    {
        stealVictim = new IntDistUniform( this, "StealVictimStream", 0, NUMBER_PROCESSORS-2, true, false );
        preSpawnExecutionTime = new RealDistUniform( this, "PreSpawnExecutionTimeStream", MIN_PRE_SPAWN_TIME, MAX_PRE_SPAWN_TIME, true, false );
        preSyncExecutionTime = new RealDistUniform( this, "PostSpawnExecutionTimeStream", MIN_PRE_SYNC_TIME, MAX_PRE_SYNC_TIME, true, false );
        postSyncExecutionTime = new RealDistUniform( this, "PostSyncExecutionTimeStream", MIN_POST_SYNC_TIME, MAX_POST_SYNC_TIME, true, false );
    }

    // define any additional methods if necessary,
    // e.g. access methods to model components

    /** Signal that the root process has finished. */
    public void rootHasFinished()
    {
        System.out.println( "Root process has finished" );
        finished = true;
        finishTime = currentTime();
    }
}
