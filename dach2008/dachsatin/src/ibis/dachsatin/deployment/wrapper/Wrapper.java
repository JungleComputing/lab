package ibis.dachsatin.deployment.wrapper;

import ibis.dachsatin.util.FileUtils;
import ibis.util.RunProcess;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class Wrapper {

	private static PrintStream out = System.out;
	private static PrintStream err = System.err;
	
	private static String [] mountCMD = { "/data/local/gfarm_v2/bin/gfarm2fs", "" }; 
	private static String [] umountCMD = { "f/usr/bin/usermount", "-u", "" };
	
	private static String uniqueID;
	private static String tmpRoot;
	private static String dfsDir;
	private static String tmpDir;
	
	private static void createTMP() throws IOException { 
		
		String tmp = System.getProperty("java.io.tmpdir");
		
		if (tmp == null) { 
			throw new IOException("Property java.io.tmpdir not set!");
		}
		
		tmpRoot = tmp + File.separator + uniqueID + ".dir";
		dfsDir = tmpRoot + File.separator + "dfs";
		tmpDir = tmpRoot + File.separator + "tmp";
		
		FileUtils.createDir(tmpRoot, true);
		FileUtils.createDir(dfsDir, false);
		FileUtils.createDir(tmpDir, false);
	}
	
	private static void mountDFS() throws IOException { 
	
		mountCMD[1] = dfsDir;
		
		System.out.println("Mounting DFS using command " + Arrays.toString(mountCMD));
		
		RunProcess p = new RunProcess(mountCMD);
		p.run();
		int result = p.getExitStatus();
	
		// Note: the DFS may already be mounted, so check first, and only complain if 
		// the problem dir is not there 
		
		File check = new File(dfsDir + File.separator + "problems");
		
		if (!check.exists()) {
			if (result != 0) { 
				throw new IOException("Failed to mount DFS!");			
			} else { 
				throw new IOException("DFS is empty ?");			
			}
		}
	}
	

	private static void umountDFS() throws IOException { 
		
		umountCMD[2] = dfsDir;
	
		System.out.println("Unmounting DFS using command " + Arrays.toString(umountCMD));
		
		RunProcess p = new RunProcess(umountCMD);
		p.run();
		int result = p.getExitStatus();
		
		if (result != 0) { 
			throw new IOException("Failed to unmount DFS!");			
		}
	}
	
	public static void main(String [] args) { 
		
		int dryRun = -1;
		
		String targetClass = null;
		
		ArrayList<String> newArgs = new ArrayList<String>();
 	
		uniqueID = System.getProperty("dach.machine.id");
		
		if (uniqueID == null) { 
			err.println("Machine ID not set!");
			System.exit(0);
		} else { 
			System.out.println("Starting wrapper on " + uniqueID);
		}
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-dryRun") && i != args.length-1) { 
    			dryRun = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-mount") && i != args.length - 1) {
				mountCMD[0] = args[++i];
			} else if (args[i].equals("-unmount") && i != args.length - 1) {
				umountCMD[0] = args[++i];
			} else if (args[i].equals("-class") && i != args.length - 1) {
				targetClass = args[++i];
			} else { 
				newArgs.add(args[i]);
			}
		}

		if (targetClass == null) { 
			err.println(uniqueID + ": Target class not set!");
			System.exit(0);
		}
		
		String out = System.getProperty("dach.dir.output");
		
		if (out == null) { 
			err.println(uniqueID + ": Output directory not set!");
			System.exit(0);
		}
		
		try {
			//uniqueID = FileUtils.createUniqueName(id);
			createTMP();
			mountDFS();
		} catch (Exception e) {
			System.err.println(uniqueID + ": Failed to init system!");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		System.setProperty("dach.dir.data", dfsDir);
		System.setProperty("dach.dir.tmp", tmpDir);
		// System.setProperty("dach.machine.id", uniqueID);
		
		try { 
			System.out.println(uniqueID + ": loading class: " + targetClass);
			
			Class<?> c = Class.forName(targetClass);
			Method m = c.getDeclaredMethod("main", new Class [] { String [].class });
			
			String [] a = newArgs.toArray(new String[newArgs.size()]);
			
			if (dryRun == 0) { 
				System.out.println(uniqueID + ": DryRun -- NOT invoking: " + targetClass + ".main(" 
						+ Arrays.toString(a) + ")");
			} else { 
				m.invoke(null, new Object [] { a });
			}
		} catch (Exception e) {
			System.err.println(uniqueID + ": Failed to wrap java class!!");
			e.printStackTrace(System.err);
		}
		
		try {
			umountDFS();
		} catch (IOException e) {
			System.err.println(uniqueID + ": Failed to unmount DFS!");
			e.printStackTrace(System.err);
		}
		
		FileUtils.deleteDirectory(tmpRoot);

		// Apparently, Satin needs a little persuasion before it wants to stop!
		System.exit(0);
	}	
}
