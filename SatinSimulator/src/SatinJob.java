import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.SimTime;


/** A job. */
public class SatinJob extends SimProcess {
	private final SatinProcess processor;
	private final Model model;
	private static final double PROCESS_DURATION = 4.2;
	private final int depth;
	private static final boolean TRACE_JOBS = true;

	public SatinJob( Model model, String name, boolean trace, SatinProcess processor, int depth )
	{
		super( model, name, trace );
		this.processor = processor;
		this.model = model;
		this.depth = depth;
	}

	@Override
	public void lifeCycle()
	{
		activate( new SimTime( PROCESS_DURATION ) );
		if( depth>0 ){
			SatinJob joba = new SatinJob( model, "job" + depth + "a", TRACE_JOBS, processor, depth-1 );
			SatinJob jobb = new SatinJob( model, "job" + depth + "b", TRACE_JOBS, processor, depth-1 );
			processor.queueJob( joba );
			processor.queueJob( jobb );
		}
	}

}
