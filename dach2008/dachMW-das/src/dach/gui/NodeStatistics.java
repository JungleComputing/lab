package dach.gui;

import java.util.LinkedList;

public class NodeStatistics {

	public enum EventType { 
		STEAL_REQUEST,
		STEAL_REPLY_EMPTY,
		STEAL_REPLY_FULL,
		STEAL_REPLY_DONE,
		START_COMPUTE,
		JOB_RESULT
	}
	
	public class Event { 
		
		public final EventType type;	
		public final long time;
		public final long ID;
		public final Object data;
		
		public Event(EventType type, long ID, long time, Object data) { 
			this.type = type;
			this.ID = ID;
			this.time = time;
			this.data = data;
		}
		
		public String toString() { 
			return time + ":" + type + " " + ID;
		}		
	}
	
	public final String cluster;
	public final String node; 
	
	private long earliestJobStartTime = Long.MAX_VALUE;
	private long latestJobEndTime = Long.MIN_VALUE;
	
	private long earliestStartTime = Long.MAX_VALUE;
	private long lastestEndTime = Long.MIN_VALUE;
	
	private int jobs = 0;
	
	public LinkedList<Event> events = new LinkedList<Event>(); 
	
	public NodeStatistics(String cluster, String node) {
		this.cluster = cluster;		
		this.node = node;
	}
	
	public void addEvent(EventType type, long ID, long time, Object data) { 
		events.addLast(new Event(type, ID, time, data));
		
		switch (type) { 
		case STEAL_REQUEST:
		case STEAL_REPLY_EMPTY:	
		case START_COMPUTE:
		case STEAL_REPLY_DONE:	
			earliestStartTime = Math.min(earliestStartTime, time);
			lastestEndTime = Math.max(lastestEndTime, time);
			break;
		case STEAL_REPLY_FULL:
			earliestStartTime = Math.min(earliestStartTime, time);
			earliestJobStartTime = Math.min(earliestJobStartTime, time);
			lastestEndTime = Math.max(lastestEndTime, time);
			break;
		case JOB_RESULT:
			latestJobEndTime = Math.max(latestJobEndTime, time);
			lastestEndTime = Math.max(lastestEndTime, time);
			jobs++;
			break;
		}
	}
	
	public String toString() { 
		return cluster + " " + node + " " + events;  
	}

	public long getLatestJobEndTime() {
		return latestJobEndTime;
	}

	public long getEarliestJobStartTime() {
		return earliestJobStartTime;
	}

	public long getLatestEndTime() {
		return lastestEndTime;
	}

	public long getEarliestStartTime() {
		return earliestStartTime;
	}

	public int getJobs() {
		return jobs;
	}
}
