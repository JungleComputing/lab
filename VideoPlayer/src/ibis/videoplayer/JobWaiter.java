package ibis.videoplayer;

import java.util.ArrayList;

import ibis.maestro.CompletionListener;
import ibis.maestro.Job;
import ibis.maestro.JobResultValue;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;

/**
 * Waits for the given set of jobs.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class JobWaiter implements CompletionListener {
    private int jobNo = 0;
    private int outstandingJobs = 0;
    private ArrayList<JobResultValue> results = new ArrayList<JobResultValue>();

    private static class WaiterTaskIdentifier implements TaskIdentifier {
	final int id;
	
	WaiterTaskIdentifier( int id )
	{
	    this.id = id;
	}
    }

    public synchronized void submit( Node node, Job j )
    {
	TaskIdentifier id = new WaiterTaskIdentifier( jobNo++ );
	outstandingJobs++;
	node.submitTask( j, this, id );
    }

    /**
     * 
     * @param node
     * @param id
     * @param result
     */
    @Override
    public void jobCompleted( Node node, TaskIdentifier id, JobResultValue result )
    {
	int ix = ((WaiterTaskIdentifier) id).id;
	results.set( ix, result );
	outstandingJobs--;
    }

    /**
     * Wait for all jobs to be completed.
     * @return The array of all reported job results.
     */
    public JobResultValue[] sync()
    {
	while( true ) {
	    if( outstandingJobs == 0 ) {
		break;
	    }
	    // FIXME: actually do some waiting.
	}
	JobResultValue res[] = new JobResultValue[results.size()];
	results.toArray( res );
	return res;
    }
}
