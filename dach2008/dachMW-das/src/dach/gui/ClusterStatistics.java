package dach.gui;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class ClusterStatistics {

	public final String name;
	public final LinkedList<NodeStatistics> nodes = new LinkedList<NodeStatistics>();

	private long earliestJobStartTime = Long.MAX_VALUE;
	private long lastestJobEndTime = Long.MIN_VALUE;
	
	private long earliestStartTime = Long.MAX_VALUE;
	private long lastestEndTime = Long.MIN_VALUE;
	
	private int jobs = 0;
	
	private class SortEndTime implements Comparator<NodeStatistics> {

		public int compare(NodeStatistics n1, NodeStatistics n2) {
			return (int) (n1.getLatestJobEndTime() - n2.getLatestJobEndTime());
		} 
		
	}
	
	public ClusterStatistics(final String name) {
		this.name = name;
	}

	public void addNode(NodeStatistics n) { 
		nodes.addLast(n);
		
		lastestJobEndTime = Math.max(lastestJobEndTime, n.getLatestJobEndTime());
		earliestJobStartTime = Math.min(earliestJobStartTime, n.getEarliestJobStartTime());

		lastestEndTime = Math.max(lastestEndTime, n.getLatestEndTime());
		earliestStartTime = Math.min(earliestStartTime, n.getEarliestStartTime());
		
		jobs += n.getJobs();
	}
	
	public void sort(Comparator<NodeStatistics> comparator) { 
		Collections.sort(nodes, comparator);
	}
	
	public void sort() { 
		sort(new SortEndTime());
	}
	
	public long getLatestJobEndTime() {
		return lastestJobEndTime;
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
	
	public int getNodes() { 
		return nodes.size();
	}
}
