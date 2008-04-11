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
        private int runningJobs = 0;

        Listener( int jobCount )
        {
            this.jobCount = jobCount;
        }

        /** Handle the completion of job 'j': the result is 'result'.
         * @param id The job that was completed.
         * @param result The result of the job.
         */
        @Override
        public synchronized void jobCompleted( Node node, TaskIdentifier id, JobResultValue result ) {
            //System.out.println( "result is " + result );
            jobsCompleted++;
            runningJobs--;
            //System.out.println( "I now have " + jobsCompleted + "/" + jobCount + " jobs" );
            if( jobsCompleted>=jobCount ){
                System.out.println( "I got all job results back; stopping test program" );
                node.setStopped();
            }
            notifyAll();
        }

        public synchronized void waitForRoom()
        {
            runningJobs++;
            while( runningJobs>2 ){
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    // Not interesting.
                }
            }
        }
    }

    private static class TestTypeInformation implements TypeInformation {
        private static final long serialVersionUID = -4668477198718023902L;

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
            w.allowJobType( BuildFragmentJob.buildJobType() );
            w.allowJobType( BuildFragmentJob.FetchFrameJob.buildJobType() );
            w.allowJobType( BuildFragmentJob.DecompressFrameJob.buildJobType() );
            w.allowJobType( BuildFragmentJob.ColorCorrectFrameJob.buildJobType() );
            w.allowJobType( BuildFragmentJob.ScaleFrameJob.buildJobType() );
        }

        /**
         * Compares two job types based on priority. Returns
         * 1 if type a has more priority as b, etc.
         * 
         * Job types further down the stream are more important than job types
         * upstream, since once we start a job we should finish it as soon as
         * possible.
         * @param a One of the job types to compare.
         * @param b The other job type to compare.
         * @return The comparison result.
         */
        public int compare( JobType a, JobType b )
        {
            return JobType.comparePriorities( a, b );
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
                listener.waitForRoom();
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
        int frameCount = 0;

        if( args.length == 0 ){
            System.err.println( "Missing parameter: I need a job count, or 'worker'" );
            System.exit( 1 );
        }
        String arg = args[0];
        if( arg.equalsIgnoreCase( "worker" ) ){
            goForMaestro = false;
        }
        else {
            frameCount = Integer.parseInt( arg );
        }
        System.out.println( "Running on platform " + Service.getPlatformVersion() + " args.length=" + args.length + " goForMaestro=" + goForMaestro + "; frameCount=" + frameCount );
        try {
            new BuildVideo().run( frameCount, goForMaestro );
        }
        catch( Exception e ) {
            e.printStackTrace( System.err );
        }
    }
}
