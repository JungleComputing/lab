package ibis.dachmaestro;

import ibis.util.RunProcess;

/**
 * This class implements a divide-and-conquer parallel comparator for a given list of image pairs.
 * @author Kees van Reeuwijk, Jason Maassen
 *
 */
public class Comparator {

    /** Contractual obligation. */
    private static final long serialVersionUID = -858338988356512054L;

    private String compare(Pair pair, String exec) {

        if (exec == null) { 		
            exec = System.getenv("DACHCOMPARATOR");

            if (exec == null ) {
                return "No command found!";
            }
        }

        System.out.println("Comparing '" + pair.before + "' and '" + pair.after + "'");

        String command [] = {
            exec,
            pair.before.getAbsolutePath(),
            pair.after.getAbsolutePath()
        };

        RunProcess p = new RunProcess(command);
        p.run();

        int exit = p.getExitStatus();

        if (exit != 0) {
            String cmd = "";

            for (String c: command) {
                if (!cmd.isEmpty()) {
                    cmd += ' ';
                }
                cmd += c;
            }

            return "Comparison command '" + cmd + "' failed: stdout: " + new String(p.getStdout())
            + " stderr: " + new String(p.getStderr());
        }

        System.out.println("Completed '" + pair.before + "' and '" + pair.after + "'");

        return new String(p.getStdout());
    }        

}
