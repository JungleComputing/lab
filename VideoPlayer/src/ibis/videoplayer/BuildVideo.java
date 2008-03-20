package ibis.videoplayer;

import ibis.maestro.CompletionListener;
import ibis.maestro.JobResultValue;
import ibis.maestro.JobType;
import ibis.maestro.Node;
import ibis.maestro.TaskIdentifier;
import ibis.maestro.TypeInformation;

/**
 * Small test program.
 * 
 * @author Kees van Reeuwijk
 *
 */
public class BuildVideo {
    private static class Listener implements CompletionListener
    {
        int jobsCompleted = 0;
        private final int jobCount;

        Listener( int jobCount )
        {
            this.jobCount = jobCount;
        }

        /** Handle the completion of job 'j': the result is 'result'.
	 * @param id The job that was completed.
	 * @param result The result of the job.
	 */
	@Override
	public void jobCompleted( Node node, TaskIdentifier id, JobResultValue result ) {
	    //System.out.println( "result is " + result );
            jobsCompleted++;
            //System.out.println( "I now have " + jobsCompleted + "/" + jobCount + " jobs" );
            if( jobsCompleted>=jobCount ){
        	System.out.println( "I got all job results back; stopping test program" );
                node.setStopped();
            }
	}
    }

    private static class TestTypeInformation implements TypeInformation {

	/**
	 * Registers that a neighbor supports the given type of job.
	 * @param w The worker to register the info with.
	 * @param t The type a neighbor supports.
	 */
	@Override
	public void registerNeighborType( Node w, JobType t )
        {
	    // Nothing to do.
	}

	/** Registers the initial types of this worker.
	 * 
	 * @param w The worker to initialize.
	 */
	@Override
	public void initialize(Node w)
	{
	    w.allowJobType( BuildFragmentJob.jobType );
            w.allowJobType( ScaleFrameJob.jobType );
            w.allowJobType( FetchFrameJob.jobType );
	}

        /**
         * Compares two job types based on priority. Returns
         * 1 if type a has more priority as b, etc.
         * 
         * Job types further down the stream are more important than job types
         * upstream, since once we start a job we should finish it as soon as
         * possible.
         */
        public int compare( JobType a, JobType b )
        {
            if( a.equals( b ) ){
                return 0;
            }
            if( a.equals( BuildFragmentJob.jobType ) ){
                return 1;
            }
            if( b.equals( BuildFragmentJob.jobType ) ){
                return -1;
            }
            if( a.equals( ScaleFrameJob.jobType ) ){
                return 1;
            }
            if( b.equals( ScaleFrameJob.jobType ) ){
                return -1;
            }
            if( a.equals( FetchFrameJob.jobType ) ){
                return 1;
            }
            if( b.equals( FetchFrameJob.jobType ) ){
                return -1;
            }
            return 0;
        }
	
    }
    
    @SuppressWarnings("synthetic-access")
    private void run( int frameCount, boolean goForMaestro ) throws Exception
    {
        Node node = new Node( new TestTypeInformation(), goForMaestro );
        // How many fragments will there be?
        int fragmentCount = (frameCount+Settings.FRAME_FRAGMENT_COUNT-1)/Settings.FRAME_FRAGMENT_COUNT;
        Listener listener = new Listener( fragmentCount );

	System.out.println( "Node created" );
        if( node.isMaestro() ) {
            System.out.println( "I am maestro; building a movie of " + frameCount + " frames" );
            for( int frame=0; frame<frameCount; frame += Settings.FRAME_FRAGMENT_COUNT ){
                final int endFrame = frame+Settings.FRAME_FRAGMENT_COUNT-1;
        	TaskIdentifier id = node.buildTaskIdentifier( frame );
		BuildFragmentJob j = new BuildFragmentJob( frame, endFrame  );
        	node.submitTask( j, listener, id );
            }
        }
        node.waitToTerminate();
    }

    /** The command-line interface of this program.
     * 
     * @param args The list of command-line parameters.
     */
    public static void main( String args[] )
    {
        boolean goForMaestro = true;
        int jobCount = 0;

        if( args.length == 0 ){
            System.err.println( "Missing parameter: I need a job count, or 'worker'" );
            System.exit( 1 );
        }
        String arg = args[0];
        if( arg.equalsIgnoreCase( "worker" ) ){
            goForMaestro = false;
        }
        else {
            jobCount = Integer.parseInt( arg );
        }
	System.out.println( "Running on platform " + Service.getPlatformVersion() + " args.length=" + args.length + " goForMaestro=" + goForMaestro + "; jobCount=" + jobCount );
	try {
            new BuildVideo().run( jobCount, goForMaestro );
        }
        catch( Exception e ) {
            e.printStackTrace( System.err );
        }
    }
}
