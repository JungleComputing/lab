/**
 * 
 */
package ibis.videoplayer;

import ibis.maestro.JobResultValue;

class Frame implements JobResultValue {
    private static final long serialVersionUID = 8797700803728846092L;
    final int array[];
    
    Frame( int array[] ){
        this.array = array;
    }
}