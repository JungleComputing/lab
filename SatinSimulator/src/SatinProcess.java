import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.SimProcess;

/**
 * @author Kees van Reeuwijk
 *
 */
public class SatinProcess extends SimProcess
{
	private SatinProcess workpool[];
	private ProcessQueue workQueue;

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
		while( true ){
			SatinJob job = (SatinJob) workQueue.first();
			if( job != null ){
				workQueue.remove( job );
			}
			else {
				
			}
		}
	}
	
	public void queueJob( SatinJob job )
	{
		workQueue.insert( job );
	}
}