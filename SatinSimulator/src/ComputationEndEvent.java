import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;


public class ComputationEndEvent extends Event
{
	public ComputationEndEvent(Model owner, String name, boolean showInTrace) {
		super( owner, name, showInTrace );
	}

	@Override
	public void eventRoutine( Entity who )
	{
		SatinJob p = (SatinJob) who;
		p.decrementActiveChildren();
	}

}
