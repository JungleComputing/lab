package ibis.dfs;

import java.util.HashMap;

public class DFSFileCache {

	private final HashMap<String, CachedFile> cache = new HashMap<String, CachedFile>();
	
	private final long maxCacheSize; 
	private long cacheSize; 

	public DFSFileCache(long maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}

	
	
	public synchronized long getCacheSize() { 
		return cacheSize;
	} 
		
	public synchronized boolean containsFile(String path) { 
		return cache.containsKey(path);
	}
	
	public synchronized boolean waitForSpace(long size, long timeout) { 
	
		if (size > maxCacheSize) { 
			return false;
		}
		
		long end = System.currentTimeMillis() + timeout;
		
		while (cacheSize + size > maxCacheSize) { 
		
			if (timeout > 0) { 				
				long tmp = end - System.currentTimeMillis();
				
				if (tmp <= 0) { 
					return false;
				} else { 
					try { 
						wait(tmp);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			} else { 
				try { 
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		
		return true;
	}
	
	public synchronized boolean waitForFile(String path, long timeout) { 
		
		if (cache.containsKey(path)) { 
			return true;
		}
			
		long end = System.currentTimeMillis() + timeout;
		
		while (!cache.containsKey(path)) { 
		
			if (timeout > 0) { 				
				long tmp = end - System.currentTimeMillis();
				
				if (tmp <= 0) { 
					return false;
				} else { 
					try { 
						wait(tmp);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			} else { 
				try { 
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		
		return cache.containsKey(path);
	}
	
	public synchronized boolean removeFile(String path) { 

		CachedFile tmp = cache.remove(path);
		
		if (tmp != null) { 
			cacheSize -= tmp.fileSize;
			notifyAll();
			return true;
		}
		
		return false;
	}
	
	public CachedFile getFile(String path) { 
		return cache.get(path);
	}

	public CachedFile getAndRemoveFile(String path) {

		CachedFile tmp = cache.remove(path);
		
		if (tmp != null) { 
			cacheSize -= tmp.fileSize;
			notifyAll();
		}

		return tmp;
	}
	
	public void addFile(CachedFile c) { 
		cache.put(c.path, c); 
		cacheSize += c.fileSize; 
	}
	

	
}
