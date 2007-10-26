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
    private static final int preSpawnDelay = 10;
    private static final int preSyncDelay = 10;
    private static final int postSyncDelay = 20;

    /**
     * The spawned method.
     * @param level The recursion level.
     */
    public void spawn( int level ) {
        try{
            Thread.sleep( preSpawnDelay );
            if( level>0 ) {
                spawn( level-1 );
                spawn( level-1 );
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
        
        long start = System.currentTimeMillis();
        r.spawn( 8 );
        r.sync();
        long stop = System.currentTimeMillis();
        System.out.println( "Run time: " + (stop-start) + " ms" );

    }

}
