package dach.gui;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class ShowDataTransfer {

	final long start;
	final long end;
	final LinkedList<ClusterStatistics> stats;

	final long [] data;
	final JobInfo [] jobs;
	
	private ShowDataTransfer(long start, long end,
			LinkedList<ClusterStatistics> stats, JobInfo [] jobs) { 
		this.start = start;
		this.end = end;
		this.stats = stats;
		this.jobs = jobs;
		
		data = new long[(int)(end-start) + 1000];
	} 
	
	private JobInfo getJob(int id) { 
	
		JobInfo tmp = jobs[id];
		
		if (tmp.number != id) { 
			System.out.println("EEP could not find job! " + tmp.number + " " + id);
			return null;
		}

		return tmp;
	}
	
	private void addTransfer(long startTime, int transferTime, long size) { 
	
		int index = (int) (startTime / 1000);
		int left = 1000 - (int) (startTime % 1000);
		
		if (left <= transferTime) {
			data[index] += size;
		} else if (transferTime <= 1000) {
			data[index] = (long)((left / 1000.0) * size); 
			data[index+1] = size - data[index];
		} else {			
			long tmp = transferTime;
			double part = left; 
			
			while (tmp > 0) { 
				double mult = ((double) part) / transferTime;   				
				data[index++] = (long) (mult * size);	
				tmp -= part;
				
				if (tmp > 1000) { 
					part = 1000;
				} else { 
					part = tmp;
				}
			}
		}
	}
	
	private void handleEvent(NodeStatistics.Event e) { 
	
		long [] data = (long[]) e.data;
		
		int transferTime1 = (int) Math.round(data[2]);
		int transferTime2 = (int) Math.round(data[3]);		
		int computeTime = (int) Math.round(data[4]);
		
		long start = (e.time * 1000) - computeTime; 
		
		JobInfo job = getJob((int)e.ID);
		
		if (job == null) { 
			return;
		}
		
		addTransfer(start, transferTime1, job.size1);
		addTransfer(start + transferTime1, transferTime2, job.size1);
	}

	private void handleNode(NodeStatistics n) {
	
		for (NodeStatistics.Event e : n.events) { 			
			if (e.type == NodeStatistics.EventType.JOB_RESULT) { 
				handleEvent(e);
			}
		}		
	}
	

	private void handleNodes(ClusterStatistics c) { 
	
		for (NodeStatistics n : c.nodes) { 
			handleNode(n);
		}
	}
	
	private void start() {
		
		for (ClusterStatistics c : stats) { 
			handleNodes(c);
		}
		
		for (int i=0;i<data.length;i++) { 
			System.out.println(i + " " + (data[i] * 8) / (1000.0*1000.0));
		}
		
	}
	
	public static void main(String [] args) { 
		
		if (args.length < 5) { 
			System.err.println("Usage: OutputParser.main time dir prefix postfix cluster+");
			System.exit(1);
		}
		
		OutputParser p = new OutputParser(args[0], false);
		
		long startTime = p.getTime(args[0]); 
		long endTime = p.getTime(args[1]); 
		
		File dir = new File(args[2]);
		String prefix = args[3];
		String postfix = args[4];
		
		File jobs = new File(args[5]);
		
		String [] clusters = new String[args.length-6];
		System.arraycopy(args, 6, clusters, 0, args.length-6);				
		
		try {
			LinkedList<ClusterStatistics> s = p.parseAll(dir, startTime, prefix, clusters, postfix);

			System.out.println("Got " + s.size() + " clusters");
		
			long nodeEndTime = 0;
			long jobEndTime = 0;
			
			for (ClusterStatistics c : s) { 
				System.out.println("  " + c.name + " " + c.nodes.size() + " nodes " + c.getJobs() + " jobs");
			
				jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
				nodeEndTime = Math.max(nodeEndTime, c.getLatestEndTime());
			}
		
			System.out.println("total time = " + nodeEndTime);
			System.out.println("total job time = " + jobEndTime);
			
			System.out.println("total work time = " + p.totalWorkTime);
			System.out.println("total compute time = " + p.totalComputeTime);
			System.out.println("total transfer time = " + (p.totalWorkTime - p.totalComputeTime));
		
			JobInfo [] jobInfo = JobInfo.parseJobInfo(jobs);
			
			new ShowDataTransfer(startTime, endTime, s, jobInfo).start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
