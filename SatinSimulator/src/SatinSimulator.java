
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimTime;

/**
 * @author Kees van Reeuwijk
 *
 */
public class SatinSimulator extends Model
{

    // define model components here

    /** constructs a model... */
    public SatinSimulator()
    {
	super( null, "SatinSimulator", true, true );
    }

    /** initialise static components */
    public void init()
    {
	// TODO:
    }

    /** activate dynamic components */
    public void doInitialSchedules()
    {
	// TODO:
    }

    /** returns a description of this model to be used in the report.
     * @return The description.
     */
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
