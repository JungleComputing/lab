package ibis.dachmaestro;

import ibis.maestro.AtomicTask;
import ibis.maestro.Node;
import ibis.maestro.Service;
import ibis.maestro.UnpredictableTask;
import ibis.util.RunProcess;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class FileComparatorTask implements AtomicTask, UnpredictableTask
{
    /** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;
    private final String exec;
    private static final long BENCHMARK_MULTIPLIER = 10;

    /**
     * Returns the name of this task.
     * @return The name.
     */
    @Override
    public String getName()
    {
        return "Compare files";
    }

    FileComparatorTask( String exec ) throws Exception
    {
        this.exec = exec;
    }

    private Result compare( FilePair pair )
    {
        System.out.println( "Comparing files '" + pair.before + "' and '" + pair.after + "'" );
        long startTime = System.nanoTime();

        // FIXME: specify temp directory.
        String command [] = {
            exec,
            pair.before.getAbsolutePath(),
            pair.after.getAbsolutePath()
        };

        RunProcess p = new RunProcess(command);
        p.run();
        long time = System.nanoTime()-startTime;

        int exit = p.getExitStatus();

        if( exit != 0 ) {
            String cmd = "";

            for (String c: command) {
                if (!cmd.isEmpty()) {
                    cmd += ' ';
                }
                cmd += c;
            }

            return new Result(
                null,
                time,
                "Comparison command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
                + " stderr: " + new String( p.getStderr() )
            );
        }

        System.out.println("Completed '" + pair.before + "' and '" + pair.after + "' in " + Service.formatNanoseconds( time ) );

        return new Result( new String( p.getStdout() ), time, null );
    }

    /**
     * Returns true iff we can support this task.
     * @return Whether we can support this task.
     */
    @Override
    public boolean isSupported()
    {
        String hardwarename = System.getProperty( "hardwarename" );
        boolean supported = false;
        if( hardwarename.equals( "x86_64" ) ) {
            supported = true;
        }
        System.out.println( "hardwarename=" + hardwarename + " supported=" + supported );
        return supported;
    }

    /**
     * Runs this task. Specifically: given a pair, runs the comparator.
     * @param in The input.
     * @param node The node this runs on.
     * @return The comparison result.
     */
    @Override
    public Object run( Object in, Node node )
    {
        FilePair pair = (FilePair) in;
        return compare( pair );
    }

    private static final int BANDS = 3;
    private static final int width = 1000;
    private static final int height = 1000;

    /** Given a byte
     * returns an unsigned int.
     * @param v
     * @return
     */
    private static int byteToInt( byte v )
    {
        return v & 0xFF;
    }

    private static byte applyConvolution(
        byte v00, byte v01, byte v02,
        byte v10, byte v11, byte v12,
        byte v20, byte v21, byte v22,
        int k00, int k01, int k02,
        int k10, int k11, int k12,
        int k20, int k21, int k22,
        int weight
    )
    {
        int val =
            k00*byteToInt(v00)+k10*byteToInt(v10)+k20*byteToInt(v20)+
            k01*byteToInt(v01)+k11*byteToInt(v11)+k21*byteToInt(v21)+
            k02*byteToInt(v02)+k12*byteToInt(v12)+k22*byteToInt(v22);
        return (byte) Math.min( 255 , Math.max( 0, (val+weight/2)/weight ) );
    }


    /**
     * Applies the kernel with the given factors to an image, and returns a new
     * image.
     * @return The image with the kernel applied.
     */
    private static long convolution3x3(
        int k00, int k01, int k02,
        int k10, int k11, int k12,
        int k20, int k21, int k22,
        int weight
    )
    {
        byte res[] = new byte[width*height*BANDS];
        byte data[] = new byte[width*height*BANDS];
        final int rowBytes = width*BANDS;

        // Copy the top and bottom line into the result image.
        System.arraycopy( data, 0, res, 0, rowBytes );
        System.arraycopy( data, (height-1)*rowBytes, res, (height-1)*rowBytes, rowBytes );

        /** Apply kernal to the remaining rows. */
        int wix = rowBytes;   // Skip first row.
        byte r00, r01, r02, r10, r11, r12, r20, r21, r22;
        byte g00, g01, g02, g10, g11, g12, g20, g21, g22;
        byte b00, b01, b02, b10, b11, b12, b20, b21, b22;
        for( int h=1; h<(height-1); h++ ) {
            int rix = rowBytes*h;
            r00 = data[rix-rowBytes];
            r01 = data[rix];
            r02 = data[rix+rowBytes];
            rix++;
            g00 = data[rix-rowBytes];
            g01 = data[rix];
            g02 = data[rix+rowBytes];
            rix++;
            b00 = data[rix-rowBytes];
            b01 = data[rix];
            b02 = data[rix+rowBytes];
            rix++;
            r10 = data[rix-rowBytes];
            r11 = data[rix];
            r12 = data[rix+rowBytes];
            rix++;
            g10 = data[rix-rowBytes];
            g11 = data[rix];
            g12 = data[rix+rowBytes];
            rix++;
            b10 = data[rix-rowBytes];
            b11 = data[rix];
            b12 = data[rix+rowBytes];
            rix++;
            // Write the left border pixel.
            res[wix++] = r00;
            res[wix++] = g00;
            res[wix++] = b00;
            for( int w=1; w<(width-1); w++ ) {
                r20 = data[rix-rowBytes];
                r21 = data[rix];
                r22 = data[rix+rowBytes];
                rix++;
                g20 = data[rix-rowBytes];
                g21 = data[rix];
                g22 = data[rix+rowBytes];
                rix++;
                b20 = data[rix-rowBytes];
                b21 = data[rix];
                b22 = data[rix+rowBytes];
                rix++;
                res[wix++] = applyConvolution( r00, r01, r02, r10, r11, r12, r20, r21, r22, k00, k01, k02, k10, k11, k12, k20, k21, k22, weight );
                res[wix++] = applyConvolution( g00, g01, g02, g10, g11, g12, g20, g21, g22, k00, k01, k02, k10, k11, k12, k20, k21, k22, weight );
                res[wix++] = applyConvolution( b00, b01, b02, b10, b11, b12, b20, b21, b22, k00, k01, k02, k10, k11, k12, k20, k21, k22, weight );
                r00 = r10;
                r10 = r20;
                r01 = r11;
                r11 = r21;
                r02 = r12;
                r12 = r22;
                g00 = g10;
                g10 = g20;
                g01 = g11;
                g11 = g21;
                g02 = g12;
                g12 = g22;
                b00 = b10;
                b10 = b20;
                b01 = b11;
                b11 = b21;
                b02 = b12;
                b12 = b22;
            }
            // Write the right border pixel.
            res[wix++] = r11;
            res[wix++] = g11;
            res[wix++] = b11;
        }
        long sum = 0;

        for( byte b: res ) {
            sum += b;
        }
        return sum;
    }

    private static long sharpen()
    {
        return convolution3x3(
            -1,  -1, -1,
            -1,   9, -1,
            -1,  -1, -1,
            1
        );
    }

    private long runBenchmark()
    {
        long startTime = System.nanoTime();
        sharpen();
        return System.nanoTime()-startTime;
    }

    /** Estimate the time to compare two files. (Overrides method in superclass.)
     * We try to get an estimate that is representative of the processor, so that
     * we pick an efficient processor first, but lower than the real execution time,
     * so that the system is encouraged to try all processors (and at least initially
     * spread the load).
     * @return The estimated execution time of a task.
     */
    @Override
    public long estimateTaskExecutionTime()
    {
        long benchmarkTime = runBenchmark();
        return BENCHMARK_MULTIPLIER*benchmarkTime;
    }
}
