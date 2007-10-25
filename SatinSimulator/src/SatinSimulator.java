
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
    static final boolean showInReport = true;
    static final boolean showInTrace = true;
    static final int NUMBER_PROCESSORS = 16;
    private IntDistUniform stealVictim;
    private RealDistUniform preSpawnExecutionTime;
    private RealDistUniform postSpawnExecutionTime;
    private SatinProcessor processors[] = new SatinProcessor[NUMBER_PROCESSORS];
    private static final int START_LEVEL = 8;
    private static final double STEAL_TIME = 10.0;
    private static final double MAX_EXEC_TIME = 1+100 + STEAL_TIME;
    private static final double SIMULATION_TIME = (1.5*MAX_EXEC_TIME * (1<<START_LEVEL))/Math.sqrt( NUMBER_PROCESSORS );
    private boolean finished = false;
    private SimTime finishTime;

    // define model components here

    /** constructs a model... */
    public SatinSimulator()
    {
        super( null, "SatinSimulator", showInReport, showInTrace );
    }

    /** initialise static components */
    @Override
    public void init()
    {
        stealVictim = new IntDistUniform( this, "StealVictimStream", 0, NUMBER_PROCESSORS-2, true, false );
        preSpawnExecutionTime = new RealDistUniform( this, "PreSpawnExecutionTimeStream", 0.1, 1, true, false );
        postSpawnExecutionTime = new RealDistUniform( this, "PostSpawnExecutionTimeStream", 10, 100, true, false );
    }

    /** activate dynamic components */
    @Override
    public void doInitialSchedules()
    {
        for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
            double slowdown = 1.0;

            if( (i % 2)  == 0 ) {
        	slowdown = 4.0;
            }
            SatinProcessor p = new SatinProcessor( this, processors, i, slowdown );
            processors[i] = p;
        }
        SatinJob rootjob = new SatinJob( this, "rootjob", true, processors[0], START_LEVEL, null, 0 );
        processors[0].queueJob( rootjob );
        for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
            processors[i].activate( new SimTime( 0.0 ) );
        }
    }

    /** returns a description of this model to be used in the report.
     * @return The description.
     */
    @Override
    public String description()
    {
    	return "The Satin workstealing environment";
    }

    // define any additional methods if necessary,
    // e.g. access methods to model components

    /** Given our own processor number, return the victim to
     * steal work from.
     * @param procno Our own processor number.
     * @return The processor to steal from.
     */
    public int getStealVictim( int procno )
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
    
    /** Returns the time that a successful steal request takes.
     * @return The steal time.
     */
    public double getStealTime()
    {
	return STEAL_TIME;
    }

    /** Return the execution time of a process.
     * @param slowdown The slowdown for the executing processor.
     * @return The execution time.
     */
    public double getPreSpawnExecutionTime( double slowdown )
    {
        return slowdown*preSpawnExecutionTime.sample();
    }

    /** Return the execution time of a process.
     * @param slowdown The slowdown for the executing processor.
     * @return The execution time.
     */
    public double getPostSpawnExecutionTime( double slowdown )
    {
        return slowdown*postSpawnExecutionTime.sample();
    }

    /** Signal that the root process has finished. */
    public void rootHasFinished()
    {
	System.out.println( "Root process has finished" );
	finished = true;
	finishTime = this.currentTime();
    }

    /** Return the time the computation finished.
     * @return The finish time.
     */
    public double getFinishTime()
    {
	return finishTime.getTimeValue();
    }

    // define any additional methods if necessary,
    // e.g. access methods to model components
    
    /** runs the model.
     * @param args Command-line parameters.
     */
    public static void main( String[] args )
    {
        // create model and experiment
        SatinSimulator model = new SatinSimulator();
        Experiment exp = new Experiment( "Experiment" );
        // and connect them
        model.connectToExperiment( exp );
    
        // set experiment parameters
        exp.setShowProgressBar( true );
        SimTime stopTime = new SimTime( SIMULATION_TIME );
        exp.tracePeriod( new SimTime(0.0), stopTime );
        exp.stop( stopTime );
    
        // start experiment
        exp.start();
    
        // generate report and shut everything off
        exp.report();
        {
            double totalIdle = 0.0;

            for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
        	double idleTime = model.processors[i].getIdleTime();
		System.out.println( "P" + i + ": idle time: " + idleTime );
		totalIdle += idleTime;
            }
            double t = model.getFinishTime();
            System.out.println( "Idle fraction: " + totalIdle/(t*NUMBER_PROCESSORS) );
        }
        System.out.println( "Finish time: " + model.getFinishTime() );
    
        exp.finish();
    }
}
