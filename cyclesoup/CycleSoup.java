// File: $Id: CycleSoup.java,v 1.24 2005/05/25 19:15:38 reeuwijk Exp $

import ibis.satin.SatinObject;
import ibis.util.RunProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Kees van Reeuwijk.
 *
 * Command-line handling of a render farm.
 */
public class CycleSoup extends SatinObject implements CycleSoupSatinInterface {
    private static final boolean traceCommands = true; 
    private static final boolean verbose = true;
    private int label = 0;

    /** The (maximal) number of jobs handed out to the external script. */ 
    public final int batchSize = 10;

    private CycleSoup() {
        // Nothing interesting.
    }

    static {
        // Set some ibis properies that are essential for this program
        //System.setProperty( "satin.ft", "true" );
        System.setProperty( "satin.branching-factor", "2" );
    }

    /**
     * Given a file <code>f</code>, returns a byte array with the contents of thie file.
     * @param f The file to read.
     * @return The content of the file.
     * @throws IOException
     */
    public static byte[] readFile( File f ) throws IOException
    {
        final int sz = (int) f.length();
        
        byte buf[] = new byte[sz];
        
        FileInputStream s = new FileInputStream( f );
        int n = s.read( buf );
        if( n != sz ){
            System.err.println( "File is " + sz + " bytes, but I could only read " + n + " bytes. I give up." );
            System.exit( 1 );
        }
        s.close();
        return buf;
    }

    /**
     * Given a file <code>f</code> and a byte buffer <code>buf</code>, create the given file, and fill it
     * with the bytes in <code>buf</code>
     * @param f The file to create.
     * @param buf The contents of the file.
     * @throws IOException
     */
    public static void writeFile( File f, byte buf[] ) throws IOException
    {
        FileOutputStream output = new FileOutputStream( f );
        output.write( buf );
        output.close();
    }

    private static void removeSandbox( File f )
    {
        File files[] = f.listFiles();

        for( int i=0; i<files.length; i++ ){
            files[i].delete();
        }
        f.delete();
    }

    /** Print the platform version that is used. */
    static String getPlatformVersion()
    {
        java.util.Properties p = System.getProperties();
        
        return "Java " + p.getProperty( "java.version" ) + " (" + p.getProperty( "java.vendor" ) + ") on " + p.getProperty( "os.name" ) + " " + p.getProperty( "os.version" ) + " (" + p.getProperty( "os.arch" ) + ")";
    }

