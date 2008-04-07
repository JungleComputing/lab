/**
 * 
 */
package ibis.videoplayer;

import ibis.maestro.JobResultValue;

class Frame implements JobResultValue {
    private static final long serialVersionUID = 8797700803728846092L;
    final short r[];
    final short g[];
    final short b[];
    final int frameno;
    
    Frame( int frameno, short r[], short g[], short b[] ){
	this.r = r;
	this.g = g;
	this.b = b;
        this.frameno = frameno;
    }
}