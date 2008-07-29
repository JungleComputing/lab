package ibis.dachsatin.deployment.wrapper;

import ibis.dachsatin.util.FileUtils;
import ibis.util.RunProcess;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Wrapper {

	private static PrintStream out = System.out;
	private static PrintStream err = System.err;
	
	private static String [] mountCMD = { "/home/jason/bin/gfarm2fs", "" }; 
	private static String [] umountCMD = { "/home/jason/bin/fusermount", "-u", "" };
	
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
		
		if (result != 0) { 
			throw new IOException("Failed to mount DFS!");			
		}
		
		File check = new File(dfsDir + File.separator + "problems");
		
		if (!check.exists()) { 
			throw new IOException("DFS is empty ?");			
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
		
		String out = System.getProperty("dach.dir.output");
		
		if (out == null) { 
			err.println("Ouput directory not set!");
			System.exit(1);
		}
		
		try { 
			uniqueID = FileUtils.createUniqueName();
		} catch (Exception e) {
			err.println("Failed to generate unique name!");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	
		try { 
			System.setOut(new PrintStream(out + File.separator + "out." + uniqueID));
			System.setErr(new PrintStream(out + File.separator + "err." + uniqueID));
		} catch (Exception e) {
			err.println("Failed to re-route stdout/stderr!");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		
		try { 
			createTMP();
			mountDFS();
		} catch (Exception e) {
			System.err.println("Failed to init system!");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		System.setProperty("dach.dir.data", dfsDir);
		System.setProperty("dach.dir.tmp", tmpDir);
		System.setProperty("dach.machine.id", uniqueID);
		
		try { 
			
			System.out.println("Using wrapper for class: " + args[0]);
			
			Class<?> c = Class.forName(args[0]);
			Method m = c.getDeclaredMethod("main", new Class [] { String [].class });
			m.invoke(null, new Object [] { Arrays.copyOfRange(args, 1, args.length) });
		} catch (Exception e) {
			System.err.println("Failed to wrap java class!!");
			e.printStackTrace(System.err);
	
		}
		
		try {
			umountDFS();
		} catch (IOException e) {
			System.err.println("Failed to unmount DFS!");
			e.printStackTrace(System.err);
		}
		
		FileUtils.deleteDirectory(tmpRoot);
	}	
}
