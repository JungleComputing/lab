package ibis.dachmaestro;

import ibis.maestro.Job;

import java.io.File;

/**
 * Command-line interface.
 * 
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Main {

    private static void usage() { 
        System.err.println("Usage: Main <directory>");
        System.exit(1);
    }

    /**
     * Runs this program.
     * @param args The command-line parameters.
     */
    public static void main( String[] args )
    {
        Job job;

        long start = System.currentTimeMillis();

        if (args.length == 0) { 
            usage();
        }

        File dir = null;
        boolean verbose = false;
        String command = null;

        for (int i=0;i<args.length;i++) { 

            if (args[i].equals("-v") || args[i].equals("--verbose")) { 
                verbose = true;
            } else if (args[i].equals("-c") || args[i].equals("--command")) {
                if (command == null) { 
                    command = args[++i];
                } else { 
                    System.err.println("Command already specified!");
                    System.exit(1);
                }
            } else if (args[i].equals("-d") || args[i].equals("--directory")) { 
                if (dir == null) {  
                    dir = new File(args[++i]);
                } else { 
                    System.err.println("Directory already specified!");
                    System.exit(1);
                }    			
            } else { 
                usage();
            }
        }

        if (dir == null) { 
            System.err.println("No directory specified!");
            System.exit(1);
            return;  // Only here for the stupid code analyzer.
        }

        if (command == null) { 
            System.err.println("No command specified!");
            System.exit(1);	
        }

        if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) { 
            System.err.println("Directory " + dir + " cannot be accessed!");
            System.exit(1);
        }

        FindPairs finder = new FindPairs(dir);

        Pair [] pairs = finder.getPairs();

        if (pairs.length == 0) { 
            System.err.println("No pairs found in directory " + args[0]);
            System.exit(1);
        }

        if (verbose) { 
            System.out.printf("Starting comparison of " + pairs.length + " pairs.");        	        	        	
        }

        Comparator c = new Comparator();

        long end = System.currentTimeMillis();

    }

}
