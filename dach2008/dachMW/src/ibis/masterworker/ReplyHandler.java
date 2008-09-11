package ibis.masterworker;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class ReplyHandler {
	
	private static final Logger logger = Logger.getLogger("masterworker.replyhandler");
	
	private HashMap<Long, StealReply> replies = new HashMap<Long, StealReply>();
	
	private boolean done = false;
	
	public synchronized boolean getDone() { 
		return done;
	}
	
	public synchronized void done() { 
		done = true;
		notifyAll();
	}

	public synchronized StealReply waitForReply(long ID) {
		
		Object tmp = replies.get(ID);
		
		while (tmp == null && !done) { 
			
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		
			tmp = replies.get(ID);
		}
		
		if (done) { 
			return null;
		}
		
		return replies.remove(ID);
	}


	public synchronized void clearRequest(long ID) {
		replies.remove(ID);
	}

	public synchronized void registerSingleRequest(long ID) {
		logger.info("Register single req " + ID);
		replies.put(ID, null);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void storeReply(Long ID, StealReply reply) {
		
		if (!replies.containsKey(ID)) { 
			logger.warn("No one is waiting for reply " + ID + " " + reply);
			return;
		}
		
		// single reply
		replies.put(ID, reply);
		notifyAll();
	}
}
