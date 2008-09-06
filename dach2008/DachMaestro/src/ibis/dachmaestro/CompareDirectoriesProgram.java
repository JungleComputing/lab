package ibis.dachmaestro;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

class CompareDirectoriesProgram
{
    private static class FileInfo
    {
	final File file;
	private long length = -1L;

	FileInfo( File f )
	{
	    this.file = f;
	}

	long length()
	{
	    if( length<0 ) {
		length = file.length();
	    }
	    return length;
	}
    }

    private static void add( ArrayList<FileInfo> files, File f )
    {
	if( f.isFile() ) {
	    files.add( new FileInfo( f ) );
	}
	else if( f.isDirectory() ) {
	    for( File f1: f.listFiles() ) {
		add( files, f1 );
	    }
	}
	else {
	    System.err.println( "Not a file or directory: " + f );
	    System.exit( 1 );
	}
    }
    
    private static FileInfo[] buildFileList( File dir )
    {
	ArrayList<FileInfo> l = new ArrayList<FileInfo>();
	
	add( l, dir );
	return l.toArray( new FileInfo[l.size()] );
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

    private static byte[] readFile( File f, long len )
    {
	byte res[] = new byte[(int) len];
	FileInputStream stream = null;
	try {
	    stream = new FileInputStream( f );
	    boolean ok = readBuffer( stream, res );
	    if( !ok ) {
		System.err.println( "Cannot read file '" + f + "'" );
	    }
	}
	catch( IOException x ) {
	    System.err.println( "Cannot read file '" + f + "': " + x.getLocalizedMessage() );
	    return null;
	}
	finally {
	    try {
		if( stream != null ) {
		    stream.close();
		}
	    }
	    catch( IOException e ) {
		// Nothing we can do
	    }
	}
	return res;
    }
    
    private static boolean areEqualFiles( FileInfo a, FileInfo b )
    {
	if( a.length() != b.length() ) {
	    return false;
	}
	byte fa[] = readFile( a.file, a.length() );
	byte fb[] = readFile( b.file, b.length() );
	
	if( fa == null || fb == null ) {
	    return false;
	}

	return Arrays.equals( fa, fb );
    }

    private static void reportDifferences( FileInfo la[], FileInfo lb[] )
    {
	int matches = 0;
	int onlyFirst = 0;
	int onlySecond = 0;

	for( int i=0; i<la.length; i++ ) {
	    FileInfo fia = la[i];
	    boolean foundMatch = false;

	    for( int j=0; j<lb.length; j++ ) {
		FileInfo fib = lb[j];

		if( fib != null ) {
		    if( areEqualFiles( fia, fib ) ) {
			lb[j] = null; // Remove it from the list.
			foundMatch = true;
			matches++;
		    }
		}
	    }
	    if( !foundMatch ) {
		System.out.println( "Only in first directory: " + fia.file );
		onlyFirst++;
	    }
	}
	for( FileInfo fib: lb ) {
	    if( fib != null ) {
		System.out.println( "Only in second directory: " + fib.file );
		onlySecond++;
	    }
	}
	System.out.println( "Matches: " + matches );
	System.out.println( "Only in first: " + onlyFirst );
	System.out.println( "Only in second: " + onlySecond );
    }

    /** Command-line interface.
     * 
     * @param args The command-line arguments.
     */
    public static void main( String args[] )
    {
	System.out.println( "args=" + Arrays.deepToString(args ) );
	if( args.length != 2 ) {
	    System.err.println( "Usage: CompareDirectoriesProgram <dir> <dir>" );
	    System.exit( 1 );
	}
	File dira = new File( args[0] );
	File dirb = new File( args[1] );
	FileInfo la[] = buildFileList( dira );
	FileInfo lb[] = buildFileList( dirb );

	reportDifferences( la, lb );
    }
}
