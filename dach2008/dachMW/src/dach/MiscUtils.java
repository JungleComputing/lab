package dach;

import ibis.util.RunProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MiscUtils {

	private static String [] top = { "/usr/bin/top", "-b", "-n", "1" };
	
	public static boolean fileExists(File f) { 
		return f.exists() && f.canRead() && !f.isDirectory();
	}

	public static boolean fileExists(String f) { 
		return fileExists(new File(f));
	}

	public static boolean directoryExists(File dir) { 
		return dir.exists() && dir.canRead() && dir.isDirectory();
	}

	public static boolean directoryExists(String dir) {
		return directoryExists(new File(dir));
	}
	
	public static String createUniqueName() throws IOException { 
		
		File tmp1 = File.createTempFile("dach004-", "", null);
		String uniqueID = tmp1.getName();
		tmp1.delete();
		
		System.out.println("Created unique ID: " + uniqueID);
		
		return uniqueID;
	}
	
	public static void createDir(String name, boolean deleteOnExit) throws IOException { 
	
		File dir = new File(name);
		dir.mkdir();
		
		if (!dir.exists()) { 
			throw new IOException("Failed to create directory: " + name);
		}
		
		if (deleteOnExit) { 
			dir.deleteOnExit();
		}
	}
	
	public static boolean deleteDirectory(File dir) {
        // to see if this directory is actually a symbolic link to a directory,
        // we want to get its canonical path - that is, we follow the link to
        // the file it's actually linked to
       
		if (dir == null) { 
			return true;
		}
		
		File candir;
        try {
            candir = dir.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }
  
        // a symbolic link has a different canonical path than its actual path,
        // unless it's a link to itself
        if (!candir.equals(dir.getAbsoluteFile())) {
            // this file is a symbolic link, and there's no reason for us to
            // follow it, because then we might be deleting something outside of
            // the directory we were told to delete
            return false;
        }
  
        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File [] files = candir.listFiles();
      
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
  
                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean deleted = file.delete();
             
                if (!deleted) {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory
                    if (file.isDirectory()) {
                    	deleteDirectory(file);
                    }
  
                    // otherwise, there's nothing else we can do
                }
            }
        }
  
        // now that we tried to clear the directory out, we can try to delete it
        // again
        return dir.delete();  
    }
	
	public static boolean deleteDirectory(String dir) { 
		
		if (dir == null || dir.trim().length() == 0) { 
			return false;
		}
		
		return deleteDirectory(new File(dir));
	}
	
	public static boolean checkFiles(List<File> files) { 
		
		for (File f : files) { 
			
			if (!f.exists()) {
				System.out.println("Have not found result file: " + f.getPath());				
				return false;
			}
		}
		
		return true;	
	} 
	
	public static boolean waitForFiles(List<File> files, long timeout) { 
		
		long end = System.currentTimeMillis() + timeout;
		
		do { 
			if (checkFiles(files)) { 
				return true;
			}
			
			try { 
				Thread.sleep(1000);
			} catch (Exception e) {
				// ignore
			}
			
		} while (System.currentTimeMillis() < end);
		
		return checkFiles(files);
	}

	public static LinkedList<File> selectExistingFiles(List<File> files) { 
		
		LinkedList<File> result = new LinkedList<File>();
		
		ListIterator<File> itt = files.listIterator();
		
		while (itt.hasNext()) { 
			
			File file = itt.next();
			
			if (file.exists()) {
				result.add(file);
				itt.remove();
			}
		}
		
		return result;
	}
	
	public static int run(String [] cmd, StringBuilder out, StringBuilder err) { 
		
		RunProcess p = new RunProcess(cmd);
		p.run();

		out.append(new String(p.getStdout()));
		err.append(new String(p.getStderr()));
			
		return p.getExitStatus();		
	}

	public static Set<String> parseSite(String site) throws Exception { 
		
		Set<String> nodes = new HashSet<String>();
		
		int rangeStart; 
		int rangeEnd;
		
		int index4 = site.indexOf(' ');

		int cores = 0;
		
		if (index4 < 0) { 
			// Single core
			cores = 1;
		} else { 
			
			try { 
				cores = Integer.parseInt(site.substring(index4+1).trim());
			} catch (Exception e) {
				// ignore -- handled in next 'if'
			}
			
			if (cores <= 0) { 
				throw new Exception("Illegal core value in \"" + site + "\"");
			}
			
			site = site.substring(0, index4).trim();			
		}

		int index1 = site.indexOf('[');
		int index2 = site.lastIndexOf(']');

		if (index1 < 0 || index2 < 0) {
			
			for (int c=0;c<cores;c++) { 
				// System.out.println("Adding node " + node + " (core " + c + ")"); 				
				nodes.add(site);
			}

			return nodes;
		}
		
		String nodePreFix = site.substring(0, index1);
		String nodePostFix = site.substring(index2+1);

		int index3 = site.indexOf('-', index1);

		if (index3 < 0) {
			String value = site.substring(index1+1, index2);
			rangeStart = rangeEnd = Integer.parseInt(value);
			
			for (int c=0;c<cores;c++) {
				//System.out.printf("Adding node %s%s%s (core %d)\n", nodePreFix, formatNumber(i, 3), nodePostFix, c); 
				nodes.add(nodePreFix + formatNumber(rangeStart, 3) + nodePostFix);
			}
			
			return nodes;
		}

		rangeStart = Integer.parseInt(site.substring(index1+1, index3));
		rangeEnd = Integer.parseInt(site.substring(index3+1, index2));

		for (int i=rangeStart;i<rangeEnd+1;i++) { 
			for (int c=0;c<cores;c++) {
				//System.out.printf("Adding node %s%s%s (core %d)\n", nodePreFix, formatNumber(i, 3), nodePostFix, c); 
				nodes.add(nodePreFix + formatNumber(i, 3) + nodePostFix);
			}
		}
		
		return nodes;
	}	
	
	public static final String formatNumber(int value, int positions) { 
		
		String tmp = Integer.toString(value);
		
		while (tmp.length() < positions) { 
			tmp = "0" + tmp;
		}
		
		return tmp;
	}
	
	public static List<Set<String>> readSites(String file) throws Exception { 
		
		List<Set<String>> result = new LinkedList<Set<String>>();
		
		BufferedReader r = new BufferedReader(new FileReader(file));
		
		String site = r.readLine();
		
		while (site != null) { 
			
			site = site.trim(); 
			
			if (site.length() > 0 && !site.startsWith("#")) { 
				result.add(MiscUtils.parseSite(site));
			}
			site = r.readLine();
		}
		
		return result;
	}
	
	public static void remoteCopy(String scpExec, String file, String host, String destDir) throws Exception {

		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();

		int exit = MiscUtils.run(new String [] { scpExec, host + ":" + file, destDir }, out, err); 

		if (exit != 0) {
			throw new Exception("Failed to remote copy file " + file + " (stdout: " + out + ") (stderr: " + err + ")\n");
		}
	}
	
	public static void localCopy(String cpExec, String file, String destDir) throws Exception { 
	
		if (!MiscUtils.fileExists(file)) {
			throw new Exception("Input file " + file + " not found\n");
		}
		
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();

		int exit = MiscUtils.run(new String [] { cpExec, file, destDir }, out, err); 

		if (exit != 0) {
			throw new Exception("Failed to copy file " + file + " (stdout: " + out + ") (stderr: " + err + ")\n");
		}
	}		

	public static int getMachineLoad(StringBuilder out, StringBuilder err) { 
		return run(top, out, err);
	}		
	
}

