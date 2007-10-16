package nl.cvr.Lab;

import ibis.satin.SatinObject;

/** */
class CycleSoup extends SatinObject implements CycleSoupSatinInterface {
    private static final long serialVersionUID = -2903802790642403639L;
    
    @SatinMethod int run( int n ) { return n+1; }
}

/**
 * @author Kees van Reeuwijk
 *
 */
public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
	System.out.println( "Hello world" );
    }

}
