package ibis.dachmaestro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An image as a blob of bytes in memory.
 * 
 * @author Kees van Reeuwijk
 *
 */
class Image {
    final String file;
    final byte[] bytes;

    /**
     * @param file The file name of this image.
     * @param bytes The file contents.
     */
    private Image( String file, byte[] bytes )
    {
	this.file = file;
	this.bytes = bytes;
    }

    /**
     * Keep reading bytes from the given stream until the entire buffer has been filled.
     * We might have to give up, however, if end of file is reached. In that case <code>false</code>
     * is returned, and the buffer is necessarily only partially filled. Since this is an error
     * situation, we don't bother to pass on how much of the buffer was filled.
     * @param stream The stream to read from.
     * @param buffer The buffer to fill.
     * @return True iff we managed to fill the entire buffer.
     * @throws IOException Thrown if there is a read error.
     */
    private static boolean readBuffer( InputStream stream, byte buffer[] ) throws IOException
    {
	int offset = 0;

	while( true ) {
	    int n = stream.read( buffer, offset, buffer.length-offset );
	    if( n<0 ) {
		return false;
	    }
	    offset += n;
	    if( offset>=buffer.length ) {
		break;
	    }
	}
	return true;
    }

    File write( File path ) throws IOException
    {
	File f = new File( path, file );
	FileOutputStream stream = new FileOutputStream( f );
	stream.write( bytes );
	stream.close();
	return f;
    }

    static Image read( File path, String file ) throws IOException
    {
	File f = new File( path, file );
	long sz = f.length();
	if( sz>Integer.MAX_VALUE ) {
	    throw new IOException( "Image file " + f + " is too large to fit in a Java array" );
	}
	byte data[] = new byte[(int) sz];
	FileInputStream stream = new FileInputStream( f );

	if( !readBuffer( stream, data ) ) {
	    System.err.println( "Image data ended prematurely" );
	}
	stream.close();
	return new Image( file, data ); 
    }
}
