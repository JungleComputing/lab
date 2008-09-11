package ibis.simpleComm;

import java.io.IOException;

import org.apache.log4j.Logger;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class SendPipe {

	private static final Logger logger = Logger.getLogger("ibis.simpleComm.sendpipe");
	
	private final SendPort sp;
	
	private final IbisIdentifier target;
	private final String pipeID;
	
	
	SendPipe(final SendPort sp, final IbisIdentifier target, final String pipeID) { 
		this.sp = sp;
		this.target = target;
		this.pipeID = pipeID;
	}
	
	
	public void close() { 
		try { 
			sp.close();
		} catch (Exception e) {
			logger.warn("Failed to close pipe to " + target + " - " + pipeID);
		}
	}
		
	public boolean send(byte [] data, int from, int len) { 
		
		WriteMessage wm = null;
		
		try { 
			wm = sp.newMessage();
			wm.writeInt(len);
			wm.writeArray(data, from, len);
			wm.finish();
			return true;
		} catch (IOException e) {

			logger.warn("Failed to send to " + target + " - " + pipeID, e);

			if (wm != null) { 
				wm.finish(e);
			}
			
			close();
			return false;
		}
	}
	
	
}
