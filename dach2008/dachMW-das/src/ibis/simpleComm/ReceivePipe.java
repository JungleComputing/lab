package ibis.simpleComm;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

import org.apache.log4j.Logger;

public class ReceivePipe implements Runnable {

	private static final Logger logger = Logger.getLogger("ibis.simpleComm.receivepipe");
	
	private static final int DEFAULT_CLOSE_TIMEOUT = 10000;
	private static final int DEFAULT_RECEIVE_TIMEOUT = 120000;
	
	private ReceivePort rp;
	private final String pipeID;
	
	private final int bufferSize;
	
	private TransferBuffer emptyBuffer;   
	private TransferBuffer fullBuffer;
	
	public class TransferBuffer { 
		public final byte [] data;
		public int used;
		
		TransferBuffer(int size) { 
			data = new byte[size];
			used = 0;
		}
		
		void reset() { 
			used = 0;
		}
	}
	
	ReceivePipe(final String pipeID, final int bufferSize) { 
		this.pipeID = pipeID;
		this.bufferSize = bufferSize;
		this.emptyBuffer = new TransferBuffer(bufferSize);		
	}
	
	public String getID() { 
		return pipeID;
	}
	
	public void setReceivePort(ReceivePort rp) { 
		this.rp = rp;
	}
	
	public void close() { 
		try { 
			rp.close(DEFAULT_CLOSE_TIMEOUT);
		} catch (Exception e) {
			logger.warn("Failed to close receive pipe  " + pipeID);
		}
	}
	
	public boolean waitForConnection(long timeout) { 
	
		long endTime = System.currentTimeMillis() + timeout;
		
		SendPortIdentifier [] tmp = rp.connectedTo();
		
		while (tmp.length == 0) { 
		
			try { 
				Thread.sleep(500);
			} catch (Exception e) {
				// ignore
			}
			
			tmp = rp.connectedTo();		

			if (System.currentTimeMillis() > endTime) { 
				break;
			}
		}
		
		if (tmp.length > 0) { 
			new Thread(this).start();
		}
		
		return (tmp.length != 0);
	}
	
	public synchronized TransferBuffer receive(TransferBuffer old) { 

		while (emptyBuffer != null) { 
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}

		if (old != null) { 
			emptyBuffer = old;
		} else {
			emptyBuffer = new TransferBuffer(bufferSize);
		}		
		notifyAll();		
		
		while (fullBuffer == null) { 
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}

		TransferBuffer result = fullBuffer; 
		fullBuffer = null;
		notifyAll();		
		return result;
	}
	
	private int receive(byte [] data) { 
		
		ReadMessage rm = null;
		
		try { 
			rm = rp.receive(DEFAULT_RECEIVE_TIMEOUT);
			
			int len = rm.readInt();
			
			if (len > data.length) { 
				logger.error("Buffer is too small to receive all bytes! (need " + len + " got " + data.length);
			}
			
			rm.readArray(data, 0, len);
			rm.finish();
			return len;
		} catch (IOException e) {

			logger.warn("Failed to pipe.receive from " + pipeID, e);

			if (rm != null) { 
				rm.finish(e);
			}
			
			close();
			return -1;
		}
	}
	
	public void run() { 

		TransferBuffer buf = null;
		
		while (true) { 

			synchronized (this) {
				while (emptyBuffer == null) { 
					try { 
						wait();
					} catch (Exception e) {
						// ignored
					}
				}

				buf = emptyBuffer;
				emptyBuffer = null;
			}

			int size = receive(buf.data);
			
			if (size == -1) { 
				return;
			}
			
			buf.used = size;

			synchronized (this) {

				while (fullBuffer != null) { 
					try { 
						wait();
					} catch (Exception e) {
						// ignored
					}
				}

				fullBuffer = buf;
				notifyAll();
			}
		}
	}
}
