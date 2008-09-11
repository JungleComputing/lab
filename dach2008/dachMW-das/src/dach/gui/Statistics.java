package dach.gui;

import java.util.LinkedList;

public class Statistics {

	public enum EventType { 
		STEAL_REQUEST,
		STEAL_REPLY_EMPTY,
		STEAL_REPLY_FULL,
		START_COMPUTE,
		JOB_RESULT
	}
	
	public class Event { 
		
		public final EventType type;	
		public final long time;
		
		public Event(EventType type, long time) { 
			this.type = type;
			this.time = time;			
		}
		
		public String toString() { 
			return time + ":" + type;
		}		
	}
	
	public final String cluster;
	public final String node; 
	
	public LinkedList<Event> events = new LinkedList<Event>(); 
	
	public Statistics(String cluster, String node) {
		this.cluster = cluster;		
		this.node = node;
	}
	
	public void addEvent(EventType type, long time) { 
		events.addLast(new Event(type, time));
	}
	
	public String toString() { 
		return cluster + " " + node + " " + events;  
	}
}
