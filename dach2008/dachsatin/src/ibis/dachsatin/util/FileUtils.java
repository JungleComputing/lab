package ibis.dachsatin.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class FileUtils {

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


}

