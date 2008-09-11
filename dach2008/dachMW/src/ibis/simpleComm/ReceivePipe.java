package ibis.simpleComm;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

import org.apache.log4j.Logger;

public class ReceivePipe {

	private static final Logger logger = Logger.getLogger("ibis.simpleComm.receivepipe");
	
	private static final int DEFAULT_CLOSE_TIMEOUT = 10000;
	private static final int DEFAULT_RECEIVE_TIMEOUT = 120000;
	
	private ReceivePort rp;
	private final String pipeID;
	
	ReceivePipe(final String pipeID) { 
		this.pipeID = pipeID;
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
		
		return (tmp.length != 0);
	}
	
	public int receive(byte [] data) { 
		
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
	
	
}
