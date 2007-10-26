package ibis.lab.Reference;

import ibis.satin.Spawnable;

/**
 * @author Kees van Reeuwijk
 *
 */
public interface ReferenceSatinInterface extends Spawnable {
    /**
     * The spawned method.
     * @param level The recursion level.
     */
    void spawn( int level );
}
