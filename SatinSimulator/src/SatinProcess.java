import desmoj.core.simulator.SimProcess;

/**
 * @author Kees van Reeuwijk
 *
 */
public class SatinProcess extends SimProcess
{
    SatinProcess workpool[];

    /** constructs a process... 
     * @param model The model the process belongs to.
     */
    public SatinProcess( SatinSimulator model )
    {
	super( model, "SatinProcess", true );
    }
    
    /** Set the work pool to the given list of processes.
     * @param pl The workpool.
     */
    public void setWorkpool( SatinProcess pl[] )
    {
	workpool = pl;
    }

    /** describes this process's life cycle */
    public void lifeCycle()
    {
	// define behaviour here
    }
}