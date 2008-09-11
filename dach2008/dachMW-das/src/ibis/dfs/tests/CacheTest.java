package ibis.dfs.tests;

import ibis.dfs.CachedFile;
import ibis.dfs.DFSFileCache;
import ibis.dfs.ErrorReply;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class CacheTest {

	private static final long BLOCK_SIZE = 128*1024;
	
	private final DFSFileCache cache; 
	private final long maxSize;

	private long totalRead = 0;
	private int totalAdded = 0;
	
	private ArrayList<String> cached = new ArrayList<String>();

	class Consumer extends Thread { 
		
		private final long sleepTime;
		private final File [] files;
		
		Consumer(final long sleepTime, final File [] files) { 
			this.sleepTime = sleepTime;
			this.files = files;
		}
		
		public void run() { 
			
			for (File f : files) { 
				try { 
					Thread.sleep(sleepTime);
				} catch (Exception e) {
					// TODO: handle exception
				}
				
				if (!cache.removeFile(f.getName())) { 
					System.err.println("Failed to remove: " + f.getName());
				} else {
					System.err.println("Removed from cache: " + f.getName());
				}
			}
		}
	}
	
	public CacheTest(long maxSize) {
		this.maxSize = maxSize;
		cache = new DFSFileCache(maxSize);		
	}

	private void read(File f, CachedFile cf) { 
		
		BufferedInputStream in = null;

		try {
			in = new BufferedInputStream(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			System.err.println("Failed to open file: " + f);
			return;
		}

		long total = f.length();
		long bytes = 0;
		int index = 0;
		
		try { 
			while (bytes < total) { 
				byte [] buffer = new byte[(int) BLOCK_SIZE];
				
				int read = in.read(buffer, 0, (int) BLOCK_SIZE);

				if (read == -1) {
					System.err.println("Unexpected end of file: " + f.getPath());
					return;
				}

				cf.putBlock(buffer, index++);
				bytes += read;
			} 
		} catch (Exception e) {
			System.err.println("Failed to completely read " + f);
			e.printStackTrace(System.err);
		}	
	}
	
	private void add(File f) {
	
		long len = f.length();
	
 		/*
		while (cache.getCacheSize() > 0 && cache.getCacheSize() + len > maxSize) { 
			
			if (cached.size() == 0) { 
				System.err.println("Not enough memory, but cache is empty!");
				break;
			}
	
			cache.removeFile(cached.remove(0));
		}*/
		
		if (!cache.waitForSpace(f.length(), 10000)) { 
			System.err.println("Failed to get space in the cache for: " + f);
			return;
		} else { 
			System.out.println("Adding file to cache: " + f);
		}
		
		CachedFile cf = new CachedFile(f.getName(), BLOCK_SIZE, len);
		cache.addFile(cf); 
		cached.add(f.getName());
		read(f, cf);
		
		totalRead += len;
		totalAdded++; 
	}	
	
	public void start(File dir, long sleepTime) { 
	
		File [] files = dir.listFiles();
		
		Consumer s = new Consumer(sleepTime, files);
		s.start();
		
		long start = System.currentTimeMillis();
		
		for (File f : files) { 
	
			if (f.canRead() && f.isFile()) { 
				add(f);
			}
		}
		
		long end = System.currentTimeMillis();
		
		long mbit = (totalRead*8) / ((end-start) * 1000);
		
		System.out.println("Added " + totalAdded + " files (" + totalRead + " bytes) in " +
				(end-start) + " ms. (" + mbit + " MBit/s)");

	}
	
	public static void main(String [] args) { 

		File dir = null;
		long maxSize = 1024*1024*1024;
		long sleepTime = 2000;
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-dir") && i < args.length-1) {
				dir = new File(args[++i]);
			} else if (args[i].equals("-cacheSize") && i < args.length-1) {
				maxSize = Long.parseLong(args[++i]);
			} else if (args[i].equals("-sleep") && i < args.length-1) {
				sleepTime = Long.parseLong(args[++i]);
			} else { 
				System.err.println("Unknown option " + args[i]);
				System.exit(1);
			}
		}

		if (dir == null) { 
			System.err.println("Directory not set!");
			System.exit(1);
		}
		
		if (maxSize <= 0) { 
			System.err.println("Cache size has weird value!");
			System.exit(1);
		}

		try { 
			new CacheTest(maxSize).start(dir, sleepTime);
		} catch (Exception e) {
			System.err.println("OOps!");
			e.printStackTrace(System.err);			
		}
		
	}
}
