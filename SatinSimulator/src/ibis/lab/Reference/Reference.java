// File: Reference.java

package ibis.lab.Reference;

/**
 * Real Satin program with similar behaviour to the one that
 * is simulated. Usueful to compare real and simulated statistics.
 *
 * @author Kees van Reeuwijk.
 */
public class Reference  extends ibis.satin.SatinObject implements ReferenceSatinInterface
{
    /** Contractual obligation. */
    private static final long serialVersionUID = 7331296260507993758L;
    private static final int preSpawnDelay = 50;
    private static final int preSyncDelay = 50;
    private static final int postSyncDelay = 50;

    /**
     * The spawned method.
     * @param depth The recursion depth.
     */
    public void spawn( int depth ) {
        try{
            Thread.sleep( preSpawnDelay );
            if( depth>0 ) {
                spawn( depth-1 );
                spawn( depth-1 );
                Thread.sleep( preSyncDelay );
                sync();
            }
            else {
                Thread.sleep( preSyncDelay );
            }
            Thread.sleep( postSyncDelay );
        }
        catch( InterruptedException e ){
            System.err.println( "Who interrupted my sleep??: " );
            e.printStackTrace();
        }
    }

    /** The method invoked to start the program.
     * @param args Command-line arguments
     */
    public static void main( String[] args )
    {
        Reference r = new Reference();
        
	System.out.println( "Started" );
        long start = System.currentTimeMillis();
        r.spawn( 11 );
        r.sync();
        long stop = System.currentTimeMillis();
        System.out.println( "Run time: " + (stop-start) + " ms" );

    }

}
