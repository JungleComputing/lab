
import desmoj.core.dist.IntDistUniform;
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
    static final int NUMBER_PROCESSORS = 4;
    private IntDistUniform stealVictim;
    private SatinProcessor processors[] = new SatinProcessor[NUMBER_PROCESSORS];
    private static final int START_LEVEL = 8;

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
        stealVictim = new IntDistUniform( this, "StealVictimStream", 0, NUMBER_PROCESSORS, true, false );
    }

    /** activate dynamic components */
    @Override
    public void doInitialSchedules()
    {
        for( int i=0; i<NUMBER_PROCESSORS; i++ ) {
            SatinProcessor p = new SatinProcessor( this, processors, i );
            processors[i] = p;
        }
        SatinJob rootjob = new SatinJob( this, "rootjob", true, processors[0], START_LEVEL );
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

    /** runs the model.
     * @param args Command-line parameters.
     */
    public static void main( String[] args )
    {

	// create model and experiment
	SatinSimulator model = new SatinSimulator();
	Experiment exp = new Experiment( "<MyModelExperimentName>" );
	// and connect them
	model.connectToExperiment( exp );

	// set experiment parameters
	exp.setShowProgressBar( true );
	SimTime stopTime = new SimTime( 1440.0 );
	exp.tracePeriod( new SimTime(0.0), stopTime );
	exp.stop( stopTime );

	// start experiment
	exp.start();

	// generate report and shut everything off
	exp.report();
	exp.finish();
    }
}