    /**
     * Given a bunch of bytes that represent a script, and a list of jobs to run, construct
     * a sandbox, fill it with a script file, and execute it with the given jobs as parameters. 
     * @param script The script text to executed.
     * @param jobnames The jobs to to run in the script.
     * @return Wether the script was executed sucessfully.
     */
    private boolean doOneRender( byte script[], String jobnames[] )
    {
        try {
            if( traceCommands ) {
                System.out.print( "Running script on " + jobnames.length + " jobs ... " );
            }
            File sandbox = new File( "/tmp/sandbox-" + label++ );
            File scriptFile = new File( sandbox, "script" );
            sandbox.mkdir();
            writeFile( scriptFile, script );
            RunProcess p = new RunProcess( new String[] { "chmod", "+x", scriptFile.getAbsolutePath() }, null );
            int exitcode = p.getExitStatus();
            if( exitcode != 0 ) {
                System.err.println( "chmod execution returned exit code " + exitcode );
                byte e[] = p.getStderr();
                System.err.write( e );
                return false;                
            }
            String cmd[] = new String[jobnames.length+1];
            cmd[0] = scriptFile.getAbsolutePath();
            for( int i = 0; i < jobnames.length; i++ ) {
                cmd[i+1] = jobnames[i];
            }
            p = new RunProcess( cmd, null, sandbox );
            exitcode = p.getExitStatus();
            if( traceCommands ) {
                System.out.println( " done" );
            }
            if( exitcode != 0 ){
                if( verbose ){
                    System.out.println( "Script execution returned exit code " + exitcode );
                }
                byte e[] = p.getStderr();
                System.err.write( e );
                byte o[] = p.getStdout();
                System.err.write( o );
                return false;
            }
            removeSandbox( sandbox );
            return true;
        }
        catch (IOException e) {
            if( verbose ) {
                System.out.println( "Script execution failed: " + e );
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Applies a given script to the given list of jobs.
     * This is done by recursively dividing the list into smaller pieces until
     * a reasonable length is reached.
     * @param script The script to execute.
     * @param jobs The list of job names to execute.
     * @return The list of jobs that failed to execute.
     */
    public String [] applyScript( byte script[], String jobs[] )
    {
        if( jobs.length == 0 ) {
            return null;
        }
        if( jobs.length<=batchSize ) {
            if( doOneRender( script, jobs ) ) {
                return null;
            }
            return jobs;
        }
        int mid = jobs.length/2;
        String j1[] = new String[mid];
        String j2[] = new String[jobs.length-mid];
        System.arraycopy( jobs, 0, j1, 0, mid );
        System.arraycopy( jobs, mid, j2, 0, j2.length );
        String r2[] = applyScript( script, j2 );
        String r1[] = applyScript( script, j1 );
        sync();
        
        // Now assemble the list of failed jobs from the sublists. If one of the sublists
        // is null we can simply return the other one.
        if( r1 == null ) {
            return r2;
        }
        if( r2 == null ) {
            return r1;
        }
        
        // Both are not null, we'll have to construct a new list.
        String res[] = new String[r1.length+r2.length];
        System.arraycopy( r1, 0, res, 0, 0 );
        System.arraycopy( r2, 0, res, j1.length, j2.length );
        return res;
    }

    /**
     * Print some text explaining the required command-line parameters of the program.
     */
    private static void showUsage()
    {
        System.out.println( "Usage: java CycleSoup <flag>...<flag> [command]" );
        System.out.println( "Where each <flag> is one of:" );
        System.out.println( " --help\t\t\tShow this help text and stop." );
        System.out.println( " --jobs <file>\tSpecify the file listing the jobs to execute." );
        System.out.println( " --script <file>\tThe script to use." );
        System.out.println( " -h\t\t\tShow this help text and stop." );
        System.out.println( " -j <file>\t\tSpecify the file listing the jobs to execute." );
        System.out.println( " -s <file>\t\tThe script to use." );
    }

    /** 
     * Given a file, read the lines of the file into an array of Strings.
     * @param f The file to read.
     * @return The lines of the file as an array of Strings.
     * @throws IOException
     */
    private static String[] readLines( File f ) throws IOException
    {
        ArrayList l = new ArrayList();
    
        BufferedReader in = new BufferedReader( new FileReader( f ) );
        
        for(;;){
            String line = in.readLine();
            if( line == null ) {
                break;
            }
            l.add( line );
        }
        String res[] = new String[l.size()];
        for( int i=0; i<res.length; i++ ) {
            res[i] = (String) l.get( i );
        }
        return res;
    }

    /**
     * The command-line interface of the render queue.
     * @param args The command-line parameters of the invocation.
     */
    public static void main( String[] args )
    {
        String script = null;
        File jobsFile = null;

        for( int i=0; i<args.length; i++ ){
            String arg = args[i];
            
            if( arg.equals( "--help" ) || arg.equals( "-h" ) ){
                showUsage();
                System.exit( 0 );
            }
            else if( arg.equals( "--script" ) || arg.equals( "-s" ) ){
                i++;
                if( i>=args.length ){
                    System.err.println( "Command line ends after `" + arg + "', but that should be followed by a file name" );
                    System.exit( 1 );
                }
                script = args[i];
            }
            else if( arg.equals( "--jobs" ) || arg.equals( "-j" ) ){
                i++;
                if( i>=args.length ){
                    System.err.println( "Command line ends after `" + arg + "', but that should be followed by a file name" );
                    System.exit( 1 );
                }
                jobsFile = new File( args[i] );
            }
            else {
                System.err.println( "Unknown option `" + arg + "'" );
                System.exit( 1 );
            }
        }
        
        if( verbose ) {
            System.out.println( getPlatformVersion() );
        }

        // This is simply because I'm too lazy ATM. TODO: remove this.
        if( script == null ) {
            script = "povrender";
        }
        if( jobsFile == null ) {
            System.err.println( "No jobs file specified" );
            System.exit( 1 );
        }
        try {
            String jobs[] = readLines( jobsFile );

            CycleSoup s = new CycleSoup();
            byte scriptContents[] = readFile( new File( script ) );
            String failedJobs[] = s.applyScript( scriptContents, jobs );
            s.sync();
            if( failedJobs != null ) {
                if( failedJobs.length == jobs.length ) {
                    System.out.println( "All jobs failed" );
                    System.exit( 1 );
                }
                if( failedJobs.length != 0 ) {
                    System.out.println( "The following jobs failed" );
                    for( int i=0; i<failedJobs.length; i++ ){
                        String job = failedJobs[i];

                        System.out.println( job );
                    }
                }
            }
        }
        catch( Exception e ){
            System.out.println( "Caught exception " + e );
            e.printStackTrace();
        }
    }
}
