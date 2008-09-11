package ibis.dfs;

public class CachedFile {

	public final String path;
	public final long blockSize;	
	public final long fileSize;	

	private final byte [][] blocks; 
	
	public CachedFile(String path, long blockSize, long fileSize) { 
		
		this.path = path;
		this.fileSize = fileSize;
		this.blockSize = blockSize;
		
		int b = (int)(fileSize / blockSize);
		
		if (fileSize % blockSize != 0) { 
			b++;
		}
		
		this.blocks = new byte[b][];
	}
	
	public int blocks() { 
		return blocks.length;
	}
	
	public synchronized boolean hasBlock(int index) { 
		return (blocks[index] != null);
	} 

	public synchronized byte [] getBlock(int index, long timeout) { 
		
		if (blocks[index] != null) { 
			return blocks[index];
		}

		long end = System.currentTimeMillis() + timeout;

		while (blocks[index] == null) {

			if (timeout > 0) { 

				long tmp = end - System.currentTimeMillis();

				if (tmp <= 0) { 
					return blocks[index];
				} else { 
					try { 
						wait(tmp);
					} catch (Exception e) {
						// ignore
					}	
				}
			} else { 
				try { 
					wait();
				} catch (Exception e) {
					// ignore
				}
			}
		}

		return blocks[index];
	}

	public synchronized void putBlock(byte [] data, int index) {
		blocks[index] = data;
		notifyAll();
	}		
}
